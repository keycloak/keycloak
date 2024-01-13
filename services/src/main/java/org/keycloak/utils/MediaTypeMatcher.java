package org.keycloak.utils;

import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;

public class MediaTypeMatcher {

    public static boolean isHtmlRequest(HttpHeaders headers) {
        return isAcceptMediaType(headers, MediaType.TEXT_HTML_TYPE);
    }

    public static boolean isJsonRequest(HttpHeaders headers) {
        return isAcceptMediaType(headers, MediaType.APPLICATION_JSON_TYPE);
    }

    private static boolean isAcceptMediaType(HttpHeaders headers, MediaType textHtmlType) {
        for (MediaType m : headers.getAcceptableMediaTypes()) {
            if (!m.isWildcardType() && m.isCompatible(textHtmlType)) {
                return true;
            }
        }
        return false;
    }
}
