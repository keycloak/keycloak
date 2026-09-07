package org.keycloak.protocol.oidc.resourceindicators;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.regex.Pattern;

public class ResourceIndicatorValidation {

    // RFC 8141: NID = alphanum 0*30(ldh) alphanum, NSS = pchar *(pchar / "/"),
    // with pchar as defined by RFC 3986: unreserved / pct-encoded / sub-delims / ":" / "@".
    private static final String PCHAR = "[a-z0-9\\-._~!$&'()*+,;=:@]|%[0-9a-f]{2}";

    private static final Pattern URN_REGEX = Pattern.compile("^urn:[a-z0-9][a-z0-9-]{0,30}[a-z0-9]:(" + PCHAR + ")((" + PCHAR + ")|/)*+$", Pattern.CASE_INSENSITIVE);

    private ResourceIndicatorValidation() {
    }

    public static boolean isValidResourceIndicator(String resourceIndicator) {
        if (resourceIndicator == null) {
            return true;
        }

        try {
            URI uri = new URI(resourceIndicator);
            if ("urn".equalsIgnoreCase(uri.getScheme())) {
                return URN_REGEX.matcher(resourceIndicator).matches();
            } else {
                if (!uri.isAbsolute()) {
                    return false;
                } else if (uri.getFragment() != null) {
                    return false;
                } else if (uri.getQuery() != null) {
                    return false;
                } else if (uri.getPath() == null) {
                    return false;
                }
            }
            return true;
        } catch (URISyntaxException e) {
            return false;
        }
    }

}
