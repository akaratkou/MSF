package local.ims.task1.resource_service.exceptions;

public class BaseRequestException extends RuntimeException {

    private final String details;

    public BaseRequestException(String details) {
        this.details = details;
    }

    public String getDetails() {
        return details;
    }
}