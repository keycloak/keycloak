package org.keycloak.testsuite.cluster;

import org.junit.Before;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.testsuite.arquillian.ContainerInfo;
import static org.keycloak.testsuite.util.WaitUtils.pause;

/**
 *
 * @author tkyjovsk
 */
public abstract class AbstractTwoNodeClusterTest extends AbstractClusterTest {

    @Before
    public void beforeTwoNodeClusterTest() {
        startBackendNodes(2);
        pause(3000);
    }

    protected ContainerInfo backend1Info() {
        return backendNode(0);
    }

    protected ContainerInfo backend2Info() {
        return backendNode(1);
    }

    protected Keycloak backend1AdminClient() {
        return backendAdminClients.get(0);
    }

    protected Keycloak backend2AdminClient() {
        return backendAdminClients.get(1);
    }

    protected void startBackend1() {
        startBackendNode(0);
    }

    protected void startBackend2() {
        startBackendNode(1);
    }

    protected void failback() {
        startBackend1();
        startBackend2();
    }

    protected void killBackend1() {
        killBackendNode(0);
    }

    protected void killBackend2() {
        killBackendNode(1);
    }

}
