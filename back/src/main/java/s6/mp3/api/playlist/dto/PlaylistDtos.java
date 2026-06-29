package s6.mp3.api.playlist.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import s6.mp3.api.playlist.Playlist;
import s6.mp3.api.track.dto.TrackDtos.TrackDto;

import java.util.List;

/**
 * DTOs lies aux playlists.
 */
public final class PlaylistDtos {

    private PlaylistDtos() {
    }

    /**
     * Criteres de generation automatique. Tous facultatifs.
     * <ul>
     *   <li>{@code minMinutes}/{@code maxMinutes} : fourchette de duree visee.
     *       {@code durationMinutes} reste accepte (compat) comme cible unique.</li>
     *   <li>{@code includeArtists}/{@code includeAlbums} : graines (priorite). La
     *       playlist commence par ces artistes/albums puis se complete avec le
     *       reste de la bibliotheque pour atteindre la duree visee.</li>
     *   <li>{@code onlyArtists}/{@code onlyAlbums} : sous-ensemble marque
     *       « uniquement ». Un artiste « uniquement » restreint toute la playlist
     *       a cet artiste ; un album « uniquement » impose l'album entier (et,
     *       seul, arrete la la generation).</li>
     *   <li>{@code excludeArtists}/{@code excludeAlbums}/{@code excludeGenres} :
     *       exclusions absolues — ces morceaux n'apparaissent jamais, meme si la
     *       playlist en devient plus courte.</li>
     *   <li>{@code includeGenres} : si non vide, seuls ces genres sont retenus.</li>
     * </ul>
     * Le champ {@code genre} est conserve pour compatibilite (ancien client).
     */
    public record GenerateRequest(
            Integer durationMinutes,
            Integer minMinutes,
            Integer maxMinutes,
            String genre,
            List<String> includeGenres,
            List<String> excludeGenres,
            List<String> includeArtists,
            List<String> onlyArtists,
            List<String> excludeArtists,
            List<String> includeAlbums,
            List<String> onlyAlbums,
            List<String> excludeAlbums) {
    }

    /** Sauvegarde / mise a jour d'une playlist (liste ordonnee d'ids de morceaux). */
    public record SavePlaylistRequest(
            @NotBlank String name,
            @NotNull List<Long> trackIds) {
    }

    /** Reponse : playlist generee ou enregistree. */
    public record PlaylistDto(
            Long id,
            String name,
            int totalDuration,
            List<TrackDto> tracks) {

        public static PlaylistDto of(Long id, String name, List<TrackDto> tracks) {
            int total = tracks.stream().mapToInt(t -> t.duration() == null ? 0 : t.duration()).sum();
            return new PlaylistDto(id, name, total, tracks);
        }

        public static PlaylistDto from(Playlist p) {
            List<TrackDto> tracks = p.getItems().stream()
                    .map(item -> TrackDto.from(item.getTrack()))
                    .toList();
            return of(p.getId(), p.getName(), tracks);
        }
    }
}
