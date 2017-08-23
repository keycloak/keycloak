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
    public static final Map<String, String> headerAttributeMap;
    public static final Map<String, String> defaultHeaders;

    static {
        Map<String, String> headerMap = new HashMap<>();
        headerMap.put("xFrameOptions", "X-Frame-Options");
        headerMap.put("contentSecurityPolicy", "Content-Security-Policy");
        headerMap.put("xContentTypeOptions", "X-Content-Type-Options");
        headerMap.put("xRobotsTag", "X-Robots-Tag");
        headerMap.put("xXSSProtection", "X-XSS-Protection");

        Map<String, String> dh = new HashMap<>();
        dh.put("xFrameOptions", "SAMEORIGIN");
        dh.put("contentSecurityPolicy", "frame-src 'self'; frame-ancestors 'self'; object-src 'none';");
        dh.put("xContentTypeOptions", "nosniff");
        dh.put("xRobotsTag", "none");
        dh.put("xXSSProtection", "1; mode=block");

        defaultHeaders = Collections.unmodifiableMap(dh);
        headerAttributeMap = Collections.unmodifiableMap(headerMap);
    }
}