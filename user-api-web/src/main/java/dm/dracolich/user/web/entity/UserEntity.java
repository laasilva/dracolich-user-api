package dm.dracolich.user.web.entity;

import dm.dracolich.user.dto.AccessLevelEnum;
import dm.dracolich.user.dto.AccountStatusEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "users")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserEntity {
    @Id
    private String id;
    private String email;
    private String username;
    private Integer userHash;
    private String password;
    private AccessLevelEnum accessLevel;
    private AccountStatusEnum accountStatus;
    private UserProfileEntity profile;
    private MetadataEntity metadata;
}
