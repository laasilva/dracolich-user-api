package dm.dracolich.user.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserProfileDto {
    private String id;
    private String name;
    private String displayName;
    private LocalDate birthDay;
    private Gender gender;
    private Pronouns pronouns;
    private String bio;
    private String profilePicture;
    private String headerPicture;
}
