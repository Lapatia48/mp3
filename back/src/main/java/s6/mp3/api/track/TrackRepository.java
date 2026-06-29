package s6.mp3.api.track;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TrackRepository extends JpaRepository<Track, Long> {

    Optional<Track> findByContentHash(String contentHash);

    /**
     * Recherche filtree : chaque critere est optionnel (ignore si null).
     * {@code q} cherche dans le titre, l'artiste ou l'album.
     */
    /**
     * Les parametres sont deja en minuscules (et {@code q} sous forme
     * {@code %motif%}), passes a null si absents. On evite {@code lower(:param)}
     * sur un parametre null (PostgreSQL le typerait en bytea -> erreur).
     */
    @Query("""
            select t from Track t
            where (:genre  is null or lower(t.genre)  = :genre)
              and (:artist is null or lower(t.artist) = :artist)
              and (:album  is null or lower(t.album)  = :album)
              and (:q is null
                   or lower(t.title)  like :q
                   or lower(t.artist) like :q
                   or lower(t.album)  like :q)
            order by t.artist, t.album, t.title
            """)
    List<Track> search(@Param("genre") String genre,
                       @Param("artist") String artist,
                       @Param("album") String album,
                       @Param("q") String q);
}
