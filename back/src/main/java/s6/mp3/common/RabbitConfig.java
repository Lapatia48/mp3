package s6.mp3.common;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Configuration commune RabbitMQ partagee par les 3 programmes standalone.
 *
 * <p>Deux files durables :
 * <ul>
 *   <li>{@code queue.scan}     : Programme 1 -> Programme 2</li>
 *   <li>{@code queue.metadata} : Programme 2 -> Programme 3</li>
 * </ul>
 *
 * <p>Le bean {@link MessageConverter} (JSON) est automatiquement utilise par
 * Spring Boot pour le {@code RabbitTemplate} (envoi) et pour les
 * {@code @RabbitListener} (reception).
 */
@Configuration
@Profile({"scanner", "extractor", "uploader"})
public class RabbitConfig {

    public static final String QUEUE_SCAN = "queue.scan";
    public static final String QUEUE_METADATA = "queue.metadata";

    @Bean
    public Queue scanQueue() {
        return new Queue(QUEUE_SCAN, true);
    }

    @Bean
    public Queue metadataQueue() {
        return new Queue(QUEUE_METADATA, true);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }
}
