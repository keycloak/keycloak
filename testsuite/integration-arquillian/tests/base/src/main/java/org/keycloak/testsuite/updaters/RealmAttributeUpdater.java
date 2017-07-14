package org.keycloak.testsuite.updaters;

import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.RealmRepresentation;
import java.io.Closeable;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author hmlnarik
 */
public class RealmAttributeUpdater {

    private final Map<String, String> originalAttributes = new HashMap<>();

    private final RealmResource realmResource;

    private final RealmRepresentation rep;

    public RealmAttributeUpdater(RealmResource realmResource) {
        this.realmResource = realmResource;
        this.rep = realmResource.toRepresentation();
        if (this.rep.getAttributes() == null) {
            this.rep.setAttributes(new HashMap<>());
        }
    }

    public RealmAttributeUpdater setAttribute(String name, String value) {
        if (! originalAttributes.containsKey(name)) {
            this.originalAttributes.put(name, this.rep.getAttributes().put(name, value));
        } else {
            this.rep.getAttributes().put(name, value);
        }
        return this;
    }

    public RealmAttributeUpdater removeAttribute(String name) {
        if (! originalAttributes.containsKey(name)) {
            this.originalAttributes.put(name, this.rep.getAttributes().put(name, null));
        } else {
            this.rep.getAttributes().put(name, null);
        }
        return this;
    }

    public Closeable update() {
        realmResource.update(rep);

        return () -> {
            rep.getAttributes().putAll(originalAttributes);
            realmResource.update(rep);
        };
    }
}
