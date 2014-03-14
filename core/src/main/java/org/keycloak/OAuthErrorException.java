package org.keycloak;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class OAuthErrorException extends Exception {
    public static final String INVALID_REQUEST = "invalid_request";
    public static final String INVALID_CLIENT = "invalid_client";
    public static final String INVALID_GRANT = "invalid_grant";
    public static final String INVALID_SCOPE = "invalid_grant";
    public static final String UNAUTHORIZED_CLIENT = "unauthorized_client";
    public static final String UNSUPPORTED_GRANT_TYPE = "unsupported_grant_type";

    public OAuthErrorException(String error, String description, String message, Throwable cause) {
        super(message, cause);
        this.error = error;
        this.description = description;
    }
    public OAuthErrorException(String error, String description, String message) {
        super(message);
        this.error = error;
        this.description = description;
    }
    public OAuthErrorException(String error, String description) {
        super(description);
        this.error = error;
        this.description = description;
    }
    public OAuthErrorException(String error, String description, Throwable cause) {
        super(description, cause);
        this.error = error;
        this.description = description;
    }

    public OAuthErrorException(String error) {
        super(error);
        this.error = error;
    }
    public OAuthErrorException(String error, Throwable cause) {
        super(error, cause);
        this.error = error;
    }


    protected String error;
    protected String description;

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
