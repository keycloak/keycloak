package org.keycloak.testsuite.updaters;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.ClientsResource;
import org.keycloak.representations.idm.ClientRepresentation;
import java.io.Closeable;
import java.util.HashMap;
import java.util.List;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;

/**
 *
 * @author hmlnarik
 */
public class ClientAttributeUpdater {

    private final ClientResource clientResource;

    private final ClientRepresentation rep;
    private final ClientRepresentation origRep;

    public static ClientAttributeUpdater forClient(Keycloak adminClient, String realm, String clientId) {
        ClientsResource clients = adminClient.realm(realm).clients();
        List<ClientRepresentation> foundClients = clients.findByClientId(clientId);
        assertThat(foundClients, hasSize(1));
        ClientResource clientRes = clients.get(foundClients.get(0).getId());
        
        return new ClientAttributeUpdater(clientRes);
    }

    public ClientAttributeUpdater(ClientResource clientResource) {
        this.clientResource = clientResource;
        this.origRep = clientResource.toRepresentation();
        this.rep = clientResource.toRepresentation();
        if (this.rep.getAttributes() == null) {
            this.rep.setAttributes(new HashMap<>());
        }
    }

    public ClientAttributeUpdater setClientId(String clientId) {
        this.rep.setClientId(clientId);
        return this;
    }

    public ClientAttributeUpdater setAttribute(String name, String value) {
        this.rep.getAttributes().put(name, value);
        return this;
    }

    public ClientAttributeUpdater removeAttribute(String name) {
        this.rep.getAttributes().remove(name);
        return this;
    }

    public ClientAttributeUpdater setConsentRequired(Boolean consentRequired) {
        rep.setConsentRequired(consentRequired);
        return this;
    }

    public ClientAttributeUpdater setFrontchannelLogout(Boolean frontchannelLogout) {
        rep.setFrontchannelLogout(frontchannelLogout);
        return this;
    }

    public Closeable update() {
        clientResource.update(rep);

        return () -> clientResource.update(origRep);
    }
}
