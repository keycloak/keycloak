package org.keycloak.protocol.oidc.utils;

import org.hamcrest.Matcher;
import org.jboss.resteasy.spi.ResteasyUriInfo;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.models.ClientModel;
import org.keycloak.models.RealmModel;
import org.mockito.Mock;

import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class RedirectUtilsTest {

    @Mock
    ClientModel client;
    @Mock
    RealmModel realm;
    private Set<String> validRedirects;
    private UriInfo uriInfo;

    @Before
    public void setUp() throws Exception {
        initMocks(this);

        validRedirects = new HashSet<>(Arrays.asList("http://example.com", "http://with-dash.example.com/*",
                "http://sub.domain.example.com/foo", "http://localhost:8180/*", "http://*.subwildcard.example.com"));
        uriInfo = new ResteasyUriInfo(new URI("http://localhost:8080/auth/realms/test/protocol/openid-connect/auth"));

        when(realm.getName()).thenReturn("test-realm");
        when(client.getRedirectUris()).thenReturn(validRedirects);
    }

    @Test
    public void testNoParamMultipleValidUris() throws Exception {
        assertThat(RedirectUtils.verifyRedirectUri(uriInfo, null, realm, client), nullValue());
    }

    @Test
    public void testNoParamNoValidUris() throws Exception {
        when(client.getRedirectUris()).thenReturn(Collections.EMPTY_SET);
        assertThat(RedirectUtils.verifyRedirectUri(uriInfo, null, realm, client), nullValue());
    }

    @Test
    public void testNoValidUris() throws Exception {
        when(client.getRedirectUris()).thenReturn(Collections.EMPTY_SET);
        assertThat(RedirectUtils.verifyRedirectUri(uriInfo, "http://with-dash.example.com", realm, client), nullValue());
    }

    @Test
    public void testValid() throws Exception {
        checkRedirectUri("http://with-dash.example.com");
    }

    @Test
    public void testWithParams() throws Exception {
        checkRedirectUri("http://with-dash.example.com/return?key=value&otherKey=otherValue");
    }

    @Test
    public void testQueryComponents() throws Exception {
        checkRedirectUri("http://with-dash.example.com/return?key=value/");
        checkRedirectUri("http://with-dash.example.com/return?key");
        checkRedirectUri("http://with-dash.example.com/return?key&otherKey=otherValue");
        checkRedirectUri("http://with-dash.example.com/return?key&otherKey");
    }

    @Test
    public void testWildcard() throws Exception {
        checkRedirectUri("http://with-dash.example.com:8080", nullValue());
        checkRedirectUri("http://with-dash.example.com/somepath");
        checkRedirectUri("http://with-dash.example.com/somepath/with/long/path");
        checkRedirectUri("http://localhost:8180/somepath/with/long/path");
    }

    @Test
    public void testDifferentCaseInHostname() throws Exception {
        checkRedirectUri("http://wItH-DaSh.example.com", equalTo("http://with-dash.example.com"));
        checkRedirectUri("hTTP://wItH-DaSh.example.com", equalTo("http://with-dash.example.com"));
        checkRedirectUri("httP://sub.dOMAIn.example.com/foo", equalTo("http://sub.domain.example.com/foo"));
    }

    @Test
    public void testWildcardSubdomain() throws Exception {
        checkRedirectUri("http://dev1.subwildcard.example.com");
    }

    private void checkRedirectUri(final String redirect) {
        assertThat(RedirectUtils.verifyRedirectUri(uriInfo, redirect, realm, client), equalTo(redirect));
    }

    private void checkRedirectUri(final String redirect, final Matcher matcher) {
        assertThat(RedirectUtils.verifyRedirectUri(uriInfo, redirect, realm, client), matcher);
    }
}
