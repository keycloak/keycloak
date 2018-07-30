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

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class HierarchicalPathBasedKeycloakConfigResolverTest {

    @Test
    public void genericAndSpecificConfigurations() throws Exception {
        HierarchicalPathBasedKeycloakConfigResolver resolver = new HierarchicalPathBasedKeycloakConfigResolver();
        populate(resolver, true);

        assertThat(resolver.resolve(new MockRequest("http://localhost/a/b/c/d/e?a=b")).getRealm(), equalTo("a-b-c-d-e"));
        assertThat(resolver.resolve(new MockRequest("http://localhost/a/b/c/d/x?a=b")).getRealm(), equalTo("a-b-c-d"));
        assertThat(resolver.resolve(new MockRequest("http://localhost/a/b/c/x/x?a=b")).getRealm(), equalTo("a-b-c"));
        assertThat(resolver.resolve(new MockRequest("http://localhost/a/b/x/x/x?a=b")).getRealm(), equalTo("a-b"));
        assertThat(resolver.resolve(new MockRequest("http://localhost/a/x/x/x/x?a=b")).getRealm(), equalTo("a"));
        assertThat(resolver.resolve(new MockRequest("http://localhost/x/x/x/x/x?a=b")).getRealm(), equalTo(""));

        populate(resolver, false);
        try {
            resolver.resolve(new MockRequest("http://localhost/x/x/x/x/x?a=b"));
            fail("Expected java.lang.IllegalStateException: Can't find Keycloak configuration ...");
        } catch (IllegalStateException expected) {
        }
    }

    @SuppressWarnings("unchecked")
    private PathBasedKeycloakConfigResolver populate(PathBasedKeycloakConfigResolver resolver, boolean fallback)
            throws Exception {
        Field f = PathBasedKeycloakConfigResolver.class.getDeclaredField("cache");
        f.setAccessible(true);
        Map<String, KeycloakDeployment> cache = (Map<String, KeycloakDeployment>) f.get(resolver);
        cache.clear();
        cache.put("a-b-c-d-e", newKeycloakDeployment("a-b-c-d-e"));
        cache.put("a-b-c-d", newKeycloakDeployment("a-b-c-d"));
        cache.put("a-b-c", newKeycloakDeployment("a-b-c"));
        cache.put("a-b", newKeycloakDeployment("a-b"));
        cache.put("a", newKeycloakDeployment("a"));
        if (fallback) {
            cache.put("", newKeycloakDeployment(""));
        }

        return resolver;
    }

    private KeycloakDeployment newKeycloakDeployment(String realm) {
        KeycloakDeployment deployment = new KeycloakDeployment();
        deployment.setRealm(realm);

        return deployment;
    }

    private class MockRequest implements OIDCHttpFacade.Request {

        private String uri;

        public MockRequest(String uri) {
            this.uri = uri;
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
            return null;
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
