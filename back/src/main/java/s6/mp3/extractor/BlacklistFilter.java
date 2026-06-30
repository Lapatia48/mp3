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
 * Liste noire d'import : par <b>artiste</b> et par <b>genre</b>.
 *
 * <p>Deux fichiers, meme logique :
 * <ul>
 *   <li>{@code blacklist/artiste.txt} — artistes a bloquer ;</li>
 *   <li>{@code blacklist/genre.txt} — genres a bloquer.</li>
 * </ul>
 * Un motif par ligne. La comparaison est <b>insensible a la casse</b> et porte
 * sur une <b>sous-chaine</b> : la ligne {@code skaiz} bloque l'artiste
 * {@code "Skaiz Official"}, la ligne {@code rock} bloque le genre {@code "Pop-Rock"}.
 * Les lignes vides et celles commencant par {@code #} (commentaires) sont ignorees.
 *
 * <p>Chaque fichier est relu a chaud s'il a ete modifie (on peut donc ajouter une
 * entree sans redemarrer le programme).
 *
 * <p>Activation : profil Spring {@code extractor}.
 */
@Component
@Profile("extractor")
public class BlacklistFilter {

    /** Categorie de blocage : sert aussi de nom de sous-dossier dans {@code blacklisted/}. */
    public enum Reason {
        ARTIST("artistes"),
        GENRE("genres");

        private final String subdir;

        Reason(String subdir) {
            this.subdir = subdir;
        }

        /** Sous-dossier de destination dans {@code blacklisted/}. */
        public String subdir() {
            return subdir;
        }
    }

    @Value("${app.blacklist-artist-file:blacklist/artiste.txt}")
    private String artistFile;

    @Value("${app.blacklist-genre-file:blacklist/genre.txt}")
    private String genreFile;

    private RuleSet artists;
    private RuleSet genres;

    /**
     * Determine si le morceau doit etre blackliste, et pour quelle raison.
     *
     * @return {@link Reason#ARTIST}, {@link Reason#GENRE}, ou {@code null} si le
     *         morceau est autorise. L'artiste est prioritaire sur le genre.
     */
    public Reason check(String artist, String genre) {
        if (artists == null) {
            artists = new RuleSet("artistes", artistFile);
            genres = new RuleSet("genres", genreFile);
        }
        if (artists.matches(artist)) {
            return Reason.ARTIST;
        }
        if (genres.matches(genre)) {
            return Reason.GENRE;
        }
        return null;
    }

    /** Une liste de motifs chargee depuis un fichier, rechargee a chaud. */
    private static final class RuleSet {

        private static final Logger log = LoggerFactory.getLogger(RuleSet.class);

        private final String label;
        private final String path;

        /** Motifs (deja en minuscules) charges depuis le fichier. */
        private volatile List<String> patterns = List.of();
        /** Date de derniere modification prise en compte, pour ne relire qu'au besoin. */
        private volatile long lastModified = -1L;

        RuleSet(String label, String path) {
            this.label = label;
            this.path = path;
        }

        /** La valeur correspond-elle (sous-chaine, casse ignoree) a un motif ? */
        boolean matches(String value) {
            if (value == null || value.isBlank()) {
                return false;
            }
            reloadIfNeeded();
            String needle = value.toLowerCase();
            for (String pattern : patterns) {
                if (needle.contains(pattern)) {
                    return true;
                }
            }
            return false;
        }

        /** (Re)charge le fichier si sa date de modification a change. */
        private synchronized void reloadIfNeeded() {
            File file = new File(path).getAbsoluteFile();
            if (!file.exists()) {
                if (lastModified != 0L) {
                    log.warn("Liste noire ({}) introuvable : {} (rien de bloque)", label, file.getAbsolutePath());
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
                log.info("Liste noire ({}) chargee : {} entree(s) depuis {}",
                        label, loaded.size(), file.getAbsolutePath());
            } catch (Exception e) {
                log.error("Impossible de lire la liste noire ({}) {} : {}",
                        label, file.getAbsolutePath(), e.getMessage());
            }
        }
    }
}
