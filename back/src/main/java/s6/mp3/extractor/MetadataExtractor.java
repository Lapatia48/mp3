package s6.mp3.extractor;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.AudioHeader;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import s6.mp3.common.MetadataMessage;
import s6.mp3.common.RabbitConfig;
import s6.mp3.common.ScanMessage;

import java.io.File;

/**
 * Programme 2 : Extracteur de metadonnees.
 *
 * <p>Consomme {@code queue.scan}, extrait les metadonnees du MP3 via
 * jaudiotagger, puis publie le resultat dans {@code queue.metadata}.
 *
 * <p>Activation : profil Spring {@code extractor}.
 */
@Component
@Profile("extractor")
public class MetadataExtractor {

    private static final Logger log = LoggerFactory.getLogger(MetadataExtractor.class);

    private final RabbitTemplate rabbitTemplate;

    public MetadataExtractor(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @RabbitListener(queues = RabbitConfig.QUEUE_SCAN)
    public void onScanMessage(ScanMessage msg) {
        log.info("Debut extraction : {}", msg.getFileName());

        File file = new File(msg.getPath());
        if (!file.exists()) {
            log.warn("Fichier introuvable, extraction ignoree : {}", msg.getPath());
            return;
        }

        try {
            AudioFile audioFile = AudioFileIO.read(file);
            AudioHeader header = audioFile.getAudioHeader();
            Tag tag = audioFile.getTag();

            MetadataMessage out = new MetadataMessage();
            out.setPath(msg.getPath());
            out.setFileName(msg.getFileName());
            out.setDuration(header != null ? header.getTrackLength() : null);

            if (tag != null) {
                out.setTitle(read(tag, FieldKey.TITLE));
                out.setArtist(read(tag, FieldKey.ARTIST));
                out.setAlbum(read(tag, FieldKey.ALBUM));
                out.setGenre(read(tag, FieldKey.GENRE));
                out.setYear(read(tag, FieldKey.YEAR));
                out.setDate(read(tag, FieldKey.YEAR));
            }

            // A defaut de titre, utiliser le nom du fichier (sans extension).
            if (out.getTitle() == null) {
                out.setTitle(stripExtension(msg.getFileName()));
            }

            rabbitTemplate.convertAndSend(RabbitConfig.QUEUE_METADATA, out);
            log.info("Metadonnees extraites avec succes : {}", out);
        } catch (Exception e) {
            log.error("Erreur lors de l'extraction de {} : {}", msg.getFileName(), e.getMessage(), e);
        }
    }

    /** Lit un champ de tag en gerant l'absence et les exceptions. */
    private String read(Tag tag, FieldKey key) {
        try {
            String value = tag.getFirst(key);
            return (value == null || value.isBlank()) ? null : value.trim();
        } catch (Exception e) {
            return null;
        }
    }

    private String stripExtension(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot > 0 ? fileName.substring(0, dot) : fileName;
    }
}
