package dm.dracolich.user.web.service;

import dm.dracolich.forge.error.ApiError;
import dm.dracolich.forge.exception.ResponseException;
import dm.dracolich.user.dto.AccessLevelEnum;
import dm.dracolich.user.dto.AccountStatusEnum;
import dm.dracolich.user.dto.ErrorCodes;
import dm.dracolich.user.dto.auth.AuthResponse;
import dm.dracolich.user.dto.auth.LoginRequest;
import dm.dracolich.user.dto.auth.RegisterRequest;
import dm.dracolich.user.web.entity.MetadataEntity;
import dm.dracolich.user.web.entity.RefreshTokenEntity;
import dm.dracolich.user.web.entity.UserEntity;
import dm.dracolich.user.web.entity.UserProfileEntity;
import dm.dracolich.user.web.repository.RefreshTokenRepository;
import dm.dracolich.user.web.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.validator.routines.EmailValidator;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Random;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    @Override
    public Mono<String> register(RegisterRequest request) {
        return validateRegistration(request)
                .then(Mono.defer(() -> userRepository.existsByEmail(request.email())))
                .flatMap(exists -> {
                    if (exists) {
                        return Mono.error(buildException(ErrorCodes.DMD012, HttpStatus.CONFLICT));
                    }
                    return userRepository.existsByUsername(request.username());
                })
                .flatMap(exists -> {
                    if (exists) {
                        return Mono.error(buildException(ErrorCodes.DMD013, HttpStatus.CONFLICT));
                    }
                    return Mono.fromCallable(() -> passwordEncoder.encode(request.password()))
                            .subscribeOn(Schedulers.boundedElastic());
                })
                .flatMap(hashedPassword -> {
                    Instant now = Instant.now();
                    UserEntity user = UserEntity.builder()
                            .email(request.email())
                            .username(request.username())
                            .userHash(new Random().nextInt(1000, 9999))
                            .password(hashedPassword)
                            .accessLevel(AccessLevelEnum.COMMON_USER)
                            .accountStatus(AccountStatusEnum.PENDING_CONFIRMATION)
                            .profile(UserProfileEntity.builder()
                                    .displayName(request.displayName())
                                    .build())
                            .metadata(MetadataEntity.builder()
                                    .createdAt(now)
                                    .updatedAt(now)
                                    .build())
                            .build();
                    return userRepository.save(user);
                })
                .flatMap(savedUser -> {
                    String confirmationToken = jwtService.generateConfirmationToken(savedUser.getId());
                    return emailService.sendConfirmationEmail(savedUser.getEmail(), confirmationToken)
                            .thenReturn("Registration successful. Please check your email to confirm your account.")
                            .onErrorResume(e -> {
                                log.error("Failed to send confirmation email to {}", savedUser.getEmail(), e);
                                return Mono.just("Registration successful. Confirmation email could not be sent, please contact support.");
                            });
                });
    }

    @Override
    public Mono<String> confirmAccount(String token) {
        return jwtService.validateConfirmationToken(token)
                .switchIfEmpty(Mono.error(buildException(ErrorCodes.DMD014, HttpStatus.BAD_REQUEST)))
                .flatMap(userRepository::findById)
                .switchIfEmpty(Mono.error(buildException(ErrorCodes.DMD014, HttpStatus.BAD_REQUEST)))
                .flatMap(user -> {
                    if (user.getAccountStatus() != AccountStatusEnum.PENDING_CONFIRMATION) {
                        return Mono.error(buildException(ErrorCodes.DMD014, HttpStatus.BAD_REQUEST));
                    }
                    user.setAccountStatus(AccountStatusEnum.ACTIVE);
                    user.getMetadata().setUpdatedAt(Instant.now());
                    return userRepository.save(user);
                })
                .thenReturn("Account confirmed successfully.");
    }

    @Override
    public Mono<AuthResponse> login(LoginRequest request) {
        return userRepository.findByUsername(request.username())
                .switchIfEmpty(Mono.error(buildException(ErrorCodes.DMD016, HttpStatus.UNAUTHORIZED)))
                .flatMap(user -> Mono.fromCallable(() -> passwordEncoder.matches(request.password(), user.getPassword()))
                        .subscribeOn(Schedulers.boundedElastic())
                        .flatMap(matches -> {
                            if (!matches) {
                                return Mono.error(buildException(ErrorCodes.DMD016, HttpStatus.UNAUTHORIZED));
                            }
                            if (user.getAccountStatus() != AccountStatusEnum.ACTIVE) {
                                return Mono.error(new ResponseException(
                                        ErrorCodes.DMD015.format(user.getAccountStatus().name()),
                                        List.of(new ApiError(ErrorCodes.DMD015)),
                                        HttpStatus.FORBIDDEN));
                            }
                            return issueTokens(user);
                        }));
    }

    @Override
    public Mono<AuthResponse> refresh(String refreshToken) {
        String tokenHash = hashToken(refreshToken);
        return refreshTokenRepository.findByTokenHashAndRevokedFalse(tokenHash)
                .switchIfEmpty(Mono.error(buildException(ErrorCodes.DMD017, HttpStatus.UNAUTHORIZED)))
                .flatMap(storedToken -> {
                    if (storedToken.getExpiresAt().isBefore(Instant.now())) {
                        return Mono.error(buildException(ErrorCodes.DMD017, HttpStatus.UNAUTHORIZED));
                    }
                    storedToken.setRevoked(true);
                    return refreshTokenRepository.save(storedToken)
                            .then(userRepository.findById(storedToken.getUserId()));
                })
                .switchIfEmpty(Mono.error(buildException(ErrorCodes.DMD017, HttpStatus.UNAUTHORIZED)))
                .flatMap(this::issueTokens);
    }

    @Override
    public Mono<String> logout(String refreshToken) {
        String tokenHash = hashToken(refreshToken);
        return refreshTokenRepository.findByTokenHashAndRevokedFalse(tokenHash)
                .flatMap(token -> {
                    token.setRevoked(true);
                    return refreshTokenRepository.save(token);
                })
                .thenReturn("Logged out successfully.");
    }

    private Mono<AuthResponse> issueTokens(UserEntity user) {
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken();
        String tokenHash = hashToken(refreshToken);

        Instant now = Instant.now();
        RefreshTokenEntity refreshTokenEntity = RefreshTokenEntity.builder()
                .userId(user.getId())
                .tokenHash(tokenHash)
                .issuedAt(now)
                .expiresAt(now.plusSeconds(604800)) // 7 days
                .revoked(false)
                .build();

        return refreshTokenRepository.save(refreshTokenEntity)
                .thenReturn(new AuthResponse(accessToken, refreshToken, 900L));
    }

    private Mono<Void> validateRegistration(RegisterRequest request) {
        if (request.email() == null || !EmailValidator.getInstance().isValid(request.email())) {
            return Mono.error(buildException(ErrorCodes.DMD019, HttpStatus.BAD_REQUEST));
        }
        if (request.password() == null || request.password().length() < 8) {
            return Mono.error(buildException(ErrorCodes.DMD018, HttpStatus.BAD_REQUEST));
        }
        if (request.username() == null || request.username().isBlank()) {
            return Mono.error(buildException(ErrorCodes.DMD013, HttpStatus.BAD_REQUEST));
        }
        return Mono.empty();
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    private ResponseException buildException(ErrorCodes errorCode, HttpStatus status) {
        return new ResponseException(
                errorCode.getMessage(),
                List.of(new ApiError(errorCode)),
                status);
    }
}
