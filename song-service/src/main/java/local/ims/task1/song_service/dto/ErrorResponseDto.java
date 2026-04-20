package local.ims.task1.song_service.dto;

import lombok.Data;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Data
public class ErrorResponseDto {
    private final String errorMessage;
    private final HttpStatus errorCode;


    public ErrorResponseDto(String errorMessage, HttpStatus errorCode) {
        this.errorMessage = errorMessage;
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return String.valueOf(errorCode.value());
    }
}
