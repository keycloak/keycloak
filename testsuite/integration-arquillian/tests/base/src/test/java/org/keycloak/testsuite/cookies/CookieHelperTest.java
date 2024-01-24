package org.keycloak.testsuite.cookies;

import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.services.util.CookieHelper;
import org.keycloak.testsuite.AbstractKeycloakTest;
import org.keycloak.testsuite.client.KeycloakTestingClient;

import java.io.IOException;
import java.util.List;
import java.util.Set;

public class CookieHelperTest extends AbstractKeycloakTest {

    private KeycloakTestingClient testing;
    private SetHeaderFilter filter;

    @Before
    public void before() {
        filter = new SetHeaderFilter();
        String serverUrl = suiteContext.getAuthServerInfo().getContextRoot().toString() + "/auth";
        ResteasyClientBuilder restEasyClientBuilder = KeycloakTestingClient.getRestEasyClientBuilder(serverUrl);
        ResteasyClient resteasyClient = restEasyClientBuilder.build();
        resteasyClient.register(filter);
        testing = KeycloakTestingClient.getInstance(serverUrl, resteasyClient);
    }

    @After
    public void after() {
        testing.close();
    }

    @Test
    public void testCookieHeaderWithSpaces() {
        filter.setHeader("Cookie", "terms_user=; KC_RESTART=eyJhbGciOiJIUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJhZDUyMjdhMy1iY2ZkLTRjZjAtYTdiNi0zOTk4MzVhMDg1NjYifQ.eyJjaWQiOiJodHRwczovL3Nzby5qYm9zcy5vcmciLCJwdHkiOiJzYW1sIiwicnVyaSI6Imh0dHBzOi8vc3NvLmpib3NzLm9yZy9sb2dpbj9wcm92aWRlcj1SZWRIYXRFeHRlcm5hbFByb3ZpZGVyIiwiYWN0IjoiQVVUSEVOVElDQVRFIiwibm90ZXMiOnsiU0FNTF9SRVFVRVNUX0lEIjoibXBmbXBhYWxkampqa2ZmcG5oYmJoYWdmZmJwam1rbGFqbWVlb2lsaiIsInNhbWxfYmluZGluZyI6InBvc3QifX0.d0QJSOQ6pJGzqcjqDTRwkRpU6fwYeICedL6R9Gqs8CQ; AUTH_SESSION_ID=451ec4be-a0c8-430e-b489-6580f195ccf0; AUTH_SESSION_ID=55000981-8b5e-4c8d-853f-ee4c582c1d0d;AUTH_SESSION_ID=451ec4be-a0c8-430e-b489-6580f195ccf0; AUTH_SESSION_ID=55000981-8b5e-4c8d-853f-ee4c582c1d0d;AUTH_SESSION_ID=451ec4be-a0c8-430e-b489-6580f195ccf0; AUTH_SESSION_ID=55000981-8b5e-4c8d-853f-ee4c582c1d0d4;");

        testing.server().run(session -> {
            Set<String> authSessionIds = CookieHelper.getCookieValues(session, "AUTH_SESSION_ID");
            Assert.assertEquals(3, authSessionIds.size());
        });
    }

    @Test
    public void testLegacyCookie() {
        filter.setHeader("Cookie", "MYCOOKIE=new;MYCOOKIE_LEGACY=legacy");

        testing.server().run(session -> {
            Assert.assertEquals("new", CookieHelper.getCookieValue(session, "MYCOOKIE"));

            Set<String> cookieValues = CookieHelper.getCookieValues(session, "MYCOOKIE");
            Assert.assertEquals(1, cookieValues.size());
            Assert.assertEquals("new", cookieValues.iterator().next());
        });

        filter.setHeader("Cookie", "MYCOOKIE_LEGACY=legacy");

        testing.server().run(session -> {
            Assert.assertEquals("legacy", CookieHelper.getCookieValue(session, "MYCOOKIE"));

            Set<String> cookieValues = CookieHelper.getCookieValues(session, "MYCOOKIE");
            Assert.assertEquals(1, cookieValues.size());
            Assert.assertEquals("legacy", cookieValues.iterator().next());
        });
    }

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
    }

    public static class SetHeaderFilter implements ClientRequestFilter {

        private String key;
        private String value;

        public void setHeader(String key, String value) {
            this.key = key;
            this.value = value;
        }

        @Override
        public void filter(ClientRequestContext requestContext) throws IOException {
            if (key != null && value != null) {
                requestContext.getHeaders().add(key, value);
            }
        }
    }

}
