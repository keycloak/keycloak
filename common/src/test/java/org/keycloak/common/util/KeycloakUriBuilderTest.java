/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates
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
 *
 */

package org.keycloak.common.util;

import java.util.Collections;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author rmartinc
 */
public class KeycloakUriBuilderTest {

    @Test
    public void test() {
        Assertions.assertEquals("http://localhost:8080/path?attr1=value1&attr2=value2",
                KeycloakUriBuilder.fromUri("http://localhost:8080/path?attr1=value1&attr2=value2")
                        .build().toString());

        Assertions.assertEquals("http://localhost/path?attr1=value1&attr2=value2",
                KeycloakUriBuilder.fromUri("http://localhost:80")
                        .path("path")
                        .queryParam("attr1", "value1")
                        .queryParam("attr2", "value2")
                        .build().toString());

        Assertions.assertEquals("unknown://localhost:9000/path",
                KeycloakUriBuilder.fromUri("unknown://localhost:9000/path").build().toString());

        Assertions.assertEquals("https://localhost/path?attr1=value1",
                KeycloakUriBuilder.fromUri("https://{hostname}:443/path?attr1={value}")
                        .build("localhost", "value1").toString());

        Assertions.assertEquals("https://localhost:8443/path?attr1=value1",
                KeycloakUriBuilder.fromUri("https://localhost:8443/path?attr1={value}")
                        .buildFromMap(Collections.singletonMap("value", "value1")).toString());
    }

    @Test
    public void testPort() {
        Assertions.assertEquals("https://localhost:8443/path", KeycloakUriBuilder.fromUri("https://localhost:8443/path").buildAsString());
        Assertions.assertEquals("https://localhost:8443/path", KeycloakUriBuilder.fromUri("https://localhost:8443/path").preserveDefaultPort().buildAsString());

        Assertions.assertEquals("https://localhost/path", KeycloakUriBuilder.fromUri("https://localhost:443/path").buildAsString());
        Assertions.assertEquals("https://localhost:443/path", KeycloakUriBuilder.fromUri("https://localhost:443/path").preserveDefaultPort().buildAsString());

        Assertions.assertEquals("http://localhost/path", KeycloakUriBuilder.fromUri("http://localhost:80/path").buildAsString());
        Assertions.assertEquals("http://localhost:80/path", KeycloakUriBuilder.fromUri("http://localhost:80/path").preserveDefaultPort().buildAsString());

        // Port always preserved (even if preserverPort not specified) due the port 80 doesn't match "https" scheme
        Assertions.assertEquals("https://localhost:80/path", KeycloakUriBuilder.fromUri("https://localhost:80/path").buildAsString());

        // Port not in the build URL when it was not specified in the original URL (even if preserverPort() is true)
        Assertions.assertEquals("http://localhost/path", KeycloakUriBuilder.fromUri("http://localhost/path").buildAsString());
        Assertions.assertEquals("http://localhost/path", KeycloakUriBuilder.fromUri("http://localhost/path").preserveDefaultPort().buildAsString());
        Assertions.assertEquals("https://localhost/path", KeycloakUriBuilder.fromUri("https://localhost/path").buildAsString());
        Assertions.assertEquals("https://localhost/path", KeycloakUriBuilder.fromUri("https://localhost/path").preserveDefaultPort().buildAsString());
    }

    @Test
    public void testTemplateAndNotTemplate() {
        Assertions.assertEquals("https://localhost:8443/path?key=query#fragment", KeycloakUriBuilder.fromUri(
                "https://localhost:8443/{path}?key={query}#{fragment}").buildAsString("path", "query", "fragment"));
        Assertions.assertEquals("https://localhost:8443/%7Bpath%7D?key=%7Bquery%7D#%7Bfragment%7D", KeycloakUriBuilder.fromUri(
                "https://localhost:8443/{path}?key={query}#{fragment}", false).buildAsString());
    }

    @Test
    public void testUserInfo() {
        Assertions.assertEquals("https://user-info@localhost:8443/path?key=query#fragment", KeycloakUriBuilder.fromUri(
                "https://{userinfo}@localhost:8443/{path}?key={query}#{fragment}").buildAsString("user-info", "path", "query", "fragment"));
        Assertions.assertEquals("https://user%20info%40%2F@localhost:8443/path?key=query#fragment", KeycloakUriBuilder.fromUri(
                "https://{userinfo}@localhost:8443/{path}?key={query}#{fragment}").buildAsString("user info@/", "path", "query", "fragment"));
        Assertions.assertEquals("https://user-info%E2%82%AC@localhost:8443", KeycloakUriBuilder.fromUri(
                "https://user-info%E2%82%AC@localhost:8443", false).buildAsString());
        Assertions.assertEquals("https://user-info%E2%82%AC@localhost:8443", KeycloakUriBuilder.fromUri(
                "https://user-info€@localhost:8443", false).buildAsString());
    }

    @Test
    public void testEmptyHostname() {
        Assertions.assertEquals("app.immich:///oauth-callback", KeycloakUriBuilder.fromUri(
                "app.immich:///oauth-callback").buildAsString());
    }
}
