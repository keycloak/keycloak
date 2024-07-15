package org.keycloak.test.framework.realm;

import jakarta.ws.rs.core.Response;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.test.framework.TestUser;
import org.keycloak.test.framework.injection.InstanceWrapper;
import org.keycloak.test.framework.injection.LifeCycle;
import org.keycloak.test.framework.injection.Registry;
import org.keycloak.test.framework.injection.Supplier;
import org.keycloak.test.framework.injection.SupplierHelpers;

public class UserSupplier implements Supplier<UserResource, TestUser> {

    private static final String USER_UUID_KEY = "userUuid";

    @Override
    public Class<TestUser> getAnnotationClass() {
        return TestUser.class;
    }

    @Override
    public Class<UserResource> getValueType() {
        return UserResource.class;
    }

    @Override
    public InstanceWrapper<UserResource, TestUser> getValue(Registry registry, TestUser annotation) {
        InstanceWrapper<UserResource, TestUser> wrapper = new InstanceWrapper<>(this, annotation);
        LifeCycle lifecycle = annotation.lifecycle();

        RealmResource realm = registry.getDependency(RealmResource.class, wrapper);

        UserConfig config = SupplierHelpers.getInstance(annotation.config());
        UserRepresentation userRepresentation = config.getRepresentation();

        if (userRepresentation.getUsername() == null) {
            String username = lifecycle.equals(LifeCycle.GLOBAL) ? config.getClass().getSimpleName() : registry.getCurrentContext().getRequiredTestClass().getSimpleName();
            userRepresentation.setUsername(username);
        }

        Response response = realm.users().create(userRepresentation);

        String path = response.getLocation().getPath();
        String userId = path.substring(path.lastIndexOf('/') + 1);

        response.close();

        wrapper.addNote(USER_UUID_KEY, userId);

        UserResource userResource = realm.users().get(userId);
        wrapper.setValue(userResource, lifecycle);

        return wrapper;
    }

    @Override
    public boolean compatible(InstanceWrapper<UserResource, TestUser> a, InstanceWrapper<UserResource, TestUser> b) {
        return a.getAnnotation().config().equals(b.getAnnotation().config()) &&
                a.getNote(USER_UUID_KEY, String.class).equals(b.getNote(USER_UUID_KEY, String.class));
    }

    @Override
    public void close(UserResource user) {
        user.remove();
    }

}
