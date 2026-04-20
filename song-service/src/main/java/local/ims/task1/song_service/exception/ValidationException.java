package local.ims.task1.song_service.exception;

public class ValidationException extends RuntimeException {

    private final String details;

    public ValidationException(String details) {
        this.details = details;
    }

    public String getDetails() {
        return details;
    }
}

