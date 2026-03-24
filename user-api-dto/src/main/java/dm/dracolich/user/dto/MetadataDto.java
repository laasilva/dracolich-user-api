package dm.dracolich.user.dto;

import java.time.Instant;

public record MetadataDto(Instant createdAt,
                          Instant updatedAt) {
}
