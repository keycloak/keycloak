package org.keycloak.protocol.ssf.endpoint;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * HTTP Push delivery failure response.
 *
 * See https://www.rfc-editor.org/rfc/rfc8935.html#section-2.3
 */
public class SsfSetPushDeliveryFailureResponse {

    public static final String ERROR_INVALID_REQUEST = "invalid_request";

    public static final String ERROR_INVALID_KEY = "invalid_key";

    public static final String ERROR_INVALID_ISSUER = "invalid_issuer";

    public static final String ERROR_INVALID_AUDIENCE = "invalid_audience";

    public static final String ERROR_AUTHENTICATION_FAILED = "authentication_failed";

    public static final String ERROR_ACCESS_DENIED = "access_denied";

    /*
     * Non standard error
     */
    public static final String ERROR_INTERNAL_ERROR = "internal_error";

    @JsonProperty("err")
    private final String error;

    @JsonProperty("description")
    private final String description;

    public SsfSetPushDeliveryFailureResponse(String error, String description) {
        this.error = error;
        this.description = description;
    }

    public String getError() {
        return error;
    }

    public String getDescription() {
        return description;
    }

    public static WebApplicationException newFailureResponse(Response.Status status, String errorCode, String errorMessage) {
        Response response = Response.status(status)
                .type(MediaType.APPLICATION_JSON)
                .entity(new SsfSetPushDeliveryFailureResponse(errorCode, errorMessage))
                .build();
        return new WebApplicationException(response);
    }
}
