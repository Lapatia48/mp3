package s6.mp3.api.track;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;

/**
 * Stockage permanent des fichiers MP3 sur disque.
 */
@Service
@Profile("api")
public class StorageService {

    private static final Logger log = LoggerFactory.getLogger(StorageService.class);

    private final Path root;

    public StorageService(@Value("${app.storage-dir}") String storageDir) {
        this.root = Paths.get(storageDir).toAbsolutePath().normalize();
    }

    @PostConstruct
    void init() {
        try {
            Files.createDirectories(root);
            log.info("Stockage permanent : {}", root);
        } catch (IOException e) {
            throw new UncheckedIOException("Impossible de creer le repertoire de stockage : " + root, e);
        }
    }

    /** Resultat d'un stockage : nom de fichier genere, chemin relatif et empreinte. */
    public record Stored(String fileName, String storagePath, String hash) {
    }

    /** Enregistre le contenu et renvoie ses references. Nom de fichier unique (UUID). */
    public Stored store(byte[] content, String originalName) {
        String ext = extensionOf(originalName);
        String stored = UUID.randomUUID() + ext;
        Path target = root.resolve(stored);
        try {
            Files.write(target, content);
        } catch (IOException e) {
            throw new UncheckedIOException("Echec d'ecriture du fichier : " + target, e);
        }
        return new Stored(originalName, stored, sha256(content));
    }

    public Stored store(MultipartFile file) {
        try {
            return store(file.getBytes(), file.getOriginalFilename());
        } catch (IOException e) {
            throw new UncheckedIOException("Echec de lecture du fichier uploade", e);
        }
    }

    public Resource loadAsResource(String storagePath) {
        return new FileSystemResource(root.resolve(storagePath));
    }

    public byte[] readBytes(String storagePath) {
        try {
            return Files.readAllBytes(root.resolve(storagePath));
        } catch (IOException e) {
            throw new UncheckedIOException("Echec de lecture du fichier stocke : " + storagePath, e);
        }
    }

    public void delete(String storagePath) {
        try {
            Files.deleteIfExists(root.resolve(storagePath));
        } catch (IOException e) {
            log.warn("Impossible de supprimer le fichier stocke {} : {}", storagePath, e.getMessage());
        }
    }

    public String sha256(byte[] content) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(content));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private String extensionOf(String name) {
        if (name == null) {
            return ".mp3";
        }
        int dot = name.lastIndexOf('.');
        return dot >= 0 ? name.substring(dot) : ".mp3";
    }
}
