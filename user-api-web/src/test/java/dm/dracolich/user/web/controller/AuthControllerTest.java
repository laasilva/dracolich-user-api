package dm.dracolich.user.web.controller;

import dm.dracolich.forge.error.ApiError;
import dm.dracolich.forge.exception.ResponseException;
import dm.dracolich.user.dto.ErrorCodes;
import dm.dracolich.user.dto.auth.AuthResponse;
import dm.dracolich.user.dto.auth.LoginRequest;
import dm.dracolich.user.dto.auth.RefreshRequest;
import dm.dracolich.user.dto.auth.RegisterRequest;
import dm.dracolich.user.web.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    private WebTestClient webClient;

    @Mock
    private AuthService authService;

    @BeforeEach
    void setUp() {
        AuthController controller = new AuthController(authService);
        webClient = WebTestClient.bindToController(controller)
                .controllerAdvice(new dm.dracolich.forge.controller.ControllerAdvice())
                .build();
    }

    // --- Register ---

    @Test
    void register_success_returns201() {
        when(authService.register(any(RegisterRequest.class)))
                .thenReturn(Mono.just("Registration successful."));

        webClient.post().uri("/auth/register")
                .header("Content-Type", "application/json")
                .bodyValue(new RegisterRequest("test@example.com", "testuser", "password123", "Test"))
                .exchange()
                .expectStatus().isCreated();
    }

    @Test
    void register_duplicateEmail_returns409() {
        when(authService.register(any(RegisterRequest.class)))
                .thenReturn(Mono.error(new ResponseException(
                        ErrorCodes.DMD012.getMessage(),
                        List.of(new ApiError(ErrorCodes.DMD012)),
                        HttpStatus.CONFLICT)));

        webClient.post().uri("/auth/register")
                .header("Content-Type", "application/json")
                .bodyValue(new RegisterRequest("taken@example.com", "user", "password123", "User"))
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void register_invalidEmail_returns400() {
        when(authService.register(any(RegisterRequest.class)))
                .thenReturn(Mono.error(new ResponseException(
                        ErrorCodes.DMD019.getMessage(),
                        List.of(new ApiError(ErrorCodes.DMD019)),
                        HttpStatus.BAD_REQUEST)));

        webClient.post().uri("/auth/register")
                .header("Content-Type", "application/json")
                .bodyValue(new RegisterRequest("bad-email", "user", "password123", "User"))
                .exchange()
                .expectStatus().isBadRequest();
    }

    // --- Confirm ---

    @Test
    void confirm_success_returns200() {
        when(authService.confirmAccount("valid-token"))
                .thenReturn(Mono.just("Account confirmed successfully."));

        webClient.get().uri("/auth/confirm?token=valid-token")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void confirm_invalidToken_returns400() {
        when(authService.confirmAccount("bad-token"))
                .thenReturn(Mono.error(new ResponseException(
                        ErrorCodes.DMD014.getMessage(),
                        List.of(new ApiError(ErrorCodes.DMD014)),
                        HttpStatus.BAD_REQUEST)));

        webClient.get().uri("/auth/confirm?token=bad-token")
                .exchange()
                .expectStatus().isBadRequest();
    }

    // --- Login ---

    @Test
    void login_success_returnsTokens() {
        when(authService.login(any(LoginRequest.class)))
                .thenReturn(Mono.just(new AuthResponse("access-jwt", "refresh-token", 900L)));

        webClient.post().uri("/auth/login")
                .header("Content-Type", "application/json")
                .bodyValue(new LoginRequest("testuser", "password"))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.accessToken").isEqualTo("access-jwt")
                .jsonPath("$.refreshToken").isEqualTo("refresh-token")
                .jsonPath("$.expiresIn").isEqualTo(900);
    }

    @Test
    void login_wrongCredentials_returns401() {
        when(authService.login(any(LoginRequest.class)))
                .thenReturn(Mono.error(new ResponseException(
                        ErrorCodes.DMD016.getMessage(),
                        List.of(new ApiError(ErrorCodes.DMD016)),
                        HttpStatus.UNAUTHORIZED)));

        webClient.post().uri("/auth/login")
                .header("Content-Type", "application/json")
                .bodyValue(new LoginRequest("user", "wrong"))
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void login_inactiveAccount_returns403() {
        when(authService.login(any(LoginRequest.class)))
                .thenReturn(Mono.error(new ResponseException(
                        ErrorCodes.DMD015.format("PENDING_CONFIRMATION"),
                        List.of(new ApiError(ErrorCodes.DMD015)),
                        HttpStatus.FORBIDDEN)));

        webClient.post().uri("/auth/login")
                .header("Content-Type", "application/json")
                .bodyValue(new LoginRequest("user", "pass"))
                .exchange()
                .expectStatus().isForbidden();
    }

    // --- Refresh ---

    @Test
    void refresh_success_returnsNewTokens() {
        when(authService.refresh(anyString()))
                .thenReturn(Mono.just(new AuthResponse("new-access", "new-refresh", 900L)));

        webClient.post().uri("/auth/refresh")
                .header("Content-Type", "application/json")
                .bodyValue(new RefreshRequest("old-refresh-token"))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.accessToken").isEqualTo("new-access");
    }

    @Test
    void refresh_invalidToken_returns401() {
        when(authService.refresh(anyString()))
                .thenReturn(Mono.error(new ResponseException(
                        ErrorCodes.DMD017.getMessage(),
                        List.of(new ApiError(ErrorCodes.DMD017)),
                        HttpStatus.UNAUTHORIZED)));

        webClient.post().uri("/auth/refresh")
                .header("Content-Type", "application/json")
                .bodyValue(new RefreshRequest("bad-token"))
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    // --- Logout ---

    @Test
    void logout_success_returns200() {
        when(authService.logout(anyString()))
                .thenReturn(Mono.just("Logged out successfully."));

        webClient.post().uri("/auth/logout")
                .header("Content-Type", "application/json")
                .bodyValue(new RefreshRequest("some-token"))
                .exchange()
                .expectStatus().isOk();
    }
}
