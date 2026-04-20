package local.ims.task1.resource_service.exceptions;


public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException( String message) {
        super(message);
    }
}