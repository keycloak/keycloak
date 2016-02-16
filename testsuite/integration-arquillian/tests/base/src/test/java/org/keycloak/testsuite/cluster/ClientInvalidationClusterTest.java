package org.keycloak.testsuite.cluster;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;
import static org.junit.Assert.assertNull;
import org.junit.Before;
import org.keycloak.admin.client.resource.ClientsResource;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.arquillian.ContainerInfo;

/**
 *
 * @author tkyjovsk
 */
public class ClientInvalidationClusterTest extends AbstractInvalidationClusterTestWithTestRealm<ClientRepresentation> {

    @Before
    public void setExcludedComparisonFields() {
        excludedComparisonFields.add("protocolMappers");
    }

    @Override
    protected ClientRepresentation createTestEntityRepresentation() {
        ClientRepresentation client = new ClientRepresentation();
        String s = randomString(5);
        client.setClientId("client_" + s);
        client.setName("name_" + s);
        return client;
    }

    protected ClientsResource clients(ContainerInfo node) {
        return getAdminClientFor(node).realm(testRealmName).clients();
    }

    @Override
    protected ClientRepresentation createEntity(ClientRepresentation client, ContainerInfo node) {
        Response response = clients(node).create(client);
        String id = ApiUtil.getCreatedId(response);
        response.close();
        client.setId(id);
        return readEntity(client, node);
    }

    @Override
    protected ClientRepresentation readEntity(ClientRepresentation client, ContainerInfo node) {
        ClientRepresentation u = null;
        try {
            u = clients(node).get(client.getId()).toRepresentation();
        } catch (NotFoundException nfe) {
            // exoected when client doesn't exist
        }
        return u;
    }

    @Override
    protected ClientRepresentation updateEntity(ClientRepresentation client, ContainerInfo node) {
        clients(node).get(client.getId()).update(client);
        return readEntity(client, node);
    }

    @Override
    protected void deleteEntity(ClientRepresentation client, ContainerInfo node) {
        clients(node).get(client.getId()).remove();
        assertNull(readEntity(client, node));
    }

    @Override
    protected ClientRepresentation testEntityUpdates(ClientRepresentation client, boolean backendFailover) {

        // clientId
        client.setClientId(client.getClientId() + "_updated");
        client = updateEntity(client, getCurrentFailNode());
        verifyEntityUpdateDuringFailover(client, backendFailover);

        // name
        client.setName(client.getName() + "_updated");
        client = updateEntity(client, getCurrentFailNode());
        verifyEntityUpdateDuringFailover(client, backendFailover);

        return client;
    }

}
