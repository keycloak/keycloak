package org.keycloak.services.error;

import jakarta.ws.rs.core.Response;

/**
 * The catalogue of problem types the Admin API v2 can return, see the Admin API v2 guide.
 * <p>
 * The {@code type} member of a problem is {@link #getType()}. Clients branch on it, so an existing constant must never
 * change its meaning or status. The status stays the authority for HTTP semantics, and the type narrows it down.
 */
public enum ProblemType {

    /**
     * Any input that is not acceptable: bean validation, unknown fields, an invalid query parameter, and so on.
     * The members of the request at fault are listed in the {@code violations} of the problem when they are known.
     */
    VALIDATION_FAILED("validation-failed", Response.Status.BAD_REQUEST, "Validation failed"),
    MALFORMED_BODY("malformed-body", Response.Status.BAD_REQUEST, "Malformed request body"),
    INVALID_PATCH("invalid-patch", Response.Status.BAD_REQUEST, "Invalid patch"),
    POLICY_VIOLATION("policy-violation", Response.Status.BAD_REQUEST, "Rejected by client policy"),

    UNAUTHENTICATED("unauthenticated", Response.Status.UNAUTHORIZED, "Authentication required"),
    FORBIDDEN("forbidden", Response.Status.FORBIDDEN, "Forbidden"),

    /**
     * A realm, a client or any other resource that does not exist. The {@code detail} says which.
     */
    NOT_FOUND("not-found", Response.Status.NOT_FOUND, "Not found"),
    API_UNAVAILABLE("api-unavailable", Response.Status.NOT_FOUND, "Admin API not available"),

    METHOD_NOT_ALLOWED("method-not-allowed", Response.Status.METHOD_NOT_ALLOWED, "Method not allowed"),

    /**
     * The request conflicts with the state of a resource: an identifying value is already in use, or the resource
     * cannot be deleted. The {@code detail} says which.
     */
    CONFLICT("conflict", Response.Status.CONFLICT, "Conflict"),

    UNSUPPORTED_MEDIA_TYPE("unsupported-media-type", Response.Status.UNSUPPORTED_MEDIA_TYPE, "Unsupported media type"),

    INTERNAL_ERROR("internal-error", Response.Status.INTERNAL_SERVER_ERROR, "Internal server error");

    public static final String TYPE_PREFIX = "urn:keycloak:admin-api:v2:problem:";

    private final String suffix;
    private final Response.Status status;
    private final String title;

    ProblemType(String suffix, Response.Status status, String title) {
        this.suffix = suffix;
        this.status = status;
        this.title = title;
    }

    /**
     * @return the value of the {@code type} member of the problem
     */
    public String getType() {
        return TYPE_PREFIX + suffix;
    }

    public Response.Status getStatus() {
        return status;
    }

    public String getTitle() {
        return title;
    }

    /**
     * The type for a status without an entry in the catalogue, for example 406 or 429.
     */
    public static String fallbackType(int status) {
        return TYPE_PREFIX + "http-" + status;
    }

}
