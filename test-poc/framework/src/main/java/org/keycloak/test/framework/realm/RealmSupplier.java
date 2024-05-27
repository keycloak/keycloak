package org.keycloak.test.framework.realm;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.test.framework.TestRealm;
import org.keycloak.test.framework.injection.InstanceWrapper;
import org.keycloak.test.framework.injection.LifeCycle;
import org.keycloak.test.framework.injection.Registry;
import org.keycloak.test.framework.injection.Supplier;
import org.keycloak.test.framework.injection.SupplierHelpers;

public class RealmSupplier implements Supplier<RealmResource, TestRealm> {

    private static final String REALM_NAME_KEY = "realmName";

    @Override
    public Class<TestRealm> getAnnotationClass() {
        return TestRealm.class;
    }

    @Override
    public Class<RealmResource> getValueType() {
        return RealmResource.class;
    }

    @Override
    public InstanceWrapper<RealmResource, TestRealm> getValue(Registry registry, TestRealm annotation) {
        InstanceWrapper<RealmResource, TestRealm> wrapper = new InstanceWrapper<>(this, annotation);

        Keycloak adminClient = registry.getDependency(Keycloak.class, wrapper);

        RealmConfig config = SupplierHelpers.getInstance(annotation.config());
        RealmRepresentation realmRepresentation = config.getRepresentation();

        if (realmRepresentation.getRealm() == null) {
            realmRepresentation.setRealm(registry.getCurrentContext().getRequiredTestClass().getSimpleName());
        }

        String realmName = realmRepresentation.getRealm();
        wrapper.addNote(REALM_NAME_KEY, realmName);

        adminClient.realms().create(realmRepresentation);

        RealmResource realmResource = adminClient.realm(realmRepresentation.getRealm());
        wrapper.setValue(realmResource);

        return wrapper;
    }

    @Override
    public LifeCycle getLifeCycle() {
        return LifeCycle.CLASS;
    }

    @Override
    public boolean compatible(InstanceWrapper<RealmResource, TestRealm> a, InstanceWrapper<RealmResource, TestRealm> b) {
        return a.getAnnotation().config().equals(b.getAnnotation().config()) &&
                a.getNote(REALM_NAME_KEY, String.class).equals(b.getNote(REALM_NAME_KEY, String.class));
    }

    @Override
    public void close(RealmResource realm) {
        realm.remove();
    }

}
