package org.keycloak.testframework.realm;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.injection.AbstractInterceptorHelper;
import org.keycloak.testframework.injection.DependenciesBuilder;
import org.keycloak.testframework.injection.Dependency;
import org.keycloak.testframework.injection.InstanceContext;
import org.keycloak.testframework.injection.Registry;
import org.keycloak.testframework.injection.RequestedInstance;
import org.keycloak.testframework.injection.Supplier;
import org.keycloak.testframework.injection.SupplierHelpers;
import org.keycloak.testframework.injection.SupplierOrder;
import org.keycloak.testframework.server.KeycloakServer;
import org.keycloak.testframework.server.KeycloakUrls;
import org.keycloak.util.JsonSerialization;
import org.keycloak.util.Strings;

public class RealmSupplier implements Supplier<ManagedRealm, InjectRealm> {

    @Override
    public List<Dependency> getDependencies(RequestedInstance<ManagedRealm, InjectRealm> instanceContext) {
        return DependenciesBuilder.create(KeycloakServer.class)
                .add(KeycloakUrls.class)
                .add(Keycloak.class, "bootstrap-client").build();
    }

    @Override
    public ManagedRealm getValue(InstanceContext<ManagedRealm, InjectRealm> instanceContext) {
        KeycloakServer server = instanceContext.getDependency(KeycloakServer.class);
        Keycloak adminClient = instanceContext.getDependency(Keycloak.class, "bootstrap-client");

        String attachTo = instanceContext.getAnnotation().attachTo();
        boolean managed = attachTo.isEmpty();

        RealmRepresentation realmRepresentation;

        if (managed) {
            RealmConfigBuilder realmConfigBuilder;
            if (!Strings.isEmpty(instanceContext.getAnnotation().fromJson())) {
                try {
                    InputStream jsonStream = instanceContext.getRegistry().getCurrentContext().getRequiredTestClass().getResourceAsStream(instanceContext.getAnnotation().fromJson());
                    if (jsonStream == null) {
                        throw new RuntimeException("Realm JSON representation not found in classpath");
                    }
                    realmConfigBuilder = RealmConfigBuilder.update(JsonSerialization.readValue(jsonStream, RealmRepresentation.class));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            } else {
                realmConfigBuilder = RealmConfigBuilder.create();
            }

            RealmConfig config = SupplierHelpers.getInstanceWithInjectedFields(instanceContext.getAnnotation().config(), instanceContext);
            realmConfigBuilder = config.configure(realmConfigBuilder);

            RealmConfigInterceptorHelper interceptor = new RealmConfigInterceptorHelper(instanceContext.getRegistry());
            realmConfigBuilder = interceptor.intercept(realmConfigBuilder, instanceContext);

            realmRepresentation = realmConfigBuilder.build();

            if (realmRepresentation.getRealm() == null) {
                realmRepresentation.setRealm(SupplierHelpers.createName(instanceContext));
            }

            if (realmRepresentation.getId() == null) {
                realmRepresentation.setId(realmRepresentation.getRealm());
            }

            adminClient.realms().create(realmRepresentation);

            // TODO Token needs to be invalidated after creating realm to have roles for new realm in the token. Maybe lightweight access tokens could help.
            adminClient.tokenManager().invalidate(adminClient.tokenManager().getAccessTokenString());
        } else {
            realmRepresentation = adminClient.realm(attachTo).toRepresentation();
        }

        instanceContext.addNote("managed", managed);

        RealmResource realmResource = adminClient.realm(realmRepresentation.getRealm());
        return new ManagedRealm(server.getBaseUrl() + "/realms/" + realmRepresentation.getRealm(), realmRepresentation, realmResource);
    }

    @Override
    public boolean compatible(InstanceContext<ManagedRealm, InjectRealm> a, RequestedInstance<ManagedRealm, InjectRealm> b) {
        InjectRealm aa = a.getAnnotation();
        InjectRealm ba = b.getAnnotation();
        return aa.config().equals(ba.config()) && aa.fromJson().equals(ba.fromJson());
    }

    @Override
    public void close(InstanceContext<ManagedRealm, InjectRealm> instanceContext) {
        if (instanceContext.getNote("managed", Boolean.class)) {
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
