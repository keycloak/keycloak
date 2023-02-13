package org.keycloak.testsuite.admin;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpOptions;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.services.resources.Cors;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class AdminPreflightTest extends AbstractAdminTest {


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
    public void testPreflight() throws IOException {
        HttpOptions options = new HttpOptions(getAdminUrl("realms/master/users"));
        options.setHeader("Origin", "http://test");

        CloseableHttpResponse response = client.execute(options);
        assertEquals(200, response.getStatusLine().getStatusCode());
        assertEquals("true", response.getFirstHeader(Cors.ACCESS_CONTROL_ALLOW_CREDENTIALS).getValue());
        assertEquals("DELETE, POST, GET, PUT", response.getFirstHeader(Cors.ACCESS_CONTROL_ALLOW_METHODS).getValue());
        assertEquals("http://test", response.getFirstHeader(Cors.ACCESS_CONTROL_ALLOW_ORIGIN).getValue());
        assertEquals("3600", response.getFirstHeader(Cors.ACCESS_CONTROL_MAX_AGE).getValue());
        assertTrue(response.getFirstHeader(Cors.ACCESS_CONTROL_ALLOW_HEADERS).getValue().contains("Authorization"));
        assertTrue(response.getFirstHeader(Cors.ACCESS_CONTROL_ALLOW_HEADERS).getValue().contains("Content-Type"));
    }

    private String getAdminUrl(String resource) {
        return suiteContext.getAuthServerInfo().getContextRoot().toString() + "/auth/admin/" + resource;
    }

}
