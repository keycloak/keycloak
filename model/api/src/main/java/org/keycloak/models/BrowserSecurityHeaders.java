package org.keycloak.models;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class BrowserSecurityHeaders {
    public static final Map<String, String> headerAttributeMap;
    public static final Map<String, String> defaultHeaders;

    static {
        Map<String, String> headerMap = new HashMap<String, String>();
        headerMap.put("xFrameOptions", "X-Frame-Options");
        headerMap.put("contentSecurityPolicy", "Content-Security-Policy");

        Map<String, String> dh = new HashMap<String, String>();
        dh.put("xFrameOptions", "SAMEORIGIN");
        dh.put("contentSecurityPolicy", "frame-src 'self'");

        defaultHeaders = Collections.unmodifiableMap(dh);
        headerAttributeMap = Collections.unmodifiableMap(headerMap);
    }
}
