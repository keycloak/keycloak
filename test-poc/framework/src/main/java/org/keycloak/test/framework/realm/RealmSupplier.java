package org.keycloak.test.framework.realm;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.test.framework.annotations.InjectRealm;
import org.keycloak.test.framework.injection.InstanceContext;
import org.keycloak.test.framework.injection.RequestedInstance;
import org.keycloak.test.framework.injection.Supplier;
import org.keycloak.test.framework.injection.SupplierHelpers;

public class RealmSupplier implements Supplier<RealmResource, InjectRealm> {

    private static final String REALM_NAME_KEY = "realmName";

    @Override
    public Class<InjectRealm> getAnnotationClass() {
        return InjectRealm.class;
    }

    @Override
    public Class<RealmResource> getValueType() {
        return RealmResource.class;
    }

    @Override
    public RealmResource getValue(InstanceContext<RealmResource, InjectRealm> instanceContext) {
        Keycloak adminClient = instanceContext.getDependency(Keycloak.class);

        RealmConfig config = SupplierHelpers.getInstance(instanceContext.getAnnotation().config());
        RealmRepresentation realmRepresentation = config.getRepresentation();

        if (realmRepresentation.getRealm() == null) {
            String realmName = instanceContext.getRef();
            realmRepresentation.setRealm(realmName);
        }

        String realmName = realmRepresentation.getRealm();
        instanceContext.addNote(REALM_NAME_KEY, realmName);

        adminClient.realms().create(realmRepresentation);

        // TODO Token needs to be invalidated after creating realm to have roles for new realm in the token. Maybe lightweight access tokens could help.
        adminClient.tokenManager().invalidate(adminClient.tokenManager().getAccessTokenString());

        return adminClient.realm(realmRepresentation.getRealm());
    }

    @Override
    public boolean compatible(InstanceContext<RealmResource, InjectRealm> a, RequestedInstance<RealmResource, InjectRealm> b) {
        return a.getAnnotation().config().equals(b.getAnnotation().config());
    }

    @Override
    public void close(InstanceContext<RealmResource, InjectRealm> instanceContext) {
        instanceContext.getValue().remove();
    }

}
