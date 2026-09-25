package org.keycloak.protocol.oidc.utils;

import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.keycloak.events.Errors;
import org.keycloak.services.ErrorResponseException;

public final class ContentTypeValidationUtil {

    // request content types are not validated for HTTP requests without payload
    // but we should validate it where a specification, e.g. RFC 6750, requires it
    public static void requireValidOrNoContentType(HttpHeaders headers) {
        getAndValidateContentType(headers);
    }

    // FIXME: remove this validation when we use Quarkus with https://github.com/quarkusio/quarkus/pull/55676
    public static void requireValidContentType(HttpHeaders headers, MediaType requiredMediaType) {
        MediaType requestMediaType = getAndValidateContentType(headers);
        if (requestMediaType != null && !requestMediaType.isCompatible(requiredMediaType)) {
            throw new ErrorResponseException(Errors.INVALID_REQUEST, "The content-type header value does not match consumed media type " + requiredMediaType, Response.Status.BAD_REQUEST);
        }
    }

    private static MediaType getAndValidateContentType(HttpHeaders headers) {
        String contentType = headers.getHeaderString(HttpHeaders.CONTENT_TYPE);
        if (contentType == null) {
            return null;
        }
        MediaType requestMediaType;
        try {
            requestMediaType = MediaType.valueOf(contentType);
        } catch (IllegalArgumentException e) {
            throw new ErrorResponseException(Errors.INVALID_REQUEST, "The content-type header value did not correspond to a valid media type", Response.Status.BAD_REQUEST);
        }
        return requestMediaType;
    }


}
