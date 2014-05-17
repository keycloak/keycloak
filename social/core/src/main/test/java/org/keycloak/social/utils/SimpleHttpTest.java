package org.keycloak.social.utils;

import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.ServletInfo;
import org.codehaus.jackson.JsonNode;
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
        JsonNode o = SimpleHttp.doPost("http://localhost:8081/tojson").asJson();
        JsonNode p = o.get("params");

        assertEquals(0, p.size());
    }

    @Test
    public void testPost() throws IOException {
        JsonNode o = SimpleHttp.doPost("http://localhost:8081/tojson").param("key-one", "value one ;)").param("key-two", "value two!&").asJson();
        JsonNode p = o.get("params");

        assertEquals(2, p.size());
        assertEquals("value one ;)", p.get("key-one").getTextValue());
        assertEquals("value two!&", p.get("key-two").getTextValue());
    }

    @Test
    public void testPostCustomHeader() throws IOException {
        JsonNode o = SimpleHttp.doPost("http://localhost:8081/tojson").header("Accept", "application/json").header("Authorization", "bearer dsfsadfsdf").asJson();
        JsonNode h = o.get("headers");

        assertEquals("application/json", h.get("Accept").getTextValue());
        assertEquals("bearer dsfsadfsdf", h.get("Authorization").getTextValue());
    }

    @Test
    public void testGetNoParams() throws IOException {
        JsonNode o = SimpleHttp.doGet("http://localhost:8081/tojson").asJson();
        JsonNode p = o.get("params");

        assertEquals(0, p.size());
    }

    @Test
    public void testGet() throws IOException {
        JsonNode o = SimpleHttp.doGet("http://localhost:8081/tojson").param("key-one", "value one ;)").param("key-two", "value two!&").asJson();
        JsonNode p = o.get("params");

        assertEquals(2, p.size());
        assertEquals("value one ;)", p.get("key-one").getTextValue());
        assertEquals("value two!&", p.get("key-two").getTextValue());
    }

    @Test
    public void testGetCustomHeader() throws IOException {
        JsonNode o = SimpleHttp.doGet("http://localhost:8081/tojson").header("Authorization", "bearer dsfsadfsdf").asJson();
        JsonNode h = o.get("headers");

        assertEquals("bearer dsfsadfsdf", h.get("Authorization").getTextValue());
    }

}
