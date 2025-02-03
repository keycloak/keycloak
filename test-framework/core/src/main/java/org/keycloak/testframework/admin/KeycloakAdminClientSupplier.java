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
            String realm = StringUtil.convertEmptyToNull(annotation.realm());

            if (realm == null) {
                throw new TestFrameworkException("Realm is required when using managed realm mode");
            }

            String clientId = StringUtil.convertEmptyToNull(annotation.clientId());
            String clientSecret = StringUtil.convertEmptyToNull(annotation.clientSecret());

            if (clientId == null || clientSecret == null) {
                throw new TestFrameworkException("Client is required when using managed realm mode");
            }

            String username = StringUtil.convertEmptyToNull(annotation.username());
            String password = StringUtil.convertEmptyToNull(annotation.password());

            if (username == null && password == null) {
                return adminClientFactory.create(realm, clientId, clientSecret, false);
            } else if (username != null && password != null) {
                return adminClientFactory.create(realm, clientId, clientSecret, username, password, false);
            } else {
                throw new TestFrameworkException("Both username and password are required");
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
