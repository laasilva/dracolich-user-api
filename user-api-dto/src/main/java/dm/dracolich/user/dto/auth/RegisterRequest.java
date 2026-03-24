package dm.dracolich.user.dto.auth;

public record RegisterRequest(String email,
                              String username,
                              String password,
                              String displayName){}
