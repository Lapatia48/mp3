package s6.mp3.uploader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import s6.mp3.common.MetadataMessage;
import s6.mp3.common.RabbitConfig;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Programme 3 : Uploader API.
 *
 * <p>Consomme {@code queue.metadata}, envoie le fichier MP3 + ses metadonnees
 * a l'API (multipart). En cas de succes le fichier source est supprime du
 * repertoire d'import ; en cas d'echec apres N tentatives, il est deplace
 * dans le dossier d'echec.
 *
 * <p>Activation : profil Spring {@code uploader}.
 */
@Component
@Profile("uploader")
public class ApiUploader {

    private static final Logger log = LoggerFactory.getLogger(ApiUploader.class);

    private final RestClient restClient = RestClient.create();

    @Value("${app.api.upload-url}")
    private String uploadUrl;

    @Value("${app.failed-dir}")
    private String failedDir;

    @Value("${app.upload.max-retries:3}")
    private int maxRetries;

    @Value("${app.upload.retry-delay-ms:2000}")
    private long retryDelayMs;

    @RabbitListener(queues = RabbitConfig.QUEUE_METADATA)
    public void onMetadata(MetadataMessage msg) {
        log.info("Debut envoi API : {}", msg.getFileName());

        File file = new File(msg.getPath());
        if (!file.exists()) {
            log.warn("Fichier introuvable, envoi abandonne : {}", msg.getPath());
            return;
        }

        boolean success = false;
        for (int attempt = 1; attempt <= maxRetries && !success; attempt++) {
            try {
                log.info("Envoi en cours (tentative {}/{})", attempt, maxRetries);
                success = upload(file, msg);
                if (!success) {
                    log.warn("Reponse non-2xx de l'API (tentative {}/{})", attempt, maxRetries);
                }
            } catch (Exception e) {
                log.error("Echec tentative {}/{} pour {} : {}",
                        attempt, maxRetries, msg.getFileName(), e.getMessage());
            }
            if (!success && attempt < maxRetries) {
                sleep(retryDelayMs);
            }
        }

        if (success) {
            log.info("Envoi termine avec succes");
            if (file.delete()) {
                log.info("Suppression du fichier source : {}", msg.getFileName());
            } else {
                log.warn("Impossible de supprimer le fichier source : {}", msg.getPath());
            }
        } else {
            log.error("Toutes les tentatives ont echoue pour {}. Deplacement vers le dossier d'echec.",
                    msg.getFileName());
            moveToFailed(file);
        }
    }

    private boolean upload(File file, MetadataMessage msg) {
        HttpHeaders jsonHeaders = new HttpHeaders();
        jsonHeaders.setContentType(MediaType.APPLICATION_JSON);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new FileSystemResource(file));
        body.add("metadata", new HttpEntity<>(msg, jsonHeaders));

        return restClient.post()
                .uri(uploadUrl)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(body)
                .retrieve()
                .toBodilessEntity()
                .getStatusCode()
                .is2xxSuccessful();
    }

    private void moveToFailed(File file) {
        try {
            File dir = new File(failedDir).getAbsoluteFile();
            if (!dir.exists() && !dir.mkdirs()) {
                log.error("Impossible de creer le dossier d'echec : {}", dir.getAbsolutePath());
                return;
            }
            Path target = dir.toPath().resolve(file.getName());
            Files.move(file.toPath(), target, StandardCopyOption.REPLACE_EXISTING);
            log.info("Fichier deplace vers : {}", target.toAbsolutePath());
        } catch (Exception e) {
            log.error("Impossible de deplacer le fichier en echec {} : {}",
                    file.getName(), e.getMessage(), e);
        }
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
