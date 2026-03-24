package dm.dracolich.user.web.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MetadataEntity {
    private Instant createdAt;
    private Instant updatedAt;
}
