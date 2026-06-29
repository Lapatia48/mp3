package s6.mp3.api.track;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import s6.mp3.api.common.NotFoundException;
import s6.mp3.api.track.dto.TrackDtos.TrackUpdateRequest;
import s6.mp3.api.track.dto.TrackDtos.UploadMetadata;

import java.util.List;

@Service
@Profile("api")
public class TrackService {

    private static final Logger log = LoggerFactory.getLogger(TrackService.class);

    private final TrackRepository repository;
    private final StorageService storage;

    public TrackService(TrackRepository repository, StorageService storage) {
        this.repository = repository;
        this.storage = storage;
    }

    /**
     * Import depuis le Programme 3 : stocke le fichier (si nouveau) et enregistre
     * les metadonnees. Idempotent : un meme contenu (meme empreinte) n'est pas duplique.
     */
    @Transactional
    public Track upload(MultipartFile file, UploadMetadata meta) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Fichier MP3 manquant ou vide");
        }
        byte[] content;
        try {
            content = file.getBytes();
        } catch (Exception e) {
            throw new IllegalStateException("Lecture du fichier impossible", e);
        }
        String hash = storage.sha256(content);

        return repository.findByContentHash(hash)
                .map(existing -> {
                    log.info("Doublon ignore (deja en base) : {}", existing.getFileName());
                    return existing;
                })
                .orElseGet(() -> {
                    String originalName = meta != null && meta.fileName() != null
                            ? meta.fileName() : file.getOriginalFilename();
                    StorageService.Stored stored = storage.store(content, originalName);

                    Track t = new Track();
                    t.setFileName(originalName);
                    t.setStoragePath(stored.storagePath());
                    t.setContentHash(hash);
                    if (meta != null) {
                        t.setTitle(meta.title());
                        t.setArtist(meta.artist());
                        t.setAlbum(meta.album());
                        t.setGenre(meta.genre());
                        t.setDuration(meta.duration());
                        t.setYear(meta.year());
                        t.setDate(meta.date());
                    }
                    if (t.getTitle() == null || t.getTitle().isBlank()) {
                        t.setTitle(originalName);
                    }
                    Track saved = repository.save(t);
                    log.info("Morceau enregistre : id={} titre={}", saved.getId(), saved.getTitle());
                    return saved;
                });
    }

    @Transactional(readOnly = true)
    public List<Track> search(String genre, String artist, String album, String q) {
        return repository.search(lower(genre), lower(artist), lower(album), likePattern(q));
    }

    @Transactional(readOnly = true)
    public Track get(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Morceau introuvable : " + id));
    }

    public org.springframework.core.io.Resource loadResource(Track track) {
        return storage.loadAsResource(track.getStoragePath());
    }

    @Transactional
    public Track update(Long id, TrackUpdateRequest req) {
        Track t = get(id);
        if (req.title() != null) t.setTitle(req.title());
        if (req.artist() != null) t.setArtist(req.artist());
        if (req.album() != null) t.setAlbum(req.album());
        if (req.genre() != null) t.setGenre(req.genre());
        if (req.duration() != null) t.setDuration(req.duration());
        if (req.year() != null) t.setYear(req.year());
        if (req.date() != null) t.setDate(req.date());
        return repository.save(t);
    }

    @Transactional
    public void delete(Long id) {
        Track t = get(id);
        storage.delete(t.getStoragePath());
        repository.delete(t);
        log.info("Morceau supprime : id={}", id);
    }

    private String lower(String s) {
        return (s == null || s.isBlank()) ? null : s.trim().toLowerCase();
    }

    private String likePattern(String s) {
        return (s == null || s.isBlank()) ? null : "%" + s.trim().toLowerCase() + "%";
    }
}
