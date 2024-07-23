package org.keycloak.test.framework.realm;

import jakarta.ws.rs.core.Response;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.test.framework.annotations.User;
import org.keycloak.test.framework.injection.InstanceContext;
import org.keycloak.test.framework.injection.RequestedInstance;
import org.keycloak.test.framework.injection.Supplier;
import org.keycloak.test.framework.injection.SupplierHelpers;

public class UserSupplier implements Supplier<UserResource, User> {

    private static final String USER_UUID_KEY = "userUuid";

    @Override
    public Class<User> getAnnotationClass() {
        return User.class;
    }

    @Override
    public Class<UserResource> getValueType() {
        return UserResource.class;
    }

    @Override
    public UserResource getValue(InstanceContext<UserResource, User> instanceContext) {
        RealmResource realm = instanceContext.getDependency(RealmResource.class);

        UserConfig config = SupplierHelpers.getInstance(instanceContext.getAnnotation().config());
        UserRepresentation userRepresentation = config.getRepresentation();

        if (userRepresentation.getUsername() == null) {
            String username = instanceContext.getRef();
            userRepresentation.setUsername(username);
        }

        Response response = realm.users().create(userRepresentation);

        String path = response.getLocation().getPath();
        String userId = path.substring(path.lastIndexOf('/') + 1);

        response.close();

        instanceContext.addNote(USER_UUID_KEY, userId);

        return realm.users().get(userId);
    }

    @Override
    public boolean compatible(InstanceContext<UserResource, User> a, RequestedInstance<UserResource, User> b) {
        return a.getAnnotation().config().equals(b.getAnnotation().config());
    }

    @Override
    public void close(InstanceContext<UserResource, User> instanceContext) {
        instanceContext.getValue().remove();
    }

}
