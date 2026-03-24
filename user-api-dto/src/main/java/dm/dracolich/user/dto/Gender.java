package dm.dracolich.user.dto;

import lombok.Getter;

@Getter
public enum Gender {
    MALE("Male"),
    FEMALE("Female"),
    NON_BINARY("Non Binary"),
    GENDERQUEER("Genderqueer"),
    GENDERFLUID("Genderfluid"),
    AGENDER("Agender"),
    BIGENDER("Bigender"),
    TWO_SPIRIT("Two Spirit"),
    DEMIGENDER("Demigender"),
    PANGENDER("Pangender"),
    INTERSEX("Intersex"),
    PREFER_NOT_TO_SAY("Prefer Not To Say"),
    OTHER("Other");

    private final String value;

    Gender(String value) {
        this.value = value;
    }
}