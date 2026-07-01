package s6.mp3.common;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Configuration commune RabbitMQ partagee par les 3 programmes standalone.
 *
 * <p><b>Files du pipeline</b> (consommees : un programme lit, traite, puis
 * supprime le message) :
 * <ul>
 *   <li>{@code queue.scan}     : Programme 1 (Scanner)    -> Programme 2 (Extracteur)</li>
 *   <li>{@code queue.metadata} : Programme 2 (Extracteur) -> Programme 3 (Uploader)</li>
 * </ul>
 *
 * <p><b>Files de journalisation</b> (aucun consommateur : les messages restent
 * en file et servent de <i>journal</i> consultable via <i>Get messages</i> dans
 * la console RabbitMQ). Chaque etape du pipeline y depose un evenement reel :
 * <ul>
 *   <li>{@code queue.scanned}   : chaque fichier .mp3 detecte par le Scanner ;</li>
 *   <li>{@code queue.extracted} : chaque morceau dont les metadonnees ont ete extraites ;</li>
 *   <li>{@code queue.deleted}   : chaque fichier ecarte par la liste noire de l'Extracteur ;</li>
 *   <li>{@code queue.send}      : chaque fichier envoye avec succes a l'API par l'Uploader.</li>
 * </ul>
 * Ces files sont bornees ({@code x-max-length}) : elles se comportent comme un
 * journal glissant (les plus anciens messages sont ecartes au-dela de la limite).
 *
 * <p>Le bean {@link MessageConverter} (JSON) est automatiquement utilise par
 * Spring Boot pour le {@code RabbitTemplate} (envoi) et pour les
 * {@code @RabbitListener} (reception).
 */
@Configuration
@Profile({"scanner", "extractor", "uploader"})
public class RabbitConfig {

    /** Files du pipeline (consommees). */
    public static final String QUEUE_SCAN = "queue.scan";
    public static final String QUEUE_METADATA = "queue.metadata";

    /** Files de journalisation (non consommees, alimentees par les evenements reels). */
    public static final String QUEUE_SCANNED = "queue.scanned";
    public static final String QUEUE_EXTRACTED = "queue.extracted";
    public static final String QUEUE_DELETED = "queue.deleted";
    public static final String QUEUE_SEND = "queue.send";

    /** Taille max d'une file de journal (journal glissant). */
    private static final int AUDIT_MAX_LENGTH = 500;

    @Bean
    public Queue scanQueue() {
        return new Queue(QUEUE_SCAN, true);
    }

    @Bean
    public Queue metadataQueue() {
        return new Queue(QUEUE_METADATA, true);
    }

    @Bean
    public Queue scannedQueue() {
        return auditQueue(QUEUE_SCANNED);
    }

    @Bean
    public Queue extractedQueue() {
        return auditQueue(QUEUE_EXTRACTED);
    }

    @Bean
    public Queue deletedQueue() {
        return auditQueue(QUEUE_DELETED);
    }

    @Bean
    public Queue sendQueue() {
        return auditQueue(QUEUE_SEND);
    }

    /** File de journal durable et bornee (journal glissant). */
    private Queue auditQueue(String name) {
        return QueueBuilder.durable(name).maxLength(AUDIT_MAX_LENGTH).build();
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }
}
