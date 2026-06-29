package s6.mp3.api.playlist;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import s6.mp3.api.track.Track;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Selectionne un sous-ensemble de morceaux dont la somme des durees tombe dans
 * la fourchette [min, max] (approche gloutonne avec ajustement final).
 */
@Component
@Profile("api")
public class PlaylistGenerator {

    public List<Track> generate(List<Track> candidates, int minSeconds, int maxSeconds) {
        List<Track> pool = new ArrayList<>();
        for (Track t : candidates) {
            if (t.getDuration() != null && t.getDuration() > 0) {
                pool.add(t);
            }
        }
        Collections.shuffle(pool);

        // Remplissage glouton sans depasser le maximum.
        List<Track> selected = new ArrayList<>();
        int sum = 0;
        for (Track t : pool) {
            if (sum >= maxSeconds) {
                break;
            }
            if (sum + t.getDuration() <= maxSeconds) {
                selected.add(t);
                sum += t.getDuration();
            }
        }

        // Si on n'atteint pas le minimum, comble l'ecart avec le morceau le plus
        // adapte (leger depassement du max tolere pour respecter le min).
        if (sum < minSeconds) {
            int gap = minSeconds - sum;
            Track best = null;
            int bestDiff = Integer.MAX_VALUE;
            for (Track t : pool) {
                if (selected.contains(t)) {
                    continue;
                }
                int diff = Math.abs(gap - t.getDuration());
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
}
