package org.keycloak.testsuite.updaters;

import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.models.UserModel;
import org.keycloak.representations.idm.UserRepresentation;
import java.io.Closeable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

/**
 *
 * @author hmlnarik
 */
public class UserAttributeUpdater {

    private final UserResource userResource;

    private final UserRepresentation rep;
    private final UserRepresentation origRep;

    public UserAttributeUpdater(UserResource userResource) {
        this.userResource = userResource;
        this.origRep = userResource.toRepresentation();
        this.rep = userResource.toRepresentation();
        if (this.rep.getAttributes() == null) {
            this.rep.setAttributes(new HashMap<>());
        }
    }

    public UserAttributeUpdater setAttribute(String name, List<String> value) {
        this.rep.getAttributes().put(name, value);
        return this;
    }

    public UserAttributeUpdater setAttribute(String name, String... values) {
        this.rep.getAttributes().put(name, Arrays.asList(values));
        return this;
    }

    public UserAttributeUpdater removeAttribute(String name) {
        this.rep.getAttributes().put(name, null);
        return this;
    }

    public UserAttributeUpdater setEmailVerified(Boolean emailVerified) {
        rep.setEmailVerified(emailVerified);
        return this;
    }

    public Closeable update() {
        userResource.update(rep);

        return () -> userResource.update(origRep);
    }

    public UserAttributeUpdater setRequiredActions(UserModel.RequiredAction... requiredAction) {
        rep.setRequiredActions(Arrays.stream(requiredAction)
                .map(action -> action.name())
                .collect(Collectors.toList())
        );
        return this;
    }
}
