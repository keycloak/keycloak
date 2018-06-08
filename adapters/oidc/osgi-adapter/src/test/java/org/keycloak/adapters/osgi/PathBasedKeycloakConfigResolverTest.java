/*
 * Copyright 2018 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.adapters.osgi;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.adapters.OIDCHttpFacade;
import org.keycloak.adapters.spi.AuthenticationError;
import org.keycloak.adapters.spi.HttpFacade;
import org.keycloak.adapters.spi.LogoutError;

import static org.junit.Assert.assertNotNull;

public class PathBasedKeycloakConfigResolverTest {

    @Test
    public void relativeURIsAndContexts() throws Exception {
        PathBasedKeycloakConfigResolver resolver = new PathBasedKeycloakConfigResolver();

        assertNotNull(populate(resolver, "test")
                .resolve(new MockRequest("http://localhost/test/a/b/c?d=e", "/a/b/c")));

        assertNotNull(populate(resolver, "test")
                .resolve(new MockRequest("http://localhost/test/a/b/c?d=e", "/a/b")));

        // means default context and actually we use first segment
        assertNotNull(populate(resolver, "test")
                .resolve(new MockRequest("http://localhost/test/a/b/c?d=e", "/test/a/b/c")));

        assertNotNull(populate(resolver, "test/a")
                .resolve(new MockRequest("http://localhost/test/a/b/c?d=e", "/b/c")));

        assertNotNull(populate(resolver, "")
                .resolve(new MockRequest("http://localhost/", "/")));
    }

    @SuppressWarnings("unchecked")
    private PathBasedKeycloakConfigResolver populate(PathBasedKeycloakConfigResolver resolver, String context)
            throws Exception {
        Field f = PathBasedKeycloakConfigResolver.class.getDeclaredField("cache");
        f.setAccessible(true);
        Map<String, KeycloakDeployment> cache = (Map<String, KeycloakDeployment>) f.get(resolver);
        cache.clear();
        cache.put(context, new KeycloakDeployment());

        return resolver;
    }

    private class MockRequest implements OIDCHttpFacade.Request {

        private String uri;
        private String relativePath;

        public MockRequest(String uri, String relativePath) {
            this.uri = uri;
            this.relativePath = relativePath;
        }

        @Override
        public String getMethod() {
            return null;
        }

        @Override
        public String getURI() {
            return this.uri;
        }

        @Override
        public String getRelativePath() {
            return this.relativePath;
        }

        @Override
        public boolean isSecure() {
            return false;
        }

        @Override
        public String getFirstParam(String param) {
            return null;
        }

        @Override
        public String getQueryParamValue(String param) {
            return null;
        }

        @Override
        public HttpFacade.Cookie getCookie(String cookieName) {
            return null;
        }

        @Override
        public String getHeader(String name) {
            return null;
        }

        @Override
        public List<String> getHeaders(String name) {
            return null;
        }

        @Override
        public InputStream getInputStream() {
            return null;
        }

        @Override
        public InputStream getInputStream(boolean buffered) {
            return null;
        }

        @Override
        public String getRemoteAddr() {
            return null;
        }

        @Override
        public void setError(AuthenticationError error) {

        }

        @Override
        public void setError(LogoutError error) {

        }
    }

}
