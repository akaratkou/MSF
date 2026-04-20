package local.ims.task1.resource_service.handlers;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import jakarta.validation.constraints.Min;
import local.ims.task1.resource_service.dto.ErrorResponseDto;
import local.ims.task1.resource_service.exceptions.BaseRequestException;
import local.ims.task1.resource_service.exceptions.InputDataBaseRequestException;
import local.ims.task1.resource_service.exceptions.ResourceNotFoundException;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ResponseEntity<ErrorResponseDto> handleResourceNotFoundException(ResourceNotFoundException ex) {
        ErrorResponseDto errorResponseDto = new ErrorResponseDto(ex.getMessage(), HttpStatus.NOT_FOUND);
        return new ResponseEntity<>(errorResponseDto, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(BaseRequestException.class)
    public ResponseEntity<ErrorResponseDto> handleValidationNotFound(BaseRequestException ex) {
        ErrorResponseDto errorResponseDto = new ErrorResponseDto(
                ex.getDetails(),
                HttpStatus.INTERNAL_SERVER_ERROR
        );
        return new ResponseEntity<>(errorResponseDto, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(InputDataBaseRequestException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ErrorResponseDto> handleValidationBadInputData(InputDataBaseRequestException ex) {
        return ResponseEntity.badRequest()
                .body(new ErrorResponseDto(ex.getDetails(), HttpStatus.BAD_REQUEST));
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ErrorResponseDto> handleHttpMediaTypeNotSupported(HttpMediaTypeNotSupportedException ex) {
        String message = "Content-Type is not supported";
        if (ex.getContentType() != null) {
            message = "Invalid file format: " + ex.getContentType() + ". Only MP3 files are allowed";
        }
        ErrorResponseDto errorResponse = new ErrorResponseDto(message, HttpStatus.BAD_REQUEST);
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, String>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        Map<String, String> response = new HashMap<>();

        String invalidValue = (ex.getValue() != null) ? ex.getValue().toString() : "null";
        String paramName = ex.getName();
        MethodParameter param = ex.getParameter();

        String message;

        if (param != null && param.hasParameterAnnotation(Min.class)) {
            Min minAnnotation = param.getParameterAnnotation(Min.class);
            message = String.format("Invalid value '%s' for %s.", invalidValue, paramName.toUpperCase());
            String customMessage = minAnnotation.message();

            if (customMessage != null && !customMessage.startsWith("{")) {
                message = message + " " + customMessage;
            }
        } else if (ex.getRequiredType() != null && Number.class.isAssignableFrom(ex.getRequiredType()) ||
                (ex.getRequiredType() == int.class || ex.getRequiredType() == long.class)) {
            message = String.format("Invalid value '%s' for parameter '%s'. Must be a valid number", invalidValue, paramName);
        } else {
            message = String.format("Invalid format for parameter '%s'", paramName);
        }

        response.put("errorMessage", message);
        response.put("errorCode", "400");
        return ResponseEntity.badRequest().body(response);
    }


    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, String>> handleConstraintViolation(ConstraintViolationException ex) {
        Map<String, String> response = new HashMap<>();

        ConstraintViolation<?> violation = ex.getConstraintViolations().iterator().next();

        String invalidValue = (violation.getInvalidValue() != null) ? violation.getInvalidValue().toString() : "null";

        String annotationMessage = violation.getMessage();

        String paramName = "";
        for (Path.Node node : violation.getPropertyPath()) {
            paramName = node.getName();
        }

        String errorMessage = String.format("Invalid value '%s' for %s. %s",
                invalidValue,
                paramName.toUpperCase(),
                annotationMessage);

        response.put("errorMessage", errorMessage);
        response.put("errorCode", String.valueOf(HttpStatus.BAD_REQUEST.value()));

        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDto> handleOther(Exception ex) {
        ErrorResponseDto errorResponse = new ErrorResponseDto("Internal server error", HttpStatus.INTERNAL_SERVER_ERROR);
        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }

}