package s6.mp3.api.playlist;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import s6.mp3.api.common.NotFoundException;
import s6.mp3.api.playlist.dto.PlaylistDtos.GenerateRequest;
import s6.mp3.api.playlist.dto.PlaylistDtos.PlaylistDto;
import s6.mp3.api.playlist.dto.PlaylistDtos.SavePlaylistRequest;
import s6.mp3.api.track.StorageService;
import s6.mp3.api.track.Track;
import s6.mp3.api.track.TrackRepository;
import s6.mp3.api.track.dto.TrackDtos.TrackDto;
import s6.mp3.api.user.User;
import s6.mp3.api.user.UserRepository;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
@Profile("api")
public class PlaylistService {

    private final PlaylistRepository playlistRepository;
    private final TrackRepository trackRepository;
    private final UserRepository userRepository;
    private final StorageService storage;
    private final PlaylistGenerator generator;

    public PlaylistService(PlaylistRepository playlistRepository,
                           TrackRepository trackRepository,
                           UserRepository userRepository,
                           StorageService storage,
                           PlaylistGenerator generator) {
        this.playlistRepository = playlistRepository;
        this.trackRepository = trackRepository;
        this.userRepository = userRepository;
        this.storage = storage;
        this.generator = generator;
    }

    /** Generation automatique (non sauvegardee). */
    @Transactional(readOnly = true)
    public PlaylistDto generate(GenerateRequest req) {
        int[] range = durationRange(req);
        // Le genre (egalite simple) reste filtre par la requete ; les listes
        // inclure/exclure des artistes et albums sont appliquees ensuite.
        List<Track> candidates = trackRepository.search(blankToNull(req.genre()), null, null, null)
                .stream()
                .filter(t -> passesFilters(t, req))
                .toList();
        List<Track> selected = generator.generate(candidates, range[0], range[1]);
        List<TrackDto> tracks = selected.stream().map(TrackDto::from).toList();
        return PlaylistDto.of(null, "Playlist generee", tracks);
    }

    @Transactional
    public PlaylistDto save(String username, SavePlaylistRequest req) {
        User owner = requireUser(username);
        Playlist playlist = new Playlist();
        playlist.setName(req.name());
        playlist.setOwner(owner);
        applyTracks(playlist, req.trackIds());
        return PlaylistDto.from(playlistRepository.save(playlist));
    }

    @Transactional(readOnly = true)
    public List<PlaylistDto> list(String username) {
        User owner = requireUser(username);
        return playlistRepository.findByOwnerIdOrderByCreatedAtDesc(owner.getId()).stream()
                .map(PlaylistDto::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public PlaylistDto get(String username, Long id) {
        return PlaylistDto.from(requireOwned(username, id));
    }

    @Transactional
    public PlaylistDto update(String username, Long id, SavePlaylistRequest req) {
        Playlist playlist = requireOwned(username, id);
        playlist.setName(req.name());
        playlist.getItems().clear();
        applyTracks(playlist, req.trackIds());
        return PlaylistDto.from(playlistRepository.save(playlist));
    }

    @Transactional
    public void delete(String username, Long id) {
        playlistRepository.delete(requireOwned(username, id));
    }

    /** Construit une archive ZIP contenant tous les MP3 de la playlist. */
    @Transactional(readOnly = true)
    public byte[] zip(String username, Long id) {
        Playlist playlist = requireOwned(username, id);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Set<String> usedNames = new HashSet<>();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            for (PlaylistTrack item : playlist.getItems()) {
                Track track = item.getTrack();
                String name = uniqueName(usedNames, track.getFileName());
                zos.putNextEntry(new ZipEntry(name));
                zos.write(storage.readBytes(track.getStoragePath()));
                zos.closeEntry();
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Echec de creation de l'archive ZIP", e);
        }
        return baos.toByteArray();
    }

    // --- helpers ---

    private void applyTracks(Playlist playlist, List<Long> trackIds) {
        List<PlaylistTrack> items = new ArrayList<>();
        int position = 0;
        for (Long trackId : trackIds) {
            Track track = trackRepository.findById(trackId)
                    .orElseThrow(() -> new NotFoundException("Morceau introuvable : " + trackId));
            items.add(new PlaylistTrack(playlist, track, position++));
        }
        playlist.getItems().addAll(items);
    }

    private User requireUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new NotFoundException("Utilisateur introuvable : " + username));
    }

    private Playlist requireOwned(String username, Long id) {
        User owner = requireUser(username);
        return playlistRepository.findByIdAndOwnerId(id, owner.getId())
                .orElseThrow(() -> new NotFoundException("Playlist introuvable : " + id));
    }

    private String uniqueName(Set<String> used, String fileName) {
        String base = (fileName == null || fileName.isBlank()) ? "track.mp3" : fileName;
        String name = base;
        int i = 1;
        while (!used.add(name)) {
            int dot = base.lastIndexOf('.');
            name = (dot > 0 ? base.substring(0, dot) : base) + "(" + i + ")"
                    + (dot > 0 ? base.substring(dot) : ".mp3");
            i++;
        }
        return name;
    }

    private String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }

    /** Convertit les criteres en fourchette {@code [minSeconds, maxSeconds]}. */
    private int[] durationRange(GenerateRequest req) {
        Integer min = req.minMinutes();
        Integer max = req.maxMinutes();
        // Compat : ancien champ duree unique -> cible exacte.
        if (min == null && max == null && req.durationMinutes() != null) {
            min = req.durationMinutes();
            max = req.durationMinutes();
        }
        int minSec = (min != null ? min : 0) * 60;
        int maxSec = (max != null ? max : 60) * 60;
        if (maxSec < minSec) {
            int tmp = maxSec;
            maxSec = minSec;
            minSec = tmp;
        }
        return new int[]{minSec, maxSec};
    }

    /** Verifie qu'un morceau respecte les listes inclure/exclure (artiste, album). */
    private boolean passesFilters(Track t, GenerateRequest req) {
        return matchesInclude(t.getArtist(), req.includeArtists())
                && !matchesExclude(t.getArtist(), req.excludeArtists())
                && matchesInclude(t.getAlbum(), req.includeAlbums())
                && !matchesExclude(t.getAlbum(), req.excludeAlbums());
    }

    /** Inclusion : liste vide => tout accepte ; sinon la valeur doit y figurer. */
    private boolean matchesInclude(String value, List<String> include) {
        if (include == null || include.isEmpty()) {
            return true;
        }
        return value != null && include.stream().anyMatch(v -> v.equalsIgnoreCase(value));
    }

    /** Exclusion : vrai si la valeur figure dans la liste (donc a ecarter). */
    private boolean matchesExclude(String value, List<String> exclude) {
        if (exclude == null || exclude.isEmpty() || value == null) {
            return false;
        }
        return exclude.stream().anyMatch(v -> v.equalsIgnoreCase(value));
    }
}
