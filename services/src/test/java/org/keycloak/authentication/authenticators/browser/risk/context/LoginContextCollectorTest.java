/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.authentication.authenticators.browser.risk.context;

import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

public class LoginContextCollectorTest {

    @Test
    public void geoSignalPrefersConfiguredHeaders() {
        MultivaluedMap<String, String> headers = new MultivaluedHashMap<>();
        headers.add(LoginContextCollector.HEADER_X_FORWARDED_COUNTRY, "ca");
        headers.add(LoginContextCollector.HEADER_CLOUDFRONT_VIEWER_COUNTRY, "gb");
        headers.add(LoginContextCollector.HEADER_CF_IP_COUNTRY, "us");

        assertThat(LoginContextCollector.geoSignalFromHeaders(headers), equalTo("US"));
    }

    @Test
    public void headerLookupIsCaseInsensitive() {
        MultivaluedMap<String, String> headers = new MultivaluedHashMap<>();
        headers.add("user-agent", "browser");

        assertThat(LoginContextCollector.firstHeader(headers, "User-Agent"), equalTo("browser"));
    }

    @Test
    public void missingHeadersDoNotCrash() {
        assertThat(LoginContextCollector.geoSignalFromHeaders(new MultivaluedHashMap<>()), nullValue());
        assertThat(LoginContextCollector.firstHeader(null, "User-Agent"), nullValue());
    }
}
