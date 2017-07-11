package org.keycloak.testsuite.updaters;

import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.representations.idm.ClientRepresentation;
import java.io.Closeable;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author hmlnarik
 */
public class ClientAttributeUpdater {

    private final Map<String, String> originalAttributes = new HashMap<>();

    private final ClientResource clientResource;

    private final ClientRepresentation rep;

    public ClientAttributeUpdater(ClientResource clientResource) {
        this.clientResource = clientResource;
        this.rep = clientResource.toRepresentation();
        if (this.rep.getAttributes() == null) {
            this.rep.setAttributes(new HashMap<>());
        }
    }

    public ClientAttributeUpdater setAttribute(String name, String value) {
        if (! originalAttributes.containsKey(name)) {
            this.originalAttributes.put(name, this.rep.getAttributes().put(name, value));
        } else {
            this.rep.getAttributes().put(name, value);
        }
        return this;
    }

    public ClientAttributeUpdater removeAttribute(String name) {
        if (! originalAttributes.containsKey(name)) {
            this.originalAttributes.put(name, this.rep.getAttributes().put(name, null));
        } else {
            this.rep.getAttributes().put(name, null);
        }
        return this;
    }

    public Closeable update() {
        clientResource.update(rep);

        return () -> {
            rep.getAttributes().putAll(originalAttributes);
            clientResource.update(rep);
        };
    }
}
