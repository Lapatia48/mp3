package s6.mp3.extractor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * Liste noire d'artistes.
 *
 * <p>Charge le fichier {@code blacklist/blacklist.txt} (un artiste par ligne).
 * La comparaison est <b>insensible a la casse</b> et porte sur une
 * <b>sous-chaine</b> : la ligne {@code skaiz} bloque l'artiste
 * {@code "Skaiz Official"}, la ligne {@code mahaleo} bloque
 * {@code "Mahaleo Officiel"}, etc. Les lignes vides et celles commencant par
 * {@code #} (commentaires) sont ignorees.
 *
 * <p>Le fichier est relu a chaque appel s'il a ete modifie, de sorte qu'on peut
 * ajouter un artiste sans redemarrer le programme.
 *
 * <p>Activation : profil Spring {@code extractor}.
 */
@Component
@Profile("extractor")
public class BlacklistFilter {

    private static final Logger log = LoggerFactory.getLogger(BlacklistFilter.class);

    @Value("${app.blacklist-file:blacklist/blacklist.txt}")
    private String blacklistFile;

    /** Motifs (deja en minuscules) charges depuis le fichier. */
    private volatile List<String> patterns = List.of();
    /** Date de derniere modification prise en compte, pour ne relire qu'au besoin. */
    private volatile long lastModified = -1L;

    /**
     * @return {@code true} si l'artiste correspond a une entree de la liste noire.
     */
    public boolean isBlacklisted(String artist) {
        if (artist == null || artist.isBlank()) {
            return false;
        }
        reloadIfNeeded();
        String needle = artist.toLowerCase();
        for (String pattern : patterns) {
            if (needle.contains(pattern)) {
                return true;
            }
        }
        return false;
    }

    /** (Re)charge le fichier si sa date de modification a change. */
    private synchronized void reloadIfNeeded() {
        File file = new File(blacklistFile).getAbsoluteFile();
        if (!file.exists()) {
            if (lastModified != 0L) {
                log.warn("Liste noire introuvable : {} (aucun artiste bloque)", file.getAbsolutePath());
                patterns = List.of();
                lastModified = 0L;
            }
            return;
        }
        long modified = file.lastModified();
        if (modified == lastModified) {
            return;
        }
        try {
            List<String> loaded = new ArrayList<>();
            for (String line : Files.readAllLines(file.toPath(), StandardCharsets.UTF_8)) {
                String entry = line.trim();
                if (entry.isEmpty() || entry.startsWith("#")) {
                    continue;
                }
                loaded.add(entry.toLowerCase());
            }
            patterns = List.copyOf(loaded);
            lastModified = modified;
            log.info("Liste noire chargee : {} artiste(s) bloque(s) depuis {}", loaded.size(), file.getAbsolutePath());
        } catch (Exception e) {
            log.error("Impossible de lire la liste noire {} : {}", file.getAbsolutePath(), e.getMessage());
        }
    }
}
