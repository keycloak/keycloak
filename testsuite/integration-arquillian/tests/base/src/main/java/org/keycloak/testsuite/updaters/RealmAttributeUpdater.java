package org.keycloak.testsuite.updaters;

import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.RealmRepresentation;
import java.io.Closeable;
import java.util.HashMap;
import java.util.function.Consumer;

/**
 *
 * @author hmlnarik
 */
public class RealmAttributeUpdater {

    private final RealmResource realmResource;

    private final RealmRepresentation rep;
    private final RealmRepresentation origRep;

    public RealmAttributeUpdater(RealmResource realmResource) {
        this.realmResource = realmResource;
        this.origRep = realmResource.toRepresentation();
        this.rep = realmResource.toRepresentation();
        if (this.rep.getAttributes() == null) {
            this.rep.setAttributes(new HashMap<>());
        }
    }

    public RealmAttributeUpdater updateWith(Consumer<RealmRepresentation> updater) {
        updater.accept(this.rep);
        return this;
    }

    public RealmAttributeUpdater setAttribute(String name, String value) {
        this.rep.getAttributes().put(name, value);
        return this;
    }

    public RealmAttributeUpdater removeAttribute(String name) {
        this.rep.getAttributes().put(name, null);
        return this;
    }

    public Closeable update() {
        realmResource.update(rep);

        return () -> realmResource.update(origRep);
    }
}
