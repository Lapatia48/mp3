package s6.mp3.api.track.dto;

import s6.mp3.api.track.Track;

/**
 * DTOs lies aux morceaux (Track).
 */
public final class TrackDtos {

    private TrackDtos() {
    }

    /** Reponse exposee a l'application web. */
    public record TrackDto(
            Long id,
            String title,
            String artist,
            String album,
            String genre,
            Integer duration,
            String year,
            String date,
            String fileName) {

        public static TrackDto from(Track t) {
            return new TrackDto(t.getId(), t.getTitle(), t.getArtist(), t.getAlbum(),
                    t.getGenre(), t.getDuration(), t.getYear(), t.getDate(), t.getFileName());
        }
    }

    /** Metadonnees recues du Programme 3 (part JSON "metadata" de l'upload). */
    public record UploadMetadata(
            String path,
            String fileName,
            String title,
            String artist,
            String album,
            String genre,
            Integer duration,
            String year,
            String date) {
    }

    /** Modification manuelle des informations d'un morceau. */
    public record TrackUpdateRequest(
            String title,
            String artist,
            String album,
            String genre,
            Integer duration,
            String year,
            String date) {
    }
}
