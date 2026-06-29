package s6.mp3.common;

import java.io.Serializable;

/**
 * Message produit par le Programme 2 (Extracteur) vers la file
 * {@code queue.metadata}, puis consomme par le Programme 3 (Uploader).
 * Contient le chemin du fichier et les metadonnees extraites.
 */
public class MetadataMessage implements Serializable {

    private String path;
    private String fileName;
    private String title;
    private String artist;
    private String album;
    private String genre;
    private Integer duration; // en secondes
    private String year;
    private String date;

    public MetadataMessage() {
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public String getAlbum() {
        return album;
    }

    public void setAlbum(String album) {
        this.album = album;
    }

    public String getGenre() {
        return genre;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }

    public Integer getDuration() {
        return duration;
    }

    public void setDuration(Integer duration) {
        this.duration = duration;
    }

    public String getYear() {
        return year;
    }

    public void setYear(String year) {
        this.year = year;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    @Override
    public String toString() {
        return "MetadataMessage{path='" + path + "', title='" + title + "', artist='" + artist
                + "', album='" + album + "', genre='" + genre + "', duration=" + duration
                + ", year='" + year + "'}";
    }
}
