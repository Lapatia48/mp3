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

import org.springframework.beans.factory.annotation.Value;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Programme 2 : Extracteur de metadonnees.
 *
 * <p>Consomme {@code queue.scan}, extrait les metadonnees du MP3 via
 * jaudiotagger, puis publie le resultat dans {@code queue.metadata}.
 *
 * <p>Avant de publier, l'artiste est confronte a la <b>liste noire</b>
 * ({@link BlacklistFilter}) : si l'artiste est bloque, le morceau n'est pas
 * importe — son fichier est deplace dans le dossier {@code blacklisted/} et
 * aucun message n'est publie vers l'Uploader.
 *
 * <p>Activation : profil Spring {@code extractor}.
 */
@Component
@Profile("extractor")
public class MetadataExtractor {

    private static final Logger log = LoggerFactory.getLogger(MetadataExtractor.class);

    private final RabbitTemplate rabbitTemplate;
    private final BlacklistFilter blacklist;

    @Value("${app.blacklisted-dir:blacklisted}")
    private String blacklistedDir;

    public MetadataExtractor(RabbitTemplate rabbitTemplate, BlacklistFilter blacklist) {
        this.rabbitTemplate = rabbitTemplate;
        this.blacklist = blacklist;
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

            // Liste noire (artiste ou genre) : si bloque, on n'importe pas et on
            // met le fichier de cote dans le sous-dossier correspondant.
            BlacklistFilter.Reason reason = blacklist.check(out.getArtist(), out.getGenre());
            if (reason != null) {
                String cause = reason == BlacklistFilter.Reason.ARTIST ? out.getArtist() : out.getGenre();
                log.info("Sur liste noire ({}) : {} ({}) -> non importe",
                        reason.subdir(), cause, msg.getFileName());
                moveToBlacklisted(file, reason.subdir());
                return;
            }

            rabbitTemplate.convertAndSend(RabbitConfig.QUEUE_METADATA, out);
            log.info("Metadonnees extraites avec succes : {}", out);
        } catch (Exception e) {
            log.error("Erreur lors de l'extraction de {} : {}", msg.getFileName(), e.getMessage(), e);
        }
    }

    /**
     * Deplace un fichier blackliste vers {@code blacklisted/<subdir>/}
     * ({@code subdir} = {@code artistes} ou {@code genres}).
     */
    private void moveToBlacklisted(File file, String subdir) {
        try {
            File dir = new File(blacklistedDir, subdir).getAbsoluteFile();
            if (!dir.exists() && !dir.mkdirs()) {
                log.error("Impossible de creer le dossier blacklisted : {}", dir.getAbsolutePath());
                return;
            }
            Path target = dir.toPath().resolve(file.getName());
            Files.move(file.toPath(), target, StandardCopyOption.REPLACE_EXISTING);
            log.info("Fichier blackliste deplace vers : {}", target.toAbsolutePath());
        } catch (Exception e) {
            log.error("Impossible de deplacer le fichier blackliste {} : {}",
                    file.getName(), e.getMessage(), e);
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
