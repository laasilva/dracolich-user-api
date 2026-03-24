package dm.dracolich.user.web.service;

import dm.dracolich.user.web.entity.UserEntity;
import io.jsonwebtoken.Claims;
import reactor.core.publisher.Mono;

public interface JwtService {
    String generateAccessToken(UserEntity user);
    String generateRefreshToken();
    String generateConfirmationToken(String userId);
    Mono<Claims> validateAccessToken(String token);
    Mono<String> validateConfirmationToken(String token);
}
