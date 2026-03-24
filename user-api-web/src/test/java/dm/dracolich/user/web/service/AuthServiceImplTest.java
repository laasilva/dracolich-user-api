package dm.dracolich.user.web.service;

import dm.dracolich.forge.exception.ResponseException;
import dm.dracolich.user.dto.AccessLevelEnum;
import dm.dracolich.user.dto.AccountStatusEnum;
import dm.dracolich.user.dto.auth.LoginRequest;
import dm.dracolich.user.dto.auth.RegisterRequest;
import dm.dracolich.user.web.entity.MetadataEntity;
import dm.dracolich.user.web.entity.RefreshTokenEntity;
import dm.dracolich.user.web.entity.UserEntity;
import dm.dracolich.user.web.entity.UserProfileEntity;
import dm.dracolich.user.web.repository.RefreshTokenRepository;
import dm.dracolich.user.web.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private JwtService jwtService;
    @Mock
    private EmailService emailService;
    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthServiceImpl authService;

    private UserEntity activeUser;

    @BeforeEach
    void setUp() {
        activeUser = UserEntity.builder()
                .id("user-123")
                .email("test@example.com")
                .username("testuser")
                .userHash(1234)
                .password("$2a$10$hashedpassword")
                .accessLevel(AccessLevelEnum.COMMON_USER)
                .accountStatus(AccountStatusEnum.ACTIVE)
                .profile(UserProfileEntity.builder().displayName("Test").build())
                .metadata(MetadataEntity.builder()
                        .createdAt(Instant.now())
                        .updatedAt(Instant.now())
                        .build())
                .build();
    }

    @Nested
    class Register {

        @Test
        void success() {
            var request = new RegisterRequest("new@example.com", "newuser", "password123", "New User");

            when(userRepository.existsByEmail("new@example.com")).thenReturn(Mono.just(false));
            when(userRepository.existsByUsername("newuser")).thenReturn(Mono.just(false));
            when(passwordEncoder.encode("password123")).thenReturn("$2a$10$encoded");
            when(userRepository.save(any(UserEntity.class))).thenAnswer(inv -> {
                UserEntity user = inv.getArgument(0);
                user.setId("new-id");
                return Mono.just(user);
            });
            when(jwtService.generateConfirmationToken("new-id")).thenReturn("conf-token");
            when(emailService.sendConfirmationEmail("new@example.com", "conf-token")).thenReturn(Mono.empty());

            StepVerifier.create(authService.register(request))
                    .assertNext(msg -> assertTrue(msg.contains("Registration successful")))
                    .verifyComplete();

            verify(userRepository).save(any(UserEntity.class));
            verify(emailService).sendConfirmationEmail("new@example.com", "conf-token");
        }

        @Test
        void emailAlreadyTaken_returnsConflict() {
            var request = new RegisterRequest("taken@example.com", "newuser", "password123", "User");

            when(userRepository.existsByEmail("taken@example.com")).thenReturn(Mono.just(true));

            StepVerifier.create(authService.register(request))
                    .expectErrorMatches(e -> e instanceof ResponseException re
                            && re.getHttpStatus() == HttpStatus.CONFLICT
                            && re.getMessage().contains("Email"))
                    .verify();
        }

        @Test
        void usernameAlreadyTaken_returnsConflict() {
            var request = new RegisterRequest("new@example.com", "taken", "password123", "User");

            when(userRepository.existsByEmail("new@example.com")).thenReturn(Mono.just(false));
            when(userRepository.existsByUsername("taken")).thenReturn(Mono.just(true));

            StepVerifier.create(authService.register(request))
                    .expectErrorMatches(e -> e instanceof ResponseException re
                            && re.getHttpStatus() == HttpStatus.CONFLICT
                            && re.getMessage().contains("Username"))
                    .verify();
        }

        @Test
        void invalidEmail_returnsBadRequest() {
            var request = new RegisterRequest("not-an-email", "user", "password123", "User");

            StepVerifier.create(authService.register(request))
                    .expectErrorMatches(e -> e instanceof ResponseException re
                            && re.getHttpStatus() == HttpStatus.BAD_REQUEST)
                    .verify();
        }

        @Test
        void nullEmail_returnsBadRequest() {
            var request = new RegisterRequest(null, "user", "password123", "User");

            StepVerifier.create(authService.register(request))
                    .expectErrorMatches(e -> e instanceof ResponseException re
                            && re.getHttpStatus() == HttpStatus.BAD_REQUEST)
                    .verify();
        }

        @Test
        void shortPassword_returnsBadRequest() {
            var request = new RegisterRequest("test@example.com", "user", "short", "User");

            StepVerifier.create(authService.register(request))
                    .expectErrorMatches(e -> e instanceof ResponseException re
                            && re.getHttpStatus() == HttpStatus.BAD_REQUEST)
                    .verify();
        }

        @Test
        void blankUsername_returnsBadRequest() {
            var request = new RegisterRequest("test@example.com", "  ", "password123", "User");

            StepVerifier.create(authService.register(request))
                    .expectErrorMatches(e -> e instanceof ResponseException re
                            && re.getHttpStatus() == HttpStatus.BAD_REQUEST)
                    .verify();
        }

        @Test
        void emailSendFailure_stillReturnsSuccess() {
            var request = new RegisterRequest("new@example.com", "newuser", "password123", "User");

            when(userRepository.existsByEmail("new@example.com")).thenReturn(Mono.just(false));
            when(userRepository.existsByUsername("newuser")).thenReturn(Mono.just(false));
            when(passwordEncoder.encode("password123")).thenReturn("$2a$10$encoded");
            when(userRepository.save(any(UserEntity.class))).thenAnswer(inv -> {
                UserEntity user = inv.getArgument(0);
                user.setId("new-id");
                return Mono.just(user);
            });
            when(jwtService.generateConfirmationToken("new-id")).thenReturn("conf-token");
            when(emailService.sendConfirmationEmail(anyString(), anyString()))
                    .thenReturn(Mono.error(new RuntimeException("SMTP down")));

            StepVerifier.create(authService.register(request))
                    .assertNext(msg -> assertTrue(msg.contains("could not be sent")))
                    .verifyComplete();
        }
    }

    @Nested
    class ConfirmAccount {

        @Test
        void success() {
            UserEntity pendingUser = UserEntity.builder()
                    .id("user-123")
                    .accountStatus(AccountStatusEnum.PENDING_CONFIRMATION)
                    .metadata(MetadataEntity.builder()
                            .createdAt(Instant.now())
                            .updatedAt(Instant.now())
                            .build())
                    .build();

            when(jwtService.validateConfirmationToken("valid-token")).thenReturn(Mono.just("user-123"));
            when(userRepository.findById("user-123")).thenReturn(Mono.just(pendingUser));
            when(userRepository.save(any(UserEntity.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

            StepVerifier.create(authService.confirmAccount("valid-token"))
                    .expectNext("Account confirmed successfully.")
                    .verifyComplete();

            assertEquals(AccountStatusEnum.ACTIVE, pendingUser.getAccountStatus());
        }

        @Test
        void invalidToken_returnsBadRequest() {
            when(jwtService.validateConfirmationToken("bad-token")).thenReturn(Mono.empty());

            StepVerifier.create(authService.confirmAccount("bad-token"))
                    .expectErrorMatches(e -> e instanceof ResponseException re
                            && re.getHttpStatus() == HttpStatus.BAD_REQUEST)
                    .verify();
        }

        @Test
        void userNotFound_returnsBadRequest() {
            when(jwtService.validateConfirmationToken("orphan-token")).thenReturn(Mono.just("deleted-user"));
            when(userRepository.findById("deleted-user")).thenReturn(Mono.empty());

            StepVerifier.create(authService.confirmAccount("orphan-token"))
                    .expectErrorMatches(e -> e instanceof ResponseException re
                            && re.getHttpStatus() == HttpStatus.BAD_REQUEST)
                    .verify();
        }
    }

    @Nested
    class Login {

        @Test
        void success() {
            when(userRepository.findByUsername("testuser")).thenReturn(Mono.just(activeUser));
            when(passwordEncoder.matches("correctpass", "$2a$10$hashedpassword")).thenReturn(true);
            when(jwtService.generateAccessToken(activeUser)).thenReturn("access-jwt");
            when(jwtService.generateRefreshToken()).thenReturn("refresh-opaque");
            when(refreshTokenRepository.save(any(RefreshTokenEntity.class)))
                    .thenAnswer(inv -> Mono.just(inv.getArgument(0)));

            StepVerifier.create(authService.login(new LoginRequest("testuser", "correctpass")))
                    .assertNext(response -> {
                        assertEquals("access-jwt", response.accessToken());
                        assertEquals("refresh-opaque", response.refreshToken());
                        assertEquals(900L, response.expiresIn());
                    })
                    .verifyComplete();
        }

        @Test
        void wrongPassword_returnsUnauthorized() {
            when(userRepository.findByUsername("testuser")).thenReturn(Mono.just(activeUser));
            when(passwordEncoder.matches("wrongpass", "$2a$10$hashedpassword")).thenReturn(false);

            StepVerifier.create(authService.login(new LoginRequest("testuser", "wrongpass")))
                    .expectErrorMatches(e -> e instanceof ResponseException re
                            && re.getHttpStatus() == HttpStatus.UNAUTHORIZED)
                    .verify();
        }

        @Test
        void userNotFound_returnsUnauthorized() {
            when(userRepository.findByUsername("ghost")).thenReturn(Mono.empty());

            StepVerifier.create(authService.login(new LoginRequest("ghost", "password")))
                    .expectErrorMatches(e -> e instanceof ResponseException re
                            && re.getHttpStatus() == HttpStatus.UNAUTHORIZED)
                    .verify();
        }

        @Test
        void accountNotActive_returnsForbidden() {
            activeUser.setAccountStatus(AccountStatusEnum.PENDING_CONFIRMATION);

            when(userRepository.findByUsername("testuser")).thenReturn(Mono.just(activeUser));
            when(passwordEncoder.matches("correctpass", "$2a$10$hashedpassword")).thenReturn(true);

            StepVerifier.create(authService.login(new LoginRequest("testuser", "correctpass")))
                    .expectErrorMatches(e -> e instanceof ResponseException re
                            && re.getHttpStatus() == HttpStatus.FORBIDDEN)
                    .verify();
        }

        @Test
        void suspendedAccount_returnsForbidden() {
            activeUser.setAccountStatus(AccountStatusEnum.SUSPENDED);

            when(userRepository.findByUsername("testuser")).thenReturn(Mono.just(activeUser));
            when(passwordEncoder.matches("correctpass", "$2a$10$hashedpassword")).thenReturn(true);

            StepVerifier.create(authService.login(new LoginRequest("testuser", "correctpass")))
                    .expectErrorMatches(e -> e instanceof ResponseException re
                            && re.getHttpStatus() == HttpStatus.FORBIDDEN)
                    .verify();
        }
    }

    @Nested
    class Refresh {

        @Test
        void success() {
            RefreshTokenEntity storedToken = RefreshTokenEntity.builder()
                    .id("token-id")
                    .userId("user-123")
                    .tokenHash("some-hash")
                    .expiresAt(Instant.now().plusSeconds(3600))
                    .revoked(false)
                    .build();

            when(refreshTokenRepository.findByTokenHashAndRevokedFalse(anyString()))
                    .thenReturn(Mono.just(storedToken));
            when(userRepository.findById("user-123")).thenReturn(Mono.just(activeUser));
            when(jwtService.generateAccessToken(activeUser)).thenReturn("new-access-jwt");
            when(jwtService.generateRefreshToken()).thenReturn("new-refresh");
            when(refreshTokenRepository.save(any(RefreshTokenEntity.class)))
                    .thenAnswer(inv -> Mono.just(inv.getArgument(0)));

            StepVerifier.create(authService.refresh("valid-refresh-token"))
                    .assertNext(response -> {
                        assertEquals("new-access-jwt", response.accessToken());
                        assertEquals("new-refresh", response.refreshToken());
                    })
                    .verifyComplete();
        }

        @Test
        void invalidToken_returnsUnauthorized() {
            when(refreshTokenRepository.findByTokenHashAndRevokedFalse(anyString()))
                    .thenReturn(Mono.empty());

            StepVerifier.create(authService.refresh("bad-token"))
                    .expectErrorMatches(e -> e instanceof ResponseException re
                            && re.getHttpStatus() == HttpStatus.UNAUTHORIZED)
                    .verify();
        }

        @Test
        void expiredToken_returnsUnauthorized() {
            RefreshTokenEntity expired = RefreshTokenEntity.builder()
                    .userId("user-123")
                    .expiresAt(Instant.now().minusSeconds(3600))
                    .revoked(false)
                    .build();

            when(refreshTokenRepository.findByTokenHashAndRevokedFalse(anyString()))
                    .thenReturn(Mono.just(expired));

            StepVerifier.create(authService.refresh("expired-token"))
                    .expectErrorMatches(e -> e instanceof ResponseException re
                            && re.getHttpStatus() == HttpStatus.UNAUTHORIZED)
                    .verify();
        }

        @Test
        void userDeleted_returnsUnauthorized() {
            RefreshTokenEntity storedToken = RefreshTokenEntity.builder()
                    .userId("deleted-user")
                    .expiresAt(Instant.now().plusSeconds(3600))
                    .revoked(false)
                    .build();

            when(refreshTokenRepository.findByTokenHashAndRevokedFalse(anyString()))
                    .thenReturn(Mono.just(storedToken));
            when(userRepository.findById("deleted-user")).thenReturn(Mono.empty());

            StepVerifier.create(authService.refresh("orphan-token"))
                    .expectErrorMatches(e -> e instanceof ResponseException re
                            && re.getHttpStatus() == HttpStatus.UNAUTHORIZED)
                    .verify();
        }
    }

    @Nested
    class Logout {

        @Test
        void success() {
            RefreshTokenEntity storedToken = RefreshTokenEntity.builder()
                    .id("token-id")
                    .userId("user-123")
                    .revoked(false)
                    .build();

            when(refreshTokenRepository.findByTokenHashAndRevokedFalse(anyString()))
                    .thenReturn(Mono.just(storedToken));
            when(refreshTokenRepository.save(any(RefreshTokenEntity.class)))
                    .thenAnswer(inv -> Mono.just(inv.getArgument(0)));

            StepVerifier.create(authService.logout("some-refresh-token"))
                    .expectNext("Logged out successfully.")
                    .verifyComplete();

            assertTrue(storedToken.getRevoked());
        }

        @Test
        void unknownToken_returnsSuccessGracefully() {
            when(refreshTokenRepository.findByTokenHashAndRevokedFalse(anyString()))
                    .thenReturn(Mono.empty());

            StepVerifier.create(authService.logout("unknown-token"))
                    .expectNext("Logged out successfully.")
                    .verifyComplete();
        }
    }
}
