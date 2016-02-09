package org.keycloak.testsuite.cluster;

import java.util.ArrayList;
import java.util.List;
import org.jboss.arquillian.container.test.api.ContainerController;
import org.jboss.arquillian.graphene.page.Page;
import org.jboss.arquillian.test.api.ArquillianResource;
import static org.junit.Assert.assertTrue;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.models.Constants;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.AbstractKeycloakTest;
import org.keycloak.testsuite.arquillian.ContainerInfo;
import org.keycloak.testsuite.auth.page.AuthRealm;
import static org.keycloak.testsuite.auth.page.AuthRealm.ADMIN;
import static org.keycloak.testsuite.auth.page.AuthRealm.MASTER;

/**
 *
 * @author tkyjovsk
 */
public abstract class AbstractClusterTest extends AbstractKeycloakTest {

    @ArquillianResource
    protected ContainerController controller;

    protected List<Keycloak> backendAdminClients = new ArrayList<>();

    public void startBackendNodes(int count) {
        if (count < 0 || count > 10) {
            throw new IllegalArgumentException();
        }
        assertTrue(suiteContext.getAuthServerBackendsInfo().size() >= count);
        for (int i = 0; i < count; i++) {

            ContainerInfo backendNode = suiteContext.getAuthServerBackendsInfo().get(i);

            controller.start(backendNode.getQualifier());
            assertTrue(controller.isStarted(backendNode.getQualifier()));

            backendAdminClients.add(createAdminClientFor(backendNode));
        }
    }

    protected Keycloak createAdminClientFor(ContainerInfo backendNode) {
        log.info("Initializing admin client for " + backendNode.getContextRoot() + "/auth");
        return Keycloak.getInstance(backendNode.getContextRoot() + "/auth",
                MASTER, ADMIN, ADMIN, Constants.ADMIN_CLI_CLIENT_ID);
    }

    protected ContainerInfo backendNode(int i) {
        return suiteContext.getAuthServerBackendsInfo().get(i);
    }

    protected void startBackendNode(int i) {
        String container = backendNode(i).getQualifier();
        if (!controller.isStarted(container)) {
            controller.start(container);
            backendAdminClients.set(i, createAdminClientFor(backendNode(i)));
        }
    }

    protected void killBackendNode(int i) {
        backendAdminClients.get(i).close();
        controller.kill(backendNode(i).getQualifier());
    }

    protected void listRealms(int i) {
        log.info(String.format("Node %s: AccessTokenString: %s", i + 1, backendAdminClients.get(i).tokenManager().getAccessTokenString()));
        for (RealmRepresentation r : backendAdminClients.get(i).realms().findAll()) {
            log.info(String.format("Node %s: Realm: %s, Id: %s", i + 1, r.getRealm(), r.getId()));
        }
    }
    
}
