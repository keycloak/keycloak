package org.keycloak.testsuite.updaters;

import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.RealmRepresentation;
import java.util.HashMap;
import java.util.List;

/**
 * Updater for realm attributes. See {@link ServerResourceUpdater} for further details.
 * @author hmlnarik
 */
public class RealmAttributeUpdater extends ServerResourceUpdater<ServerResourceUpdater, RealmResource, RealmRepresentation> {

    public RealmAttributeUpdater(RealmResource resource) {
        super(resource, resource::toRepresentation, resource::update);
        if (this.rep.getAttributes() == null) {
            this.rep.setAttributes(new HashMap<>());
        }
    }

    public RealmAttributeUpdater setAttribute(String name, String value) {
        this.rep.getAttributes().put(name, value);
        return this;
    }

    public RealmAttributeUpdater removeAttribute(String name) {
        this.rep.getAttributes().put(name, null);
        return this;
    }

    public RealmAttributeUpdater setPublicKey(String key) {
        this.rep.setPublicKey(key);
        return this;
    }

    public RealmAttributeUpdater setPrivateKey(String key) {
        this.rep.setPrivateKey(key);
        return this;
    }

    public RealmAttributeUpdater setDefaultDefaultClientScopes(List<String> defaultClientScopes) {
        rep.setDefaultDefaultClientScopes(defaultClientScopes);
        return this;
    }

}
