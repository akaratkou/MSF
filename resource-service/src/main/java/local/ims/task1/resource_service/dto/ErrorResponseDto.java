package local.ims.task1.resource_service.dto;

import org.springframework.http.HttpStatus;

public record ErrorResponseDto(
        String errorMessage,
        HttpStatus errorCode) {

    public String getErrorCode() {
        return String.valueOf(errorCode.value());
    }
}

