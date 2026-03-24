package dm.dracolich.user.dto;

import lombok.Getter;

@Getter
public enum Pronouns {
    HE_HIM("He/Him"),
    SHE_HER("She/Her"),
    THEY_THEM("They/Them"),
    HE_THEY("He/They"),
    SHE_THEY("She/They"),
    ZE_ZIR("Ze/Zir"),
    ZE_HIR("Ze/Hir"),
    XE_XEM("Xe/Xem"),
    EY_EM("Ey/Em"),
    FAE_FAER("Fae/Faer"),
    ANY_PRONOUNS("Any Pronouns"),
    ASK_ME("Ask Me"),
    PREFER_NOT_TO_SAY("Prefer Not To Say"),
    OTHER("Other");

    private final String value;

    Pronouns(String value) {
        this.value = value;
    }
}