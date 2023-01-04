package org.keycloak.utils;

import jakarta.ws.rs.core.HttpHeaders;

public class MediaTypeMatcher {

    public static boolean isHtmlRequest(HttpHeaders headers) {
        for (jakarta.ws.rs.core.MediaType m : headers.getAcceptableMediaTypes()) {
            if (!m.isWildcardType() && m.isCompatible(jakarta.ws.rs.core.MediaType.TEXT_HTML_TYPE)) {
                return true;
            }
        }
        return false;
    }

}
