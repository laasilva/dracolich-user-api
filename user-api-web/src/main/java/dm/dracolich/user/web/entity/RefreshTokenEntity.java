package dm.dracolich.user.web.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "refresh_tokens")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RefreshTokenEntity {
    @Id
    private String id;
    private String userId;
    private String tokenHash;
    private Instant issuedAt;
    private Instant expiresAt;
    private Boolean revoked;
}
