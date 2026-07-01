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
 * <p>Trois fichiers :
 * <ul>
 *   <li>{@code blacklist/artiste.txt} — artistes a bloquer ;</li>
 *   <li>{@code blacklist/genre.txt} — genres a bloquer ;</li>
 *   <li>{@code blacklist/duree.txt} — <b>duree maximale</b> autorisee, un seul
 *       nombre entier exprime <b>en secondes</b> (ex. {@code 180} = 3 min). Tout
 *       morceau plus long est blackliste.</li>
 * </ul>
 * Pour l'artiste et le genre, un motif par ligne. La comparaison est
 * <b>insensible a la casse</b> et porte sur une <b>sous-chaine</b> : la ligne
 * {@code skaiz} bloque l'artiste {@code "Skaiz Official"}, la ligne {@code rock}
 * bloque le genre {@code "Pop-Rock"}. Les lignes vides et celles commencant par
 * {@code #} (commentaires) sont ignorees.
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
        GENRE("genres"),
        DURATION("duree");

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

    @Value("${app.blacklist-duration-file:blacklist/duree.txt}")
    private String durationFile;

    private RuleSet artists;
    private RuleSet genres;
    private DurationLimit maxDuration;

    /**
     * Determine si le morceau doit etre blackliste, et pour quelle raison.
     *
     * @param duration duree du morceau en secondes (peut etre {@code null}).
     * @return {@link Reason#ARTIST}, {@link Reason#GENRE}, {@link Reason#DURATION},
     *         ou {@code null} si le morceau est autorise. Priorite : artiste,
     *         puis genre, puis duree.
     */
    public Reason check(String artist, String genre, Integer duration) {
        if (artists == null) {
            artists = new RuleSet("artistes", artistFile);
            genres = new RuleSet("genres", genreFile);
            maxDuration = new DurationLimit(durationFile);
        }
        if (artists.matches(artist)) {
            return Reason.ARTIST;
        }
        if (genres.matches(genre)) {
            return Reason.GENRE;
        }
        if (maxDuration.exceededBy(duration)) {
            return Reason.DURATION;
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

    /**
     * Duree maximale autorisee (secondes), chargee depuis un fichier a nombre
     * unique et rechargee a chaud. {@code <= 0} ou fichier absent = aucune limite.
     */
    private static final class DurationLimit {

        private static final Logger log = LoggerFactory.getLogger(DurationLimit.class);

        private final String path;

        /** Limite en secondes ; {@code 0} signifie « pas de limite ». */
        private volatile int maxSeconds = 0;
        private volatile long lastModified = -1L;

        DurationLimit(String path) {
            this.path = path;
        }

        /** La duree (secondes) depasse-t-elle la limite configuree ? */
        boolean exceededBy(Integer duration) {
            if (duration == null || duration <= 0) {
                return false;
            }
            reloadIfNeeded();
            return maxSeconds > 0 && duration > maxSeconds;
        }

        /** (Re)charge le fichier si sa date de modification a change. */
        private synchronized void reloadIfNeeded() {
            File file = new File(path).getAbsoluteFile();
            if (!file.exists()) {
                if (lastModified != 0L) {
                    log.warn("Duree maximale ({}) introuvable : {} (aucune limite)", "duree", file.getAbsolutePath());
                    maxSeconds = 0;
                    lastModified = 0L;
                }
                return;
            }
            long modified = file.lastModified();
            if (modified == lastModified) {
                return;
            }
            int loaded = 0;
            try {
                for (String line : Files.readAllLines(file.toPath(), StandardCharsets.UTF_8)) {
                    String entry = line.trim();
                    if (entry.isEmpty() || entry.startsWith("#")) {
                        continue;
                    }
                    loaded = Integer.parseInt(entry); // premier nombre = limite
                    break;
                }
                maxSeconds = Math.max(0, loaded);
                lastModified = modified;
                log.info("Duree maximale chargee : {}s depuis {}", maxSeconds, file.getAbsolutePath());
            } catch (Exception e) {
                log.error("Impossible de lire la duree maximale {} : {}",
                        file.getAbsolutePath(), e.getMessage());
            }
        }
    }
}
