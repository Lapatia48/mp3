package s6.mp3.api.user;

import jakarta.validation.Valid;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import s6.mp3.api.user.dto.AuthDtos.AuthResponse;
import s6.mp3.api.user.dto.AuthDtos.LoginRequest;
import s6.mp3.api.user.dto.AuthDtos.RegisterRequest;

@RestController
@RequestMapping("/api/auth")
@Profile("api")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }
}
