package dm.dracolich.user.web.service;

import dm.dracolich.user.dto.auth.AuthResponse;
import dm.dracolich.user.dto.auth.LoginRequest;
import dm.dracolich.user.dto.auth.RegisterRequest;
import reactor.core.publisher.Mono;

public interface AuthService {
    Mono<String> register(RegisterRequest request);
    Mono<String> confirmAccount(String token);
    Mono<AuthResponse> login(LoginRequest request);
    Mono<AuthResponse> refresh(String refreshToken);
    Mono<String> logout(String refreshToken);
}
