package dm.dracolich.user.web.service;

import dm.dracolich.user.dto.AccessLevelEnum;
import dm.dracolich.user.web.config.JwtConfig;
import dm.dracolich.user.web.entity.UserEntity;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceImplTest {

    private JwtServiceImpl jwtService;
    private JwtConfig jwtConfig;

    @BeforeEach
    void setUp() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
        generator.initialize(new ECGenParameterSpec("secp384r1"));
        KeyPair keyPair = generator.generateKeyPair();

        jwtConfig = new JwtConfig();
        jwtConfig.setEcPrivateKey((ECPrivateKey) keyPair.getPrivate());
        jwtConfig.setEcPublicKey((ECPublicKey) keyPair.getPublic());
        jwtConfig.setAccessTokenExpiration(900);
        jwtConfig.setRefreshTokenExpiration(604800);
        jwtConfig.setConfirmationTokenExpiration(86400);

        jwtService = new JwtServiceImpl(jwtConfig);
    }

    private UserEntity buildUser() {
        return UserEntity.builder()
                .id("user-123")
                .username("testuser")
                .accessLevel(AccessLevelEnum.COMMON_USER)
                .build();
    }

    @Test
    void generateAccessToken_containsExpectedClaims() {
        UserEntity user = buildUser();

        String token = jwtService.generateAccessToken(user);

        assertNotNull(token);
        assertFalse(token.isBlank());

        StepVerifier.create(jwtService.validateAccessToken(token))
                .assertNext(claims -> {
                    assertEquals("user-123", claims.getSubject());
                    assertEquals("testuser", claims.get("username", String.class));
                    assertEquals("COMMON_USER", claims.get("accessLevel", String.class));
                    assertNotNull(claims.getIssuedAt());
                    assertNotNull(claims.getExpiration());
                })
                .verifyComplete();
    }

    @Test
    void generateAccessToken_differentUsersProduceDifferentTokens() {
        UserEntity user1 = buildUser();
        UserEntity user2 = UserEntity.builder()
                .id("user-456")
                .username("otheruser")
                .accessLevel(AccessLevelEnum.ADMIN)
                .build();

        String token1 = jwtService.generateAccessToken(user1);
        String token2 = jwtService.generateAccessToken(user2);

        assertNotEquals(token1, token2);
    }

    @Test
    void generateRefreshToken_isUniqueAndCorrectLength() {
        String token1 = jwtService.generateRefreshToken();
        String token2 = jwtService.generateRefreshToken();

        assertNotNull(token1);
        assertNotNull(token2);
        assertNotEquals(token1, token2);
        assertTrue(token1.length() > 20);
    }

    @Test
    void generateConfirmationToken_containsPurposeClaim() {
        String token = jwtService.generateConfirmationToken("user-123");

        StepVerifier.create(jwtService.validateConfirmationToken(token))
                .expectNext("user-123")
                .verifyComplete();
    }

    @Test
    void validateConfirmationToken_rejectsAccessToken() {
        UserEntity user = buildUser();
        String accessToken = jwtService.generateAccessToken(user);

        StepVerifier.create(jwtService.validateConfirmationToken(accessToken))
                .verifyComplete(); // empty Mono — no purpose claim
    }

    @Test
    void validateAccessToken_rejectsInvalidToken() {
        StepVerifier.create(jwtService.validateAccessToken("invalid.token.here"))
                .expectError()
                .verify();
    }

    @Test
    void validateAccessToken_rejectsTokenSignedWithDifferentKey() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
        generator.initialize(new ECGenParameterSpec("secp384r1"));
        KeyPair otherKeyPair = generator.generateKeyPair();

        JwtConfig otherConfig = new JwtConfig();
        otherConfig.setEcPrivateKey((ECPrivateKey) otherKeyPair.getPrivate());
        otherConfig.setEcPublicKey((ECPublicKey) otherKeyPair.getPublic());
        otherConfig.setAccessTokenExpiration(900);
        JwtServiceImpl otherService = new JwtServiceImpl(otherConfig);

        String tokenFromOtherKey = otherService.generateAccessToken(buildUser());

        StepVerifier.create(jwtService.validateAccessToken(tokenFromOtherKey))
                .expectError()
                .verify();
    }

    @Test
    void validate_returnsClaimsForValidToken() {
        UserEntity user = buildUser();
        String token = jwtService.generateAccessToken(user);

        StepVerifier.create(jwtService.validate(token))
                .assertNext(claims -> assertEquals("user-123", claims.getSubject()))
                .verifyComplete();
    }
}
