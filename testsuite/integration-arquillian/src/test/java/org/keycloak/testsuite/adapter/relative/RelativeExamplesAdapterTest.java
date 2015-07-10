package org.keycloak.testsuite.adapter.relative;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.graphene.page.Page;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import org.keycloak.representations.idm.RealmRepresentation;
import static org.keycloak.testsuite.TestRealms.loadRealm;
import org.keycloak.testsuite.adapter.AbstractExamplesAdapterTest;
import static org.keycloak.testsuite.adapter.AbstractExamplesAdapterTest.EXAMPLES_HOME;
import static org.keycloak.testsuite.adapter.AbstractExamplesAdapterTest.EXAMPLES_VERSION_SUFFIX;
import org.keycloak.testsuite.page.adapter.BasicAuthExample;

/**
 *
 * @author tkyjovsk
 */
public class RelativeExamplesAdapterTest extends AbstractExamplesAdapterTest {

    @Page
    private BasicAuthExample basicAuthExample;

    @Deployment(name = BasicAuthExample.DEPLOYMENT_NAME)
    private static WebArchive customerPortalExample() throws IOException {
        return exampleDeployment("examples-basicauth");
    }

    @Override
    public void loadAdapterTestRealmsTo(List<RealmRepresentation> testRealms) {
        super.loadAdapterTestRealmsTo(testRealms);
        File testRealmFile = new File(EXAMPLES_HOME + "/keycloak-examples-" + EXAMPLES_VERSION_SUFFIX
                + "/basic-auth/basicauthrealm.json");
        try {
            testRealms.add(loadRealm(new FileInputStream(testRealmFile)));
        } catch (FileNotFoundException ex) {
            throw new IllegalStateException("Test realm file not found: " + testRealmFile);
        }
    }

    @Test
    public void testBasicAuthExample() {
        String value = "hello";
        Client client = ClientBuilder.newClient();

        Response response = client.target(basicAuthExample
                .setTemplateValues("admin", "password", value).getUri()).request().get();
        assertEquals(200, response.getStatus());
        assertEquals(value, response.readEntity(String.class));
        response.close();

        response = client.target(basicAuthExample
                .setTemplateValues("invalid-user", "password", value).getUri()).request().get();
        assertEquals(401, response.getStatus());
        assertTrue(response.readEntity(String.class).contains("Unauthorized"));
        response.close();

        response = client.target(basicAuthExample
                .setTemplateValues("admin", "invalid-password", value).getUri()).request().get();
        assertEquals(401, response.getStatus());
        assertTrue(response.readEntity(String.class).contains("Unauthorized"));
        response.close();

        client.close();
    }

}
