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

import org.junit.Assert;
import org.junit.Test;

/**
 * @author rmartinc
 */
public class KeycloakUriBuilderTest {

    @Test
    public void test() {
        Assert.assertEquals("http://localhost:8080/path?attr1=value1&attr2=value2",
                KeycloakUriBuilder.fromUri("http://localhost:8080/path?attr1=value1&attr2=value2")
                        .build().toString());

        Assert.assertEquals("http://localhost/path?attr1=value1&attr2=value2",
                KeycloakUriBuilder.fromUri("http://localhost:80")
                        .path("path")
                        .queryParam("attr1", "value1")
                        .queryParam("attr2", "value2")
                        .build().toString());

        Assert.assertEquals("unknown://localhost:9000/path",
                KeycloakUriBuilder.fromUri("unknown://localhost:9000/path").build().toString());

        Assert.assertEquals("https://localhost/path?attr1=value1",
                KeycloakUriBuilder.fromUri("https://{hostname}:443/path?attr1={value}")
                        .build("localhost", "value1").toString());

        Assert.assertEquals("https://localhost:8443/path?attr1=value1",
                KeycloakUriBuilder.fromUri("https://localhost:8443/path?attr1={value}")
                        .buildFromMap(Collections.singletonMap("value", "value1")).toString());
    }

    @Test
    public void testPort() {
        Assert.assertEquals("https://localhost:8443/path", KeycloakUriBuilder.fromUri("https://localhost:8443/path").buildAsString());
        Assert.assertEquals("https://localhost:8443/path", KeycloakUriBuilder.fromUri("https://localhost:8443/path").preserveDefaultPort().buildAsString());

        Assert.assertEquals("https://localhost/path", KeycloakUriBuilder.fromUri("https://localhost:443/path").buildAsString());
        Assert.assertEquals("https://localhost:443/path", KeycloakUriBuilder.fromUri("https://localhost:443/path").preserveDefaultPort().buildAsString());

        Assert.assertEquals("http://localhost/path", KeycloakUriBuilder.fromUri("http://localhost:80/path").buildAsString());
        Assert.assertEquals("http://localhost:80/path", KeycloakUriBuilder.fromUri("http://localhost:80/path").preserveDefaultPort().buildAsString());

        // Port always preserved (even if preserverPort not specified) due the port 80 doesn't match "https" scheme
        Assert.assertEquals("https://localhost:80/path", KeycloakUriBuilder.fromUri("https://localhost:80/path").buildAsString());

        // Port not in the build URL when it was not specified in the original URL (even if preserverPort() is true)
        Assert.assertEquals("http://localhost/path", KeycloakUriBuilder.fromUri("http://localhost/path").buildAsString());
        Assert.assertEquals("http://localhost/path", KeycloakUriBuilder.fromUri("http://localhost/path").preserveDefaultPort().buildAsString());
        Assert.assertEquals("https://localhost/path", KeycloakUriBuilder.fromUri("https://localhost/path").buildAsString());
        Assert.assertEquals("https://localhost/path", KeycloakUriBuilder.fromUri("https://localhost/path").preserveDefaultPort().buildAsString());
    }

    @Test
    public void testTemplateAndNotTemplate() {
        Assert.assertEquals("https://localhost:8443/path?key=query#fragment", KeycloakUriBuilder.fromUri(
                "https://localhost:8443/{path}?key={query}#{fragment}").buildAsString("path", "query", "fragment"));
        Assert.assertEquals("https://localhost:8443/%7Bpath%7D?key=%7Bquery%7D#%7Bfragment%7D", KeycloakUriBuilder.fromUri(
                "https://localhost:8443/{path}?key={query}#{fragment}", false).buildAsString());
    }

    @Test
    public void testUserInfo() {
        Assert.assertEquals("https://user-info@localhost:8443/path?key=query#fragment", KeycloakUriBuilder.fromUri(
                "https://{userinfo}@localhost:8443/{path}?key={query}#{fragment}").buildAsString("user-info", "path", "query", "fragment"));
        Assert.assertEquals("https://user%20info%40%2F@localhost:8443/path?key=query#fragment", KeycloakUriBuilder.fromUri(
                "https://{userinfo}@localhost:8443/{path}?key={query}#{fragment}").buildAsString("user info@/", "path", "query", "fragment"));
        Assert.assertEquals("https://user-info%E2%82%AC@localhost:8443", KeycloakUriBuilder.fromUri(
                "https://user-info%E2%82%AC@localhost:8443", false).buildAsString());
        Assert.assertEquals("https://user-info%E2%82%AC@localhost:8443", KeycloakUriBuilder.fromUri(
                "https://user-info€@localhost:8443", false).buildAsString());
    }

    @Test
    public void testEmptyHostname() {
        Assert.assertEquals("app.immich:///oauth-callback", KeycloakUriBuilder.fromUri(
                "app.immich:///oauth-callback").buildAsString());
    }
}
