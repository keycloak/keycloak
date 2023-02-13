package org.keycloak.testsuite.rest.resource;

import org.keycloak.headers.SecurityHeadersProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.testsuite.rest.TestingResourceProvider;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import static org.keycloak.testsuite.util.ServerURLs.getAuthServerContextRoot;

/**
 * @author mhajas
 */
public class TestJavascriptResource {

    private KeycloakSession session;

    public TestJavascriptResource(KeycloakSession session) {
        this.session = session;
    }

    @GET
    @Path("/js/keycloak.js")
    @Produces("application/javascript")
    public String getJavascriptAdapter() throws IOException {
        return resourceToString("/javascript/keycloak.js");
    }

    @GET
    @Path("/index.html")
    @Produces(MediaType.TEXT_HTML)
    public String getJavascriptTestingEnvironment() throws IOException {
        session.getProvider(SecurityHeadersProvider.class).options().skipHeaders();
        return resourceToString("/javascript/index.html");
    }

    @GET
    @Path("/init-in-head.html")
    @Produces(MediaType.TEXT_HTML)
    public String getJavascriptTestingEnvironmentWithInitInHead() throws IOException {
        session.getProvider(SecurityHeadersProvider.class).options().skipHeaders();
        return resourceToString("/javascript/init-in-head.html");
    }

    @GET
    @Path("/silent-check-sso.html")
    @Produces(MediaType.TEXT_HTML)
    public String getJavascriptTestingEnvironmentSilentCheckSso() throws IOException {
        return resourceToString("/javascript/silent-check-sso.html");
    }

    @GET
    @Path("/keycloak.json")
    @Produces(MediaType.APPLICATION_JSON)
    public String getKeycloakJSON() throws IOException {
        return resourceToString("/javascript/keycloak.json");
    }

    private String resourceToString(String path) throws IOException {
        try (InputStream is = TestingResourceProvider.class.getResourceAsStream(path);
             BufferedReader buf = new BufferedReader(new InputStreamReader(is))) {
            String line = buf.readLine();
            StringBuilder sb = new StringBuilder();
            while (line != null) {
                sb.append(line).append("\n");
                line = buf.readLine();
            }

            return sb.toString().replace("${js-adapter.auth-server-url}", getAuthServerContextRoot() + "/auth");
        }
    }
}
