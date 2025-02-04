package org.keycloak.tests.admin;

import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;
import org.keycloak.models.BrowserSecurityHeaders;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ManagedRealm;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

@KeycloakIntegrationTest
public class AdminHeadersTest {

    private static final String X_XSS_HEADER = "X-XSS-Protection";

    @InjectRealm
    private ManagedRealm realm;

    @Test
    public void testHeaders() {
        UserRepresentation userRep = new UserRepresentation();
        userRep.setUsername("headers-user");
        Response response = realm.admin().users().create(userRep);
        MultivaluedMap<String, Object> h = response.getHeaders();

        assertDefaultValue(BrowserSecurityHeaders.STRICT_TRANSPORT_SECURITY, h);
        assertDefaultValue(BrowserSecurityHeaders.X_FRAME_OPTIONS, h);
        assertDefaultValue(BrowserSecurityHeaders.X_CONTENT_TYPE_OPTIONS, h);
        assertDefaultValue(BrowserSecurityHeaders.REFERRER_POLICY, h);

        // Make sure that X-XSS-Protection header is not set. See: https://github.com/keycloak/keycloak/issues/21728
        assertThat(h.containsKey(X_XSS_HEADER), is(false));
        response.close();
    }

    private void assertDefaultValue(BrowserSecurityHeaders header, MultivaluedMap<String, Object> h) {
        assertThat(h.getFirst(header.getHeaderName()), is(equalTo(header.getDefaultValue())));
    }
}
