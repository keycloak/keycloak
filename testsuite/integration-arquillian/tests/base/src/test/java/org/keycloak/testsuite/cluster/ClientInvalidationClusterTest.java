package org.keycloak.testsuite.cluster;

import org.apache.commons.lang.RandomStringUtils;
import org.junit.Before;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.ClientsResource;
import org.keycloak.common.util.Retry;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.arquillian.ContainerInfo;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;

import static org.junit.Assert.assertNull;

/**
 *
 * @author tkyjovsk
 */
public class ClientInvalidationClusterTest extends AbstractInvalidationClusterTestWithTestRealm<ClientRepresentation, ClientResource> {

    @Before
    public void setExcludedComparisonFields() {
        excludedComparisonFields.add("protocolMappers");
    }

    @Override
    protected ClientRepresentation createTestEntityRepresentation() {
        ClientRepresentation client = new ClientRepresentation();
        String s = RandomStringUtils.randomAlphabetic(5);
        client.setClientId("client_" + s);
        client.setName("name_" + s);
        return client;
    }

    protected ClientsResource clients(ContainerInfo node) {
        return getAdminClientFor(node).realm(testRealmName).clients();
    }

    @Override
    protected ClientResource entityResource(ClientRepresentation client, ContainerInfo node) {
        return entityResource(client.getId(), node);
    }

    @Override
    protected ClientResource entityResource(String id, ContainerInfo node) {
        return clients(node).get(id);
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
        ClientRepresentation u = Retry.call(new Retry.Supplier<ClientRepresentation>() {
            @Override
            public ClientRepresentation get(int iteration) {
                try {
                    return entityResource(client, node).toRepresentation();
                } catch (NotFoundException nfe) {
                    return null;
                }
            }
        }, 3, 5000);
        return u;
    }

    @Override
    protected ClientRepresentation updateEntity(ClientRepresentation client, ContainerInfo node) {
        Retry.execute(new Runnable() {
            @Override
            public void run() {
                entityResource(client, node).update(client);
            }
        }, 3, 5000);
        return readEntity(client, node);
    }

    @Override
    protected void deleteEntity(ClientRepresentation client, ContainerInfo node) {
        Retry.execute(new Runnable() {
            @Override
            public void run() {
                entityResource(client, node).remove();
            }
        }, 3, 5000);
        assertNull(readEntity(client, node));
    }

    @Override
    protected ClientRepresentation testEntityUpdates(ClientRepresentation client, boolean backendFailover) {

        // clientId
        client.setClientId(client.getClientId() + "_updated");
        client = updateEntityOnCurrentFailNode(client, "clientId");
        verifyEntityUpdateDuringFailover(client, backendFailover);

        // name
        client.setName(client.getName() + "_updated");
        client = updateEntityOnCurrentFailNode(client, "name");
        verifyEntityUpdateDuringFailover(client, backendFailover);

        return client;
    }

}
