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

import java.util.stream.Stream;

import org.keycloak.config.HostnameV2Options;
import org.keycloak.config.HttpOptions;
import org.keycloak.config.ProxyOptions;

public class ConstantsDebugHostname {
    public static final String[] X_FORWARDED_PROXY_HEADERS = new String[] {
            "X-Forwarded-Host",
            "X-Forwarded-Proto",
            "X-Forwarded-Port",
            "X-Forwarded-For"
    };

    public static final String FORWARDED_PROXY_HEADER = "Forwarded";

    public static final String[] RELEVANT_HEADERS = Stream
            .concat(Stream.of("Host", FORWARDED_PROXY_HEADER), Stream.of(X_FORWARDED_PROXY_HEADERS))
            .toArray(String[]::new);

    public static final String[] RELEVANT_OPTIONS_V2 = {
            HostnameV2Options.HOSTNAME.getKey(),
            HostnameV2Options.HOSTNAME_ADMIN.getKey(),
            HostnameV2Options.HOSTNAME_BACKCHANNEL_DYNAMIC.getKey(),
            HostnameV2Options.HOSTNAME_STRICT.getKey(),
            ProxyOptions.PROXY_HEADERS.getKey(),
            HttpOptions.HTTP_ENABLED.getKey(),
            HttpOptions.HTTP_RELATIVE_PATH.getKey(),
            HttpOptions.HTTP_PORT.getKey(),
            HttpOptions.HTTPS_PORT.getKey()
    };

}
