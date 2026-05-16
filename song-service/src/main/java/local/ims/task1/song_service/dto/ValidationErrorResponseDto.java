package local.ims.task1.song_service.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.http.HttpStatus;

import java.util.Map;

public class ValidationErrorResponseDto extends ErrorResponseDto {
    private final Map<String,String> details;

    public ValidationErrorResponseDto(String errorMessage, HttpStatus errorCode, Map<String, String> details) {
        super(errorMessage, errorCode);
        this.details = details;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public Map<String, String> getDetails() {
        if ( details == null || details.isEmpty() ) {
            return null;
        }
        return details;
    }

}

