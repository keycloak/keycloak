package org.keycloak.testsuite.admin;

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.models.BrowserSecurityHeaders;
import org.keycloak.testsuite.util.UserBuilder;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.io.IOException;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

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

        assertDefaultValue(BrowserSecurityHeaders.STRICT_TRANSPORT_SECURITY, h);
        assertDefaultValue(BrowserSecurityHeaders.X_FRAME_OPTIONS, h);
        assertDefaultValue(BrowserSecurityHeaders.X_CONTENT_TYPE_OPTIONS, h);
        assertDefaultValue(BrowserSecurityHeaders.X_XSS_PROTECTION, h);
        assertDefaultValue(BrowserSecurityHeaders.REFERRER_POLICY, h);

        response.close();
    }

    private void assertDefaultValue(BrowserSecurityHeaders header, MultivaluedMap<String, Object> h) {
        assertThat(h.getFirst(header.getHeaderName()), is(equalTo(header.getDefaultValue())));
    }

    private String getAdminUrl(String resource) {
        return suiteContext.getAuthServerInfo().getContextRoot().toString() + "/auth/admin/" + resource;
    }

}
