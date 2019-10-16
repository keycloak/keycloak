/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.models;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class BrowserSecurityHeaders {

    public static final String X_FRAME_OPTIONS = "X-Frame-Options";

    public static final String X_FRAME_OPTIONS_DEFAULT = "SAMEORIGIN";

    public static final String X_FRAME_OPTIONS_KEY = "xFrameOptions";

    public static final String CONTENT_SECURITY_POLICY = "Content-Security-Policy";

    public static final String CONTENT_SECURITY_POLICY_DEFAULT = "frame-src 'self'; frame-ancestors 'self'; object-src 'none';";

    public static final String CONTENT_SECURITY_POLICY_KEY = "contentSecurityPolicy";

    public static final String CONTENT_SECURITY_POLICY_REPORT_ONLY = "Content-Security-Policy-Report-Only";

    public static final String CONTENT_SECURITY_POLICY_REPORT_ONLY_DEFAULT = "";

    public static final String CONTENT_SECURITY_POLICY_REPORT_ONLY_KEY = "contentSecurityPolicyReportOnly";

    public static final String X_CONTENT_TYPE_OPTIONS = "X-Content-Type-Options";

    public static final String X_CONTENT_TYPE_OPTIONS_DEFAULT = "nosniff";

    public static final String X_CONTENT_TYPE_OPTIONS_KEY = "xContentTypeOptions";

    public static final String X_ROBOTS_TAG = "X-Robots-Tag";

    public static final String X_ROBOTS_TAG_KEY = "xRobotsTag";

    public static final String X_ROBOTS_TAG_DEFAULT = "none";

    public static final String X_XSS_PROTECTION = "X-XSS-Protection";

    public static final String X_XSS_PROTECTION_DEFAULT = "1; mode=block";

    public static final String X_XSS_PROTECTION_KEY = "xXSSProtection";

    public static final String STRICT_TRANSPORT_SECURITY = "Strict-Transport-Security";

    public static final String STRICT_TRANSPORT_SECURITY_DEFAULT = "max-age=31536000; includeSubDomains";

    public static final String STRICT_TRANSPORT_SECURITY_KEY = "strictTransportSecurity";

    public static final Map<String, String> headerAttributeMap;
    public static final Map<String, String> defaultHeaders;

    static {
        Map<String, String> headerMap = new HashMap<>();
        headerMap.put(X_FRAME_OPTIONS_KEY, X_FRAME_OPTIONS);
        headerMap.put(CONTENT_SECURITY_POLICY_KEY, CONTENT_SECURITY_POLICY);
        headerMap.put(CONTENT_SECURITY_POLICY_REPORT_ONLY_KEY, CONTENT_SECURITY_POLICY_REPORT_ONLY);
        headerMap.put(X_CONTENT_TYPE_OPTIONS_KEY, X_CONTENT_TYPE_OPTIONS);
        headerMap.put(X_ROBOTS_TAG_KEY, X_ROBOTS_TAG);
        headerMap.put(X_XSS_PROTECTION_KEY, X_XSS_PROTECTION);
        headerMap.put(STRICT_TRANSPORT_SECURITY_KEY, STRICT_TRANSPORT_SECURITY);

        Map<String, String> dh = new HashMap<>();
        dh.put(X_FRAME_OPTIONS_KEY, X_FRAME_OPTIONS_DEFAULT);
        dh.put(CONTENT_SECURITY_POLICY_KEY, CONTENT_SECURITY_POLICY_DEFAULT);
        dh.put(CONTENT_SECURITY_POLICY_REPORT_ONLY_KEY, CONTENT_SECURITY_POLICY_REPORT_ONLY_DEFAULT);
        dh.put(X_CONTENT_TYPE_OPTIONS_KEY, X_CONTENT_TYPE_OPTIONS_DEFAULT);
        dh.put(X_ROBOTS_TAG_KEY, X_ROBOTS_TAG_DEFAULT);
        dh.put(X_XSS_PROTECTION_KEY, X_XSS_PROTECTION_DEFAULT);
        dh.put(STRICT_TRANSPORT_SECURITY_KEY, STRICT_TRANSPORT_SECURITY_DEFAULT);

        defaultHeaders = Collections.unmodifiableMap(dh);
        headerAttributeMap = Collections.unmodifiableMap(headerMap);
    }
}
