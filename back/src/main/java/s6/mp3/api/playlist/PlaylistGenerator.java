package s6.mp3.api.playlist;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import s6.mp3.api.track.Track;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Compose une playlist a partir de trois groupes deja filtres en amont :
 * <ul>
 *   <li>{@code mandatory} : morceaux obligatoires (albums « uniquement »),
 *       toujours inclus en entier, meme si la duree visee est depassee ;</li>
 *   <li>{@code preferred} : graines prioritaires (artistes/albums inclus) ;</li>
 *   <li>{@code others} : reste du pool autorise, pour completer la duree.</li>
 * </ul>
 * La somme des durees vise la fourchette {@code [minSeconds, maxSeconds]}.
 * Aucun morceau hors de ces trois listes n'est ajoute : les exclusions decidees
 * en amont sont donc absolues.
 */
@Component
@Profile("api")
public class PlaylistGenerator {

    public List<Track> generate(List<Track> mandatory,
                                List<Track> preferred,
                                List<Track> others,
                                int minSeconds,
                                int maxSeconds,
                                boolean stopAfterMandatory) {
        List<Track> selected = new ArrayList<>();
        Set<Long> used = new HashSet<>();
        int sum = 0;

        // Morceaux obligatoires : albums « uniquement » pris en entier.
        for (Track t : mandatory) {
            if (t.getId() != null && !used.add(t.getId())) {
                continue;
            }
            selected.add(t);
            sum += duration(t);
        }
        // « album uniquement » seul : on s'arrete sur l'album, sans completer.
        if (stopAfterMandatory) {
            return selected;
        }

        // Graines d'abord, puis le reste — chaque groupe melange pour varier.
        List<Track> pref = new ArrayList<>(preferred);
        List<Track> rest = new ArrayList<>(others);
        Collections.shuffle(pref);
        Collections.shuffle(rest);
        List<Track> queue = new ArrayList<>(pref);
        queue.addAll(rest);

        // Remplissage glouton sans depasser le maximum.
        for (Track t : queue) {
            if (sum >= maxSeconds) {
                break;
            }
            if (t.getId() != null && used.contains(t.getId())) {
                continue;
            }
            int d = duration(t);
            if (d <= 0) {
                continue;
            }
            if (sum + d <= maxSeconds) {
                selected.add(t);
                if (t.getId() != null) {
                    used.add(t.getId());
                }
                sum += d;
            }
        }

        // Si on n'atteint pas le minimum, comble l'ecart avec le morceau le plus
        // adapte (leger depassement du max tolere). On reste dans le pool autorise.
        if (sum < minSeconds) {
            int gap = minSeconds - sum;
            Track best = null;
            int bestDiff = Integer.MAX_VALUE;
            for (Track t : queue) {
                if (t.getId() != null && used.contains(t.getId())) {
                    continue;
                }
                int d = duration(t);
                if (d <= 0) {
                    continue;
                }
                int diff = Math.abs(gap - d);
                if (diff < bestDiff) {
                    bestDiff = diff;
                    best = t;
                }
            }
            if (best != null) {
                selected.add(best);
            }
        }
        return selected;
    }

    private int duration(Track t) {
        return (t.getDuration() != null && t.getDuration() > 0) ? t.getDuration() : 0;
    }
}
