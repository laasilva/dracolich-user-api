package dm.dracolich.user.dto.auth;

public record AuthResponse(String accessToken,
                           String refreshToken,
                           Long expiresIn) {
}
