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

public enum BrowserSecurityHeaders {

    X_FRAME_OPTIONS("xFrameOptions", "X-Frame-Options", "SAMEORIGIN"),
    CONTENT_SECURITY_POLICY("contentSecurityPolicy", "Content-Security-Policy", ContentSecurityPolicyBuilder.create().build()),
    CONTENT_SECURITY_POLICY_REPORT_ONLY("contentSecurityPolicyReportOnly", "Content-Security-Policy-Report-Only", ""),
    X_CONTENT_TYPE_OPTIONS("xContentTypeOptions", "X-Content-Type-Options", "nosniff"),
    X_ROBOTS_TAG("xRobotsTag", "X-Robots-Tag", "none"),
    STRICT_TRANSPORT_SECURITY("strictTransportSecurity", "Strict-Transport-Security", "max-age=31536000; includeSubDomains"),
    REFERRER_POLICY("referrerPolicy", "Referrer-Policy", "no-referrer");

    private final String key;
    private final String headerName;
    private final String defaultValue;

    BrowserSecurityHeaders(String key, String headerName, String defaultValue) {
        this.key = key;
        this.headerName = headerName;
        this.defaultValue = defaultValue;
    }

    public String getKey() {
        return key;
    }

    public String getHeaderName() {
        return headerName;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    @Deprecated // should be removed eventually
    public static final Map<String, String> realmDefaultHeaders;

    static {

        Map<String, String> dh = new HashMap<>();
        dh.put(X_FRAME_OPTIONS.getKey(), X_FRAME_OPTIONS.getDefaultValue());
        dh.put(CONTENT_SECURITY_POLICY.getKey(), CONTENT_SECURITY_POLICY.getDefaultValue());
        dh.put(CONTENT_SECURITY_POLICY_REPORT_ONLY.getKey(), CONTENT_SECURITY_POLICY_REPORT_ONLY.getDefaultValue());
        dh.put(X_CONTENT_TYPE_OPTIONS.getKey(), X_CONTENT_TYPE_OPTIONS.getDefaultValue());
        dh.put(X_ROBOTS_TAG.getKey(), X_ROBOTS_TAG.getDefaultValue());
        dh.put(STRICT_TRANSPORT_SECURITY.getKey(), STRICT_TRANSPORT_SECURITY.getDefaultValue());
        dh.put(REFERRER_POLICY.getKey(), REFERRER_POLICY.getDefaultValue());

        realmDefaultHeaders = Collections.unmodifiableMap(dh);
    }
}
