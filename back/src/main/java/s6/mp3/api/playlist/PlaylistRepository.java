package s6.mp3.api.playlist;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PlaylistRepository extends JpaRepository<Playlist, Long> {

    List<Playlist> findByOwnerIdOrderByCreatedAtDesc(Long ownerId);

    Optional<Playlist> findByIdAndOwnerId(Long id, Long ownerId);
}
