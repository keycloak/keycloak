package org.keycloak.utils;

import javax.ws.rs.core.HttpHeaders;

public class MediaTypeMatcher {

    public static boolean isHtmlRequest(HttpHeaders headers) {
        for (javax.ws.rs.core.MediaType m : headers.getAcceptableMediaTypes()) {
            if (!m.isWildcardType() && m.isCompatible(javax.ws.rs.core.MediaType.TEXT_HTML_TYPE)) {
                return true;
            }
        }
        return false;
    }

}
