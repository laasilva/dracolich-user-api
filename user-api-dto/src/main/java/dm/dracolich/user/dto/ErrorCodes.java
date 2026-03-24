package dm.dracolich.user.dto;

import dm.dracolich.forge.error.ErrorCode;
import lombok.Getter;

@Getter
public enum ErrorCodes implements ErrorCode {
    DMD012("DMD012", "Email address already registered."),
    DMD013("DMD013", "Username already taken."),
    DMD014("DMD014", "Invalid or expired confirmation token."),
    DMD015("DMD015", "Account is not active. Current status: %s"),
    DMD016("DMD016", "Invalid username or password."),
    DMD017("DMD017", "Invalid or expired refresh token."),
    DMD018("DMD018", "Password does not meet minimum requirements."),
    DMD019("DMD019", "Email address is not valid."),
    DMD020("DMD020", "Failed to send confirmation email.");

    private final String code;
    private final String message;

    ErrorCodes(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public String format(String... args) {
        return String.format(message, args);
    }
}
