package dm.dracolich.user.web.service;

import dm.dracolich.forge.security.JwtTokenValidator;
import dm.dracolich.user.web.config.JwtConfig;
import dm.dracolich.user.web.entity.UserEntity;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;

@Service
@RequiredArgsConstructor
public class JwtServiceImpl implements JwtService, JwtTokenValidator {

    private final JwtConfig jwtConfig;
    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    public String generateAccessToken(UserEntity user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(user.getId())
                .claim("username", user.getUsername())
                .claim("accessLevel", user.getAccessLevel().name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(jwtConfig.getAccessTokenExpiration())))
                .signWith(jwtConfig.getEcPrivateKey(), Jwts.SIG.ES384)
                .compact();
    }

    @Override
    public String generateRefreshToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    @Override
    public String generateConfirmationToken(String userId) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(userId)
                .claim("purpose", "email-confirmation")
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(jwtConfig.getConfirmationTokenExpiration())))
                .signWith(jwtConfig.getEcPrivateKey(), Jwts.SIG.ES384)
                .compact();
    }

    @Override
    public Mono<Claims> validateAccessToken(String token) {
        return validate(token);
    }

    @Override
    public Mono<String> validateConfirmationToken(String token) {
        return validate(token)
                .filter(claims -> "email-confirmation".equals(claims.get("purpose", String.class)))
                .map(Claims::getSubject);
    }

    @Override
    public Mono<Claims> validate(String token) {
        return Mono.fromCallable(() ->
                Jwts.parser()
                        .verifyWith(jwtConfig.getEcPublicKey())
                        .build()
                        .parseSignedClaims(token)
                        .getPayload()
        );
    }
}
