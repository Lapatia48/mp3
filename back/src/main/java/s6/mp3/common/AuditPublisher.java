package s6.mp3.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Journalise les evenements reels du pipeline dans des files RabbitMQ dediees
 * (non consommees). Chaque etape appelle la methode correspondante ; le message
 * reste dans la file et peut etre inspecte via <i>Get messages</i> dans la
 * console RabbitMQ (<a href="http://localhost:15672">http://localhost:15672</a>),
 * a la maniere d'un journal.
 *
 * <p>Partage par les 3 programmes standalone — chacun n'appelle que les methodes
 * de son etape (Scanner : {@link #scanned}, Extracteur : {@link #extracted} /
 * {@link #deleted}, Uploader : {@link #sent}).
 */
@Component
@Profile({"scanner", "extractor", "uploader"})
public class AuditPublisher {

    private static final Logger log = LoggerFactory.getLogger(AuditPublisher.class);

    private final RabbitTemplate rabbitTemplate;

    public AuditPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    /** Un fichier .mp3 vient d'etre detecte par le Scanner. */
    public void scanned(String fileName, String path) {
        publish(RabbitConfig.QUEUE_SCANNED, event("SCANNED",
                "fileName", fileName,
                "path", path));
    }

    /** Les metadonnees d'un morceau ont ete extraites avec succes. */
    public void extracted(MetadataMessage msg) {
        publish(RabbitConfig.QUEUE_EXTRACTED, event("EXTRACTED",
                "fileName", msg.getFileName(),
                "title", msg.getTitle(),
                "artist", msg.getArtist(),
                "genre", msg.getGenre(),
                "duration", msg.getDuration()));
    }

    /** Un fichier a ete ecarte par la liste noire (artiste, genre ou duree). */
    public void deleted(String fileName, String reason, String cause) {
        publish(RabbitConfig.QUEUE_DELETED, event("DELETED",
                "fileName", fileName,
                "reason", reason,
                "cause", cause));
    }

    /** Un fichier a ete envoye avec succes a l'API. */
    public void sent(MetadataMessage msg) {
        publish(RabbitConfig.QUEUE_SEND, event("SENT",
                "fileName", msg.getFileName(),
                "title", msg.getTitle(),
                "artist", msg.getArtist()));
    }

    private void publish(String queue, Map<String, Object> payload) {
        try {
            rabbitTemplate.convertAndSend(queue, payload);
        } catch (Exception e) {
            // La journalisation ne doit jamais faire echouer le traitement.
            log.warn("Impossible de journaliser vers {} : {}", queue, e.getMessage());
        }
    }

    /** Construit un evenement JSON horodate (type + paires cle/valeur). */
    private Map<String, Object> event(String type, Object... kv) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("type", type);
        map.put("timestamp", Instant.now().toString());
        for (int i = 0; i + 1 < kv.length; i += 2) {
            if (kv[i] != null) {
                map.put(String.valueOf(kv[i]), kv[i + 1]);
            }
        }
        return map;
    }
}
