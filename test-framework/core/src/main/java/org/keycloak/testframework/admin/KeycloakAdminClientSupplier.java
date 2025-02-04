package org.keycloak.testframework.admin;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.testframework.TestFrameworkException;
import org.keycloak.testframework.annotations.InjectAdminClient;
import org.keycloak.testframework.config.Config;
import org.keycloak.testframework.injection.InstanceContext;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.injection.RequestedInstance;
import org.keycloak.testframework.injection.StringUtil;
import org.keycloak.testframework.injection.Supplier;
import org.keycloak.testframework.realm.ManagedClient;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.ManagedUser;

public class KeycloakAdminClientSupplier implements Supplier<Keycloak, InjectAdminClient> {

    @Override
    public Class<InjectAdminClient> getAnnotationClass() {
        return InjectAdminClient.class;
    }

    @Override
    public Class<Keycloak> getValueType() {
        return Keycloak.class;
    }

    @Override
    public Keycloak getValue(InstanceContext<Keycloak, InjectAdminClient> instanceContext) {
        InjectAdminClient annotation = instanceContext.getAnnotation();

        InjectAdminClient.Mode mode = annotation.mode();

        KeycloakAdminClientFactory adminClientFactory = instanceContext.getDependency(KeycloakAdminClientFactory.class);

        if (mode.equals(InjectAdminClient.Mode.BOOTSTRAP)) {
            return adminClientFactory.create("master", Config.getAdminClientId(), Config.getAdminClientSecret(), false);
        } else if (mode.equals(InjectAdminClient.Mode.MANAGED_REALM)) {
            String realmRef = StringUtil.convertEmptyToNull(annotation.realmRef());
            ManagedRealm realm = instanceContext.getDependency(ManagedRealm.class, realmRef);

            String clientRef = StringUtil.convertEmptyToNull(annotation.clientRef());
            if (clientRef == null) {
                throw new TestFrameworkException("Client is required when using managed realm mode");
            }
            ManagedClient client = instanceContext.getDependency(ManagedClient.class, clientRef);

            String userRef = StringUtil.convertEmptyToNull(annotation.userRef());
            if (userRef == null) {
                return adminClientFactory.create(realm.getName(), client.getClientId(), client.getSecret(), false);
            } else {
                ManagedUser user = instanceContext.getDependency(ManagedUser.class, userRef);
                return adminClientFactory.create(realm.getName(), client.getClientId(), client.getSecret(), user.getUsername(), user.getPassword(), false);
            }
        } else {
            throw new TestFrameworkException("Undefined Admin Client Mode");
        }
    }

    @Override
    public LifeCycle getDefaultLifecycle() {
        return LifeCycle.GLOBAL;
    }

    @Override
    public boolean compatible(InstanceContext<Keycloak, InjectAdminClient> a, RequestedInstance<Keycloak, InjectAdminClient> b) {
        return true;
    }

    @Override
    public void close(InstanceContext<Keycloak, InjectAdminClient> instanceContext) {
        instanceContext.getValue().close();
    }

}
