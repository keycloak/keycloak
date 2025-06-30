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
package org.keycloak.quarkus.runtime.services.resources;

public class ConstantsDebugHostname {
    public static final String[] RELEVANT_HEADERS = new String[] {
            "Host",
            "Forwarded",
            "X-Forwarded-Host",
            "X-Forwarded-Proto",
            "X-Forwarded-Port",
            "X-Forwarded-For"
    };

    public static final String[] RELEVANT_OPTIONS = {
            "hostname",
            "hostname-url",
            "hostname-admin",
            "hostname-admin-url",
            "hostname-strict",
            "hostname-strict-backchannel",
            "hostname-strict-https",
            "hostname-path",
            "hostname-port",
            "proxy",
            "proxy-headers",
            "http-enabled",
            "http-relative-path",
            "http-port",
            "https-port"
    };

    public static final String[] RELEVANT_OPTIONS_V2 = {
            "hostname",
            "hostname-admin",
            "hostname-backchannel-dynamic",
            "hostname-strict",
            "proxy-headers",
            "http-enabled",
            "http-relative-path",
            "http-port",
            "https-port"
    };

}
