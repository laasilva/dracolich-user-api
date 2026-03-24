package dm.dracolich.user.web.entity;

import dm.dracolich.user.dto.Gender;
import dm.dracolich.user.dto.Pronouns;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserProfileEntity {
    private String name;
    private String displayName;
    private LocalDate birthDay;
    private Gender gender;
    private Pronouns pronouns;
    private String bio;
    private String profilePicture;
    private String headerPicture;
}
