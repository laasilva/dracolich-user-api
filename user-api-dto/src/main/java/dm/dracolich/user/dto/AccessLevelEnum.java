package dm.dracolich.user.dto;

public enum AccessLevelEnum {
    ADMIN, // unlimited POWER!
    COMMON_USER, // access to public stuff
    DEV_USER // access to use apis but NOT alter them [GET, POST, AND AUTHORIZED DELETES (user and custom character stuff)]
}
