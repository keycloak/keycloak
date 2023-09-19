/*
 * Copyright 2023 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.protocol.oidc.utils;

import java.net.URI;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.jboss.resteasy.core.ResteasyContext;
import org.jboss.resteasy.mock.MockHttpRequest;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.keycloak.common.Profile;
import org.keycloak.common.crypto.CryptoIntegration;
import org.keycloak.common.crypto.CryptoProvider;
import org.keycloak.http.HttpRequest;
import org.keycloak.models.KeycloakSession;
import org.keycloak.services.DefaultKeycloakSession;
import org.keycloak.services.DefaultKeycloakSessionFactory;
import org.keycloak.services.HttpRequestImpl;

/**
 * <p>Little test class for RedirectUtils methods.</p>
 *
 * @author rmartinc
 */
public class RedirectUtilsTest {

    private static KeycloakSession session;

    @BeforeClass
    public static void beforeClass() {
        HttpRequest httpRequest = new HttpRequestImpl(MockHttpRequest.create("GET", URI.create("https://keycloak.org/"), URI.create("https://keycloak.org")));
        ResteasyContext.getContextDataMap().put(HttpRequest.class, httpRequest);
        Profile.defaults();
        CryptoIntegration.init(CryptoProvider.class.getClassLoader());
        DefaultKeycloakSessionFactory sessionFactory = new DefaultKeycloakSessionFactory();
        sessionFactory.init();
        session = new DefaultKeycloakSession(sessionFactory);
    }

    @Test
    public void testverifyRedirectUriHttps() {
        Set<String> set = Stream.of(
                "https://keycloak.org/test1",
                "https://keycloak.org/test2",
                "https://keycloak.org/parent/*"
        ).collect(Collectors.toSet());

        Assert.assertEquals("https://keycloak.org/test1", RedirectUtils.verifyRedirectUri(session, null, "https://keycloak.org/test1", set, false));
        Assert.assertEquals("https://keycloak.org/test2", RedirectUtils.verifyRedirectUri(session, null, "https://keycloak.org/test2", set, false));
        Assert.assertEquals("https://keycloak.org/parent", RedirectUtils.verifyRedirectUri(session, null, "https://keycloak.org/parent", set, false));
        Assert.assertEquals("https://keycloak.org/parent/child", RedirectUtils.verifyRedirectUri(session, null, "https://keycloak.org/parent/child", set, false));

        Assert.assertNull(RedirectUtils.verifyRedirectUri(session, null, "https://keycloak.org/test", set, false));
        Assert.assertNull(RedirectUtils.verifyRedirectUri(session, null, "https://keycloak.org/test1/child", set, false));
        Assert.assertNull(RedirectUtils.verifyRedirectUri(session, null, "https://keycloak.com/test", set, false));
    }

    @Test
    public void testverifyRedirectUriMixedSchemes() {
        Set<String> set = Stream.of(
                "https://keycloak.org/*",
                "custom1:/test1",
                "custom1:/test2",
                "custom1:/parent/*",
                "custom2:*"
        ).collect(Collectors.toSet());

        Assert.assertEquals("custom1:/test1", RedirectUtils.verifyRedirectUri(session, null, "custom1:/test1", set, false));
        Assert.assertEquals("custom1:/test2", RedirectUtils.verifyRedirectUri(session, null, "custom1:/test2", set, false));
        Assert.assertEquals("custom1:/parent/child", RedirectUtils.verifyRedirectUri(session, null, "custom1:/parent/child", set, false));
        Assert.assertEquals("custom2:/something", RedirectUtils.verifyRedirectUri(session, null, "custom2:/something", set, false));
        Assert.assertEquals("https://keycloak.org/test", RedirectUtils.verifyRedirectUri(session, null, "https://keycloak.org/test", set, false));

        Assert.assertNull(RedirectUtils.verifyRedirectUri(session, null, "custom1:/test", set, false));
        Assert.assertNull(RedirectUtils.verifyRedirectUri(session, null, "custom1:/test1/test", set, false));
        Assert.assertNull(RedirectUtils.verifyRedirectUri(session, null, "custom3:/test", set, false));
    }

    @Test
    public void testverifyRedirectUriInvalidScheme() {
        Set<String> set = Stream.of(
                "custom1:/test1",
                "custom1:/test2",
                "custom1:/parent/*",
                "custom2:*",
                "*"
        ).collect(Collectors.toSet());

        Assert.assertEquals("custom1:/test1", RedirectUtils.verifyRedirectUri(session, null, "custom1:/test1", set, false));
        Assert.assertEquals("custom1:/test2", RedirectUtils.verifyRedirectUri(session, null, "custom1:/test2", set, false));
        Assert.assertEquals("custom1:/parent/child", RedirectUtils.verifyRedirectUri(session, null, "custom1:/parent/child", set, false));
        Assert.assertEquals("custom2:/something", RedirectUtils.verifyRedirectUri(session, null, "custom2:/something", set, false));
        Assert.assertEquals("https://keycloak.org/test", RedirectUtils.verifyRedirectUri(session, null, "https://keycloak.org/test", set, false));
        Assert.assertEquals("http://keycloak.org/test", RedirectUtils.verifyRedirectUri(session, null, "http://keycloak.org/test", set, false));
        Assert.assertEquals("https://keycloak.org/test", RedirectUtils.verifyRedirectUri(session, null, "/test", set, false));
        Assert.assertEquals("https://keycloak.com/test", RedirectUtils.verifyRedirectUri(session, "https://keycloak.com", "/test", set, false));

        Assert.assertNull(RedirectUtils.verifyRedirectUri(session, null, "custom3:/test", set, false));
    }

    @Test
    public void testverifyRedirectUriWithCurlyBrackets() {
        Set<String> set = Stream.of(
                "https://keycloak.org/%7B123%7D",
                "https://keycloak.org/parent/*"
        ).collect(Collectors.toSet());

        Assert.assertEquals("https://keycloak.org/%7B123%7D", RedirectUtils.verifyRedirectUri(session, null, "https://keycloak.org/%7B123%7D", set, false));
        Assert.assertEquals("https://keycloak.org/parent/%7B123%7D", RedirectUtils.verifyRedirectUri(session, null, "https://keycloak.org/parent/%7B123%7D", set, false));

        Assert.assertNull(RedirectUtils.verifyRedirectUri(session, null, "https://keycloak.org/%7Babc%7D", set, false));
    }

    @Test
    public void testverifyInvalidRedirectUri() {
        Set<String> set = Stream.of(
                "https://keycloak.org/*"
        ).collect(Collectors.toSet());

        Assert.assertEquals("https://keycloak.org/path%20space/", RedirectUtils.verifyRedirectUri(session, null, "https://keycloak.org/path%20space/", set, false));
        Assert.assertEquals("https://keycloak.org/path%3Cless/", RedirectUtils.verifyRedirectUri(session, null, "https://keycloak.org/path%3Cless/", set, false));
        Assert.assertEquals("https://keycloak.org/path/index.jsp?param=v1+v2", RedirectUtils.verifyRedirectUri(session, null, "https://keycloak.org/path/index.jsp?param=v1+v2", set, false));

        Assert.assertNull(RedirectUtils.verifyRedirectUri(session, null, "https://keycloak.org/path space/", set, false));
        Assert.assertNull(RedirectUtils.verifyRedirectUri(session, null, "https://keycloak.org/path<less/", set, false));
        Assert.assertNull(RedirectUtils.verifyRedirectUri(session, null, "https://keycloak.org/path/index.jsp?param=v1 v2", set, false));
    }
}
