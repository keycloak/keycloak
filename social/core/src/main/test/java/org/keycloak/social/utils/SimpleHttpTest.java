package org.keycloak.social.utils;

import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.ServletInfo;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static io.undertow.servlet.Servlets.servlet;
import static org.junit.Assert.assertEquals;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class SimpleHttpTest {

    private UndertowServer server;

    @Before
    public void before() {
        server = new UndertowServer("localhost", 8081);

        DeploymentInfo deploymentInfo = new DeploymentInfo();
        deploymentInfo.setClassLoader(getClass().getClassLoader());
        deploymentInfo.setDeploymentName("test");
        deploymentInfo.setContextPath("/");

        ServletInfo servlet = servlet("ToJsonServlet", ToJsonServlet.class)
                .addMapping("/tojson");

        deploymentInfo.addServlet(servlet);

        server.deploy(deploymentInfo);

        server.start();
    }

    @After
    public void after() {
        server.stop();
    }

    @Test
    public void testPostNoParams() throws IOException {
        JSONObject o = SimpleHttp.doPost("http://localhost:8081/tojson").asJson();
        JSONObject p = o.getJSONObject("params");

        assertEquals(0, p.length());
    }

    @Test
    public void testPost() throws IOException {
        JSONObject o = SimpleHttp.doPost("http://localhost:8081/tojson").param("key-one", "value one ;)").param("key-two", "value two!&").asJson();
        JSONObject p = o.getJSONObject("params");

        assertEquals(2, p.length());
        assertEquals("value one ;)", p.getString("key-one"));
        assertEquals("value two!&", p.getString("key-two"));
    }

    @Test
    public void testPostCustomHeader() throws IOException {
        JSONObject o = SimpleHttp.doPost("http://localhost:8081/tojson").header("Accept", "application/json").header("Authorization", "bearer dsfsadfsdf").asJson();
        JSONObject h = o.getJSONObject("headers");

        assertEquals("application/json", h.getString("Accept"));
        assertEquals("bearer dsfsadfsdf", h.getString("Authorization"));
    }

    @Test
    public void testGetNoParams() throws IOException {
        JSONObject o = SimpleHttp.doGet("http://localhost:8081/tojson").asJson();
        JSONObject p = o.getJSONObject("params");

        assertEquals(0, p.length());
    }

    @Test
    public void testGet() throws IOException {
        JSONObject o = SimpleHttp.doGet("http://localhost:8081/tojson").param("key-one", "value one ;)").param("key-two", "value two!&").asJson();
        JSONObject p = o.getJSONObject("params");

        assertEquals(2, p.length());
        assertEquals("value one ;)", p.getString("key-one"));
        assertEquals("value two!&", p.getString("key-two"));
    }

    @Test
    public void testGetCustomHeader() throws IOException {
        JSONObject o = SimpleHttp.doGet("http://localhost:8081/tojson").header("Accept", "application/json").header("Authorization", "bearer dsfsadfsdf").asJson();
        JSONObject h = o.getJSONObject("headers");

        assertEquals("application/json", h.getString("Accept"));
        assertEquals("bearer dsfsadfsdf", h.getString("Authorization"));
    }

}
