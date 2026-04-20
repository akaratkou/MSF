package local.ims.task1.song_service.exception;

public class InputDataValidationException extends ValidationException {
    public InputDataValidationException(String details) {
        super(details);
    }
}