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
     *   <li>{@code includeArtists}/{@code includeAlbums} : si non vide, seuls ces
     *       artistes/albums sont retenus (sinon tous).</li>
     *   <li>{@code excludeArtists}/{@code excludeAlbums} : artistes/albums ecartes.</li>
     * </ul>
     */
    public record GenerateRequest(
            Integer durationMinutes,
            Integer minMinutes,
            Integer maxMinutes,
            String genre,
            List<String> includeArtists,
            List<String> excludeArtists,
            List<String> includeAlbums,
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
