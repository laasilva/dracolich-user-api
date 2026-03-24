package dm.dracolich.user.web.repository;

import dm.dracolich.user.web.entity.RefreshTokenEntity;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface RefreshTokenRepository extends ReactiveMongoRepository<RefreshTokenEntity, String> {
    Mono<RefreshTokenEntity> findByTokenHashAndRevokedFalse(String tokenHash);
    Flux<RefreshTokenEntity> findAllByUserId(String userId);
    Mono<Void> deleteAllByUserId(String userId);
}
