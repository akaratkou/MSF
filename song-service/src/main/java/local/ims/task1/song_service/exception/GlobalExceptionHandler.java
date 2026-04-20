package local.ims.task1.song_service.exception;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import jakarta.validation.constraints.Min;
import local.ims.task1.song_service.dto.ErrorResponseDto;
import local.ims.task1.song_service.dto.ValidationErrorResponseDto;
import org.springframework.core.MethodParameter;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponseDto> handleResourceNotFoundException(ResourceNotFoundException ex) {
        ErrorResponseDto errorResponseDto = new ErrorResponseDto(ex.getMessage(), HttpStatus.NOT_FOUND);
        return new ResponseEntity<>(errorResponseDto, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ValidationErrorResponseDto> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> details = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            details.put(fieldName, errorMessage);
        });
        ValidationErrorResponseDto errorResponse = new ValidationErrorResponseDto("Validation error", HttpStatus.BAD_REQUEST, details);
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(InputDataValidationException.class)
    public ResponseEntity<ValidationErrorResponseDto> handleValidationException(ValidationException ex) {
        ValidationErrorResponseDto errorResponse = new ValidationErrorResponseDto(ex.getDetails(), HttpStatus.BAD_REQUEST, null);
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ResponseEntity<ErrorResponseDto> handleDataIntegrityViolationException(DataIntegrityViolationException ex) {
        ErrorResponseDto errorResponseDto = new ErrorResponseDto(ex.getMessage(), HttpStatus.CONFLICT);
        return new ResponseEntity<>(errorResponseDto, HttpStatus.CONFLICT);
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
            message = String.format("Invalid value '%s' for %s. Must be a positive integer", invalidValue, paramName.toUpperCase());
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
        response.put("errorCode", "400");

        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResponseEntity<ErrorResponseDto> handleAllExceptions(Exception ex) {
        ErrorResponseDto errorResponseDto = new ErrorResponseDto("An error occurred on the server", HttpStatus.INTERNAL_SERVER_ERROR);
        return new ResponseEntity<>(errorResponseDto, HttpStatus.INTERNAL_SERVER_ERROR);
    }

}


