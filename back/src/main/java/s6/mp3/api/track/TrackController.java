package s6.mp3.api.track;

import org.springframework.context.annotation.Profile;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import s6.mp3.api.track.dto.TrackDtos.TrackDto;
import s6.mp3.api.track.dto.TrackDtos.TrackUpdateRequest;
import s6.mp3.api.track.dto.TrackDtos.UploadMetadata;

import java.util.List;

@RestController
@RequestMapping("/api/tracks")
@Profile("api")
public class TrackController {

    private final TrackService trackService;

    public TrackController(TrackService trackService) {
        this.trackService = trackService;
    }

    /** Import depuis le Programme 3 : multipart (file + metadata JSON). */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<TrackDto> upload(@RequestPart("file") MultipartFile file,
                                           @RequestPart("metadata") UploadMetadata metadata) {
        Track saved = trackService.upload(file, metadata);
        return ResponseEntity.ok(TrackDto.from(saved));
    }

    /** Ajout manuel depuis le web (multipart : fichier + champs texte). */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public TrackDto create(@RequestPart("file") MultipartFile file,
                           @RequestParam(required = false) String title,
                           @RequestParam(required = false) String artist,
                           @RequestParam(required = false) String album,
                           @RequestParam(required = false) String genre,
                           @RequestParam(required = false) Integer duration,
                           @RequestParam(required = false) String year) {
        UploadMetadata meta = new UploadMetadata(null, file.getOriginalFilename(),
                title, artist, album, genre, duration, year, year);
        return TrackDto.from(trackService.upload(file, meta));
    }

    /** Liste avec filtres optionnels. */
    @GetMapping
    public List<TrackDto> list(@RequestParam(required = false) String genre,
                               @RequestParam(required = false) String artist,
                               @RequestParam(required = false) String album,
                               @RequestParam(required = false) String q) {
        return trackService.search(genre, artist, album, q).stream()
                .map(TrackDto::from)
                .toList();
    }

    @GetMapping("/{id}")
    public TrackDto get(@PathVariable Long id) {
        return TrackDto.from(trackService.get(id));
    }

    /** Lecture / streaming du fichier MP3. */
    @GetMapping("/{id}/stream")
    public ResponseEntity<Resource> stream(@PathVariable Long id) {
        Track track = trackService.get(id);
        Resource resource = trackService.loadResource(track);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("audio/mpeg"))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" + track.getFileName() + "\"")
                .body(resource);
    }

    @PutMapping("/{id}")
    public TrackDto update(@PathVariable Long id, @RequestBody TrackUpdateRequest request) {
        return TrackDto.from(trackService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        trackService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
