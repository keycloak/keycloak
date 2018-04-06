package org.keycloak.testsuite.updaters;

import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.representations.idm.ClientRepresentation;
import java.io.Closeable;
import java.util.HashMap;

/**
 *
 * @author hmlnarik
 */
public class ClientAttributeUpdater {

    private final ClientResource clientResource;

    private final ClientRepresentation rep;
    private final ClientRepresentation origRep;

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
