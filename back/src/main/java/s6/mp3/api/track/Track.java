package s6.mp3.api.track;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "tracks")
@Getter
@Setter
@NoArgsConstructor
public class Track {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String artist;
    private String album;
    private String genre;

    /** Duree en secondes. */
    private Integer duration;

    private String year;

    @Column(name = "track_date")
    private String date;

    @Column(nullable = false)
    private String fileName;

    /** Chemin dans le stockage permanent. */
    @Column(nullable = false)
    private String storagePath;

    /** Empreinte SHA-256 du contenu, pour la deduplication / idempotence. */
    @Column(unique = true, length = 64)
    private String contentHash;

    private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
