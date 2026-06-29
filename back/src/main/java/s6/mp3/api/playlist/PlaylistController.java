package s6.mp3.api.playlist;

import jakarta.validation.Valid;
import org.springframework.context.annotation.Profile;
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
import org.springframework.web.bind.annotation.RestController;
import s6.mp3.api.playlist.dto.PlaylistDtos.GenerateRequest;
import s6.mp3.api.playlist.dto.PlaylistDtos.PlaylistDto;
import s6.mp3.api.playlist.dto.PlaylistDtos.SavePlaylistRequest;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/playlists")
@Profile("api")
public class PlaylistController {

    private final PlaylistService playlistService;

    public PlaylistController(PlaylistService playlistService) {
        this.playlistService = playlistService;
    }

    /** Generation automatique selon criteres (resultat non sauvegarde). */
    @PostMapping("/generate")
    public PlaylistDto generate(@RequestBody GenerateRequest request) {
        return playlistService.generate(request);
    }

    @PostMapping
    public PlaylistDto save(@Valid @RequestBody SavePlaylistRequest request, Principal principal) {
        return playlistService.save(principal.getName(), request);
    }

    @GetMapping
    public List<PlaylistDto> list(Principal principal) {
        return playlistService.list(principal.getName());
    }

    @GetMapping("/{id}")
    public PlaylistDto get(@PathVariable Long id, Principal principal) {
        return playlistService.get(principal.getName(), id);
    }

    @PutMapping("/{id}")
    public PlaylistDto update(@PathVariable Long id,
                             @Valid @RequestBody SavePlaylistRequest request,
                             Principal principal) {
        return playlistService.update(principal.getName(), id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, Principal principal) {
        playlistService.delete(principal.getName(), id);
        return ResponseEntity.noContent().build();
    }

    /** Telechargement de tous les morceaux dans une archive ZIP. */
    @GetMapping("/{id}/download")
    public ResponseEntity<byte[]> download(@PathVariable Long id, Principal principal) {
        byte[] zip = playlistService.zip(principal.getName(), id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/zip"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"playlist.zip\"")
                .body(zip);
    }
}
