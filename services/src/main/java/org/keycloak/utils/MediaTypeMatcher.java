package org.keycloak.utils;

import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;

import org.jboss.logging.Logger;

public class MediaTypeMatcher {

    private static final Logger logger = Logger.getLogger(MediaTypeMatcher.class);

    public static boolean isHtmlRequest(HttpHeaders headers) {
        return isAcceptMediaType(headers, MediaType.TEXT_HTML_TYPE);
    }

    public static boolean isJsonRequest(HttpHeaders headers) {
        return isAcceptMediaType(headers, MediaType.APPLICATION_JSON_TYPE);
    }

    private static boolean isAcceptMediaType(HttpHeaders headers, MediaType textHtmlType) {
        try {
            for (MediaType m : headers.getAcceptableMediaTypes()) {
                if (!m.isWildcardType() && m.isCompatible(textHtmlType)) {
                    return true;
                }
            }
        } catch (Exception e) {
            // illegal state, or illegal argument are possible
            logger.debug("Could not determine if the media type is accepted", e);
        }
        return false;
    }
}
