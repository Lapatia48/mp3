package s6.mp3.scanner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import s6.mp3.common.AuditPublisher;
import s6.mp3.common.RabbitConfig;
import s6.mp3.common.ScanMessage;

import java.io.File;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Programme 1 : Scanner de repertoire.
 *
 * <p>Lit periodiquement le repertoire d'import, ne retient que les fichiers
 * {@code .mp3} jamais vus, et publie chacun dans la file {@code queue.scan}.
 *
 * <p>Activation : profil Spring {@code scanner}.
 */
@Component
@Profile("scanner")
public class DirectoryScanner {

    private static final Logger log = LoggerFactory.getLogger(DirectoryScanner.class);

    private final RabbitTemplate rabbitTemplate;
    private final AuditPublisher audit;

    /** Chemins absolus deja publies (evite de renvoyer le meme fichier a chaque scan). */
    private final Set<String> alreadySeen = ConcurrentHashMap.newKeySet();

    @Value("${app.incoming-dir}")
    private String incomingDir;

    public DirectoryScanner(RabbitTemplate rabbitTemplate, AuditPublisher audit) {
        this.rabbitTemplate = rabbitTemplate;
        this.audit = audit;
    }

    @Scheduled(
            fixedDelayString = "${app.scan.interval-ms:300000}",
            initialDelayString = "${app.scan.initial-delay-ms:3000}")
    public void scan() {
        log.info("Debut du scan");

        File dir = new File(incomingDir).getAbsoluteFile();
        if (!dir.exists() || !dir.isDirectory()) {
            log.warn("Repertoire d'import introuvable : {}", dir.getAbsolutePath());
            return;
        }

        pruneMissingFiles();

        File[] files = dir.listFiles();
        if (files == null) {
            log.warn("Impossible de lire le contenu de : {}", dir.getAbsolutePath());
            return;
        }

        int detected = 0;
        for (File f : files) {
            if (!f.isFile()) {
                continue;
            }
            if (!f.getName().toLowerCase().endsWith(".mp3")) {
                continue; // ignorer tout ce qui n'est pas un .mp3
            }
            String abs = f.getAbsolutePath();
            if (!alreadySeen.add(abs)) {
                continue; // deja detecte lors d'un scan precedent
            }
            log.info("Nouveau fichier detecte : {}", f.getName());
            rabbitTemplate.convertAndSend(RabbitConfig.QUEUE_SCAN, new ScanMessage(f.getName(), abs));
            audit.scanned(f.getName(), abs);
            detected++;
        }

        log.info("Fin du scan ({} nouveau(x) fichier(s))", detected);
    }

    /** Oublie les fichiers disparus pour pouvoir les redetecter s'ils reviennent. */
    private void pruneMissingFiles() {
        Iterator<String> it = alreadySeen.iterator();
        while (it.hasNext()) {
            if (!new File(it.next()).exists()) {
                it.remove();
            }
        }
    }
}
