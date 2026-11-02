package ecommerce.example.Death_Mart.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when a requested resource (product, user, order, etc.) is not found.
 * Automatically returns HTTP 404 due to @ResponseStatus.
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    /**
     * Convenience constructor for standard "Entity not found" messages.
     * Example: new ResourceNotFoundException("Product", "id", 42)
     *   → "Product not found with id: '42'"
     */
    public ResourceNotFoundException(String resourceName, String fieldName, Object fieldValue) {
        super(String.format("%s not found with %s: '%s'", resourceName, fieldName, fieldValue));
    }
}
