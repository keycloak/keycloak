package org.keycloak.protocol.oidc.utils;

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

        validRedirects = new HashSet<>(Arrays.asList(
                "http://example.com",
                "http://with-dash.example.com/*",
                "http://sub.domain.example.com/foo",
                "http://localhost:8180/*",
                "http://*.subwildcard.example.com"));
        uriInfo = new ResteasyUriInfo(new URI("http://localhost:8180/auth/realms/test/protocol/openid-connect/auth"));

        when(realm.getName()).thenReturn("test-realm");
        when(client.getRedirectUris()).thenReturn(validRedirects);
    }

    @Test
    public void shouldReturnNull_whenNoRedirectUriInput() throws Exception {
        assertThat(RedirectUtils.verifyRedirectUri(uriInfo, null, realm, client), nullValue());
    }

    @Test
    public void shouldReturnNull_whenNoValidRedirectUrisSpecified() throws Exception {
        when(client.getRedirectUris()).thenReturn(Collections.EMPTY_SET);
        assertThat(RedirectUtils.verifyRedirectUri(uriInfo, "http://localhost:8180/", realm, client), nullValue());
    }

    @Test
    public void shouldReturnNull_whenPortNumberIncorrect() throws Exception {
        assertThat(RedirectUtils.verifyRedirectUri(uriInfo, "http://localhost:8181/", realm, client), nullValue());
    }

    @Test
    public void shouldReturnNull_whenNewPortAddedToHostname() throws Exception {
        assertThat(RedirectUtils.verifyRedirectUri(uriInfo, "http://with-dash.example.com:8080", realm, client), nullValue());
    }

    @Test
    public void shouldReturnRequestedRedirect_withDash() throws Exception {
        assertThat(RedirectUtils.verifyRedirectUri(uriInfo, "http://with-dash.example.com", realm, client),
                equalTo("http://with-dash.example.com"));
    }

    @Test
    public void shouldReturnRequestedRedirect_withQueryParams() throws Exception {
        assertThat(RedirectUtils.verifyRedirectUri(uriInfo, "http://with-dash.example.com/return?key=value/", realm, client),
                equalTo("http://with-dash.example.com/return?key=value/"));
        assertThat(RedirectUtils.verifyRedirectUri(uriInfo, "http://with-dash.example.com/return?key", realm, client),
                equalTo("http://with-dash.example.com/return?key"));
        assertThat(RedirectUtils.verifyRedirectUri(uriInfo, "http://with-dash.example.com/return?key&otherKey=otherValue", realm, client),
                equalTo("http://with-dash.example.com/return?key&otherKey=otherValue"));
        assertThat(RedirectUtils.verifyRedirectUri(uriInfo, "http://with-dash.example.com/return?key&otherKey", realm, client),
                equalTo("http://with-dash.example.com/return?key&otherKey"));
    }

    @Test
    public void shouldReturnRequestedRedirect_forVariousPathsAfterWildcard() throws Exception {
        assertThat(RedirectUtils.verifyRedirectUri(uriInfo, "http://with-dash.example.com/somepath", realm, client),
                equalTo("http://with-dash.example.com/somepath"));
        assertThat(RedirectUtils.verifyRedirectUri(uriInfo, "http://with-dash.example.com/somepath/with/long/path", realm, client),
                equalTo("http://with-dash.example.com/somepath/with/long/path"));
        assertThat(RedirectUtils.verifyRedirectUri(uriInfo, "http://localhost:8180/somepath/with/long/path", realm, client),
                equalTo("http://localhost:8180/somepath/with/long/path"));
    }

    @Test
    public void shouldReturnRequestedRedirect_whenDifferentCasedHostnameGiven() throws Exception {
        assertThat(RedirectUtils.verifyRedirectUri(uriInfo, "http://wItH-DaSh.example.com", realm, client),
                equalTo("http://with-dash.example.com"));
        assertThat(RedirectUtils.verifyRedirectUri(uriInfo, "hTTP://wItH-DaSh.example.com", realm, client),
                equalTo("http://with-dash.example.com"));
        assertThat(RedirectUtils.verifyRedirectUri(uriInfo, "httP://sub.dOMAIn.example.com/foo", realm, client),
                equalTo("http://sub.domain.example.com/foo"));
    }

    @Test
    public void shouldReturnRequestedRedirect_forWildcardedSubdomain() throws Exception {
        assertThat(RedirectUtils.verifyRedirectUri(uriInfo, "http://dev1.subwildcard.example.com", realm, client),
                equalTo("http://dev1.subwildcard.example.com"));
    }

    @Test
    public void shouldReturnRequestedRedirect_forRelativeDomain() throws Exception {
        assertThat(RedirectUtils.verifyRedirectUri(uriInfo, "/client1/landing", realm, client), equalTo("http://localhost:8180/client1/landing"));
    }
}
