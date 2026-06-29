package s6.mp3.scanner;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Active la planification ({@code @Scheduled}) uniquement pour le Programme 1
 * (profil {@code scanner}).
 */
@Configuration
@Profile("scanner")
@EnableScheduling
public class SchedulingConfig {
}
