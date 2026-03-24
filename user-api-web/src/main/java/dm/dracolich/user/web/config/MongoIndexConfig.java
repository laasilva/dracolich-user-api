package dm.dracolich.user.web.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.index.Index;

import java.time.Duration;

@Configuration
@RequiredArgsConstructor
public class MongoIndexConfig {

    private final ReactiveMongoTemplate mongoTemplate;

    @PostConstruct
    public void initIndexes() {
        mongoTemplate.indexOps("users")
                .createIndex(new Index().on("email", Direction.ASC).unique())
                .then(mongoTemplate.indexOps("users")
                        .createIndex(new Index().on("username", Direction.ASC).unique()))
                .then(mongoTemplate.indexOps("refresh_tokens")
                        .createIndex(new Index().on("tokenHash", Direction.ASC).unique()))
                .then(mongoTemplate.indexOps("refresh_tokens")
                        .createIndex(new Index().on("userId", Direction.ASC)))
                .then(mongoTemplate.indexOps("refresh_tokens")
                        .createIndex(new Index().on("expiresAt", Direction.ASC).expire(Duration.ZERO)))
                .subscribe();
    }
}