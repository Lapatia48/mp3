package s6.mp3.common;

import java.io.Serializable;

/**
 * Message produit par le Programme 1 (Scanner) vers la file {@code queue.scan}.
 * Contient le nom et le chemin absolu d'un fichier MP3 detecte.
 */
public class ScanMessage implements Serializable {

    private String fileName;
    private String path;

    public ScanMessage() {
    }

    public ScanMessage(String fileName, String path) {
        this.fileName = fileName;
        this.path = path;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    @Override
    public String toString() {
        return "ScanMessage{fileName='" + fileName + "', path='" + path + "'}";
    }
}
