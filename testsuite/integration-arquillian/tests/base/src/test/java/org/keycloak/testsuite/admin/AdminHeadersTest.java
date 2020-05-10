package org.keycloak.testsuite.admin;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpOptions;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.models.BrowserSecurityHeaders;
import org.keycloak.services.resources.Cors;
import org.keycloak.testsuite.util.UserBuilder;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class AdminHeadersTest extends AbstractAdminTest {


    private CloseableHttpClient client;

    @Before
    public void before() {
        client = HttpClientBuilder.create().build();
    }

    @After
    public void after() {
        try {
            client.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testHeaders() {
        Response response = realm.users().create(UserBuilder.create().username("headers-user").build());
        MultivaluedMap<String, Object> h = response.getHeaders();

        assertEquals(BrowserSecurityHeaders.STRICT_TRANSPORT_SECURITY_DEFAULT, h.getFirst(BrowserSecurityHeaders.STRICT_TRANSPORT_SECURITY));
        assertEquals(BrowserSecurityHeaders.X_FRAME_OPTIONS_DEFAULT, h.getFirst(BrowserSecurityHeaders.X_FRAME_OPTIONS));
        assertEquals(BrowserSecurityHeaders.X_CONTENT_TYPE_OPTIONS_DEFAULT, h.getFirst(BrowserSecurityHeaders.X_CONTENT_TYPE_OPTIONS));
        assertEquals(BrowserSecurityHeaders.X_XSS_PROTECTION_DEFAULT, h.getFirst(BrowserSecurityHeaders.X_XSS_PROTECTION));

        response.close();
    }

    private String getAdminUrl(String resource) {
        return suiteContext.getAuthServerInfo().getContextRoot().toString() + "/auth/admin/" + resource;
    }

}
