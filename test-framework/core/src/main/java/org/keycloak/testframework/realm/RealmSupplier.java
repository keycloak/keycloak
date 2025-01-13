package org.keycloak.testframework.realm;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.injection.InstanceContext;
import org.keycloak.testframework.injection.Registry;
import org.keycloak.testframework.injection.RequestedInstance;
import org.keycloak.testframework.injection.Supplier;
import org.keycloak.testframework.injection.SupplierHelpers;
import org.keycloak.testframework.injection.SupplierOrder;
import org.keycloak.testframework.injection.AbstractInterceptorHelper;
import org.keycloak.testframework.server.KeycloakServer;

public class RealmSupplier implements Supplier<ManagedRealm, InjectRealm> {

    private static final String REALM_NAME_KEY = "realmName";

    @Override
    public Class<InjectRealm> getAnnotationClass() {
        return InjectRealm.class;
    }

    @Override
    public Class<ManagedRealm> getValueType() {
        return ManagedRealm.class;
    }

    @Override
    public ManagedRealm getValue(InstanceContext<ManagedRealm, InjectRealm> instanceContext) {
        KeycloakServer server = instanceContext.getDependency(KeycloakServer.class);
        Keycloak adminClient = instanceContext.getDependency(Keycloak.class, "bootstrap-client");

        RealmConfig config = SupplierHelpers.getInstance(instanceContext.getAnnotation().config());

        RealmConfigBuilder realmConfigBuilder = config.configure(RealmConfigBuilder.create());

        RealmConfigInterceptorHelper interceptor = new RealmConfigInterceptorHelper(instanceContext.getRegistry());
        realmConfigBuilder = interceptor.intercept(realmConfigBuilder, instanceContext);

        RealmRepresentation realmRepresentation = realmConfigBuilder.build();

        if (realmRepresentation.getRealm() == null) {
            String realmName = SupplierHelpers.createName(instanceContext);
            realmRepresentation.setRealm(realmName);
        }

        if (realmRepresentation.getId() == null) {
            realmRepresentation.setId(realmRepresentation.getRealm());
        }

        String realmName = realmRepresentation.getRealm();
        instanceContext.addNote(REALM_NAME_KEY, realmName);

        if (instanceContext.getAnnotation().createRealm()) {
            adminClient.realms().create(realmRepresentation);
        }

        // TODO Token needs to be invalidated after creating realm to have roles for new realm in the token. Maybe lightweight access tokens could help.
        adminClient.tokenManager().invalidate(adminClient.tokenManager().getAccessTokenString());

        RealmResource realmResource = adminClient.realm(realmRepresentation.getRealm());
        return new ManagedRealm(server.getBaseUrl() + "/realms/" + realmName, realmRepresentation, realmResource);
    }

    @Override
    public boolean compatible(InstanceContext<ManagedRealm, InjectRealm> a, RequestedInstance<ManagedRealm, InjectRealm> b) {
        if (!a.getAnnotation().config().equals(b.getAnnotation().config())) {
            return false;
        }

        RealmConfigInterceptorHelper interceptor = new RealmConfigInterceptorHelper(a.getRegistry());
        return interceptor.sameInterceptors(a);
    }

    @Override
    public void close(InstanceContext<ManagedRealm, InjectRealm> instanceContext) {
        if (instanceContext.getAnnotation().createRealm()) {
            instanceContext.getValue().admin().remove();
        }
    }

    @Override
    public int order() {
        return SupplierOrder.REALM;
    }

    private static class RealmConfigInterceptorHelper extends AbstractInterceptorHelper<RealmConfigInterceptor, RealmConfigBuilder> {

        private RealmConfigInterceptorHelper(Registry registry) {
            super(registry, RealmConfigInterceptor.class);
        }

        @Override
        public RealmConfigBuilder intercept(RealmConfigBuilder value, Supplier<?, ?> supplier, InstanceContext<?, ?> existingInstance) {
            if (supplier instanceof RealmConfigInterceptor interceptor) {
                value = interceptor.intercept(value, existingInstance);
            }
            return value;
        }

    }

}
