package com.hrms.email;

import org.springframework.http.HttpStatusCode;

public class BrevoEmailException extends RuntimeException {

    public enum Category {
        AUTHENTICATION,
        VALIDATION,
        API,
        NETWORK
    }

    private final Category category;
    private final HttpStatusCode statusCode;

    private BrevoEmailException(Category category, HttpStatusCode statusCode, String message, Throwable cause) {
        super(message, cause);
        this.category = category;
        this.statusCode = statusCode;
    }

    public static BrevoEmailException http(Category category, HttpStatusCode statusCode, Throwable cause) {
        return new BrevoEmailException(category, statusCode,
                "Brevo email submission failed with HTTP status " + statusCode.value(), cause);
    }

    public static BrevoEmailException network(Throwable cause) {
        return new BrevoEmailException(Category.NETWORK, null,
                "Brevo email submission failed because the service could not be reached", cause);
    }

    public static BrevoEmailException api(String message, Throwable cause) {
        return new BrevoEmailException(Category.API, null, message, cause);
    }

    public Category getCategory() {
        return category;
    }

    public HttpStatusCode getStatusCode() {
        return statusCode;
    }
}
