package s6.mp3.api.common;

/** Levee quand une ressource demandee n'existe pas (-> HTTP 404). */
public class NotFoundException extends RuntimeException {
    public NotFoundException(String message) {
        super(message);
    }
}
