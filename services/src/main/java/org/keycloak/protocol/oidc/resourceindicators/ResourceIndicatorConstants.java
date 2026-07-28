package org.keycloak.protocol.oidc.resourceindicators;

public interface ResourceIndicatorConstants {
    String ERROR_NOT_MATCHING = "The requested resource is not matching the original request.";
    String ERROR_INVALID_RESOURCE = "The requested resource is invalid, missing, unknown, or malformed.";
    String ERROR_RESOURCE_INDICATORS_DISABLED = "The client is not authorized to use resource indicators.";
    String URN_CLIENT_PREFIX = "urn:client:";
    String CLIENT_RESOURCE_URL_ATTRIBUTE = "resource_url";
}
