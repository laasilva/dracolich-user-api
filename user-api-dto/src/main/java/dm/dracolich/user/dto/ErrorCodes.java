package dm.dracolich.user.dto;

import dm.dracolich.forge.error.ErrorCode;
import lombok.Getter;

@Getter
public enum ErrorCodes implements ErrorCode {
    DMD012("DMD012", "This error is just for the user");

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
