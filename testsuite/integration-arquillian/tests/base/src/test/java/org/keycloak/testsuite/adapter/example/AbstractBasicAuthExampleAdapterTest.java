package org.keycloak.testsuite.adapter.example;

import org.keycloak.testsuite.adapter.AbstractExampleAdapterTest;
import java.io.File;
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
import static org.keycloak.testsuite.util.IOUtil.loadRealm;
import org.keycloak.testsuite.adapter.page.BasicAuthExample;
import static org.keycloak.testsuite.auth.page.AuthRealm.EXAMPLE;

public abstract class AbstractBasicAuthExampleAdapterTest extends AbstractExampleAdapterTest {

    @Page
    private BasicAuthExample basicAuthExample;

    @Deployment(name = BasicAuthExample.DEPLOYMENT_NAME)
    private static WebArchive basicAuthExample() throws IOException {
        return exampleDeployment("examples-basicauth");
    }

    @Override
    public void addAdapterTestRealms(List<RealmRepresentation> testRealms) {
        testRealms.add(loadRealm(new File(EXAMPLES_HOME_DIR + "/basic-auth/basicauthrealm.json")));
    }

    @Override
    public void setDefaultPageUriParameters() {
        super.setDefaultPageUriParameters();
        testRealmPage.setAuthRealm(EXAMPLE);
    }

    @Test
    public void testBasicAuthExample() {
        String value = "hello";
        Client client = ClientBuilder.newClient();

        Response response = client.target(basicAuthExample
                .setTemplateValues("admin", "password", value).buildUri()).request().get();
        assertEquals(200, response.getStatus());
        assertEquals(value, response.readEntity(String.class));
        response.close();

        response = client.target(basicAuthExample
                .setTemplateValues("invalid-user", "password", value).buildUri()).request().get();
        assertEquals(401, response.getStatus());
        assertTrue(response.readEntity(String.class).contains("Unauthorized"));
        response.close();

        response = client.target(basicAuthExample
                .setTemplateValues("admin", "invalid-password", value).buildUri()).request().get();
        assertEquals(401, response.getStatus());
        assertTrue(response.readEntity(String.class).contains("Unauthorized"));
        response.close();

        client.close();
    }

}
