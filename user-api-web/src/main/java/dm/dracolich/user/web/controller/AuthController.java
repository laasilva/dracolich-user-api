package dm.dracolich.user.web.controller;

import dm.dracolich.user.dto.auth.AuthResponse;
import dm.dracolich.user.dto.auth.LoginRequest;
import dm.dracolich.user.dto.auth.RefreshRequest;
import dm.dracolich.user.dto.auth.RegisterRequest;
import dm.dracolich.user.web.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("auth")
@Tag(name = "Authentication")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "Register a new user", description = "Creates a new user account and sends a confirmation email")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Registration successful"),
            @ApiResponse(responseCode = "409", description = "Email or username already taken")
    })
    @PostMapping(path = "/register", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<String> register(@RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @Operation(summary = "Confirm account", description = "Confirms a user account via email token")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Account confirmed"),
            @ApiResponse(responseCode = "400", description = "Invalid or expired token")
    })
    @GetMapping(path = "/confirm", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<String> confirmAccount(@RequestParam String token) {
        return authService.confirmAccount(token);
    }

    @Operation(summary = "Login", description = "Authenticates a user and returns JWT tokens")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Login successful"),
            @ApiResponse(responseCode = "401", description = "Invalid credentials"),
            @ApiResponse(responseCode = "403", description = "Account not active")
    })
    @PostMapping(path = "/login", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<AuthResponse> login(@RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @Operation(summary = "Refresh token", description = "Issues a new access token using a valid refresh token")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Token refreshed"),
            @ApiResponse(responseCode = "401", description = "Invalid or expired refresh token")
    })
    @PostMapping(path = "/refresh", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<AuthResponse> refresh(@RequestBody RefreshRequest request) {
        return authService.refresh(request.refreshToken());
    }

    @Operation(summary = "Logout", description = "Revokes the refresh token")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Logged out successfully")
    })
    @PostMapping(path = "/logout", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<String> logout(@RequestBody RefreshRequest request) {
        return authService.logout(request.refreshToken());
    }
}
