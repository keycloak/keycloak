package org.keycloak.test.framework.admin;

import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.test.framework.TestFrameworkException;
import org.keycloak.test.framework.annotations.InjectAdminClient;
import org.keycloak.test.framework.config.Config;
import org.keycloak.test.framework.injection.InstanceContext;
import org.keycloak.test.framework.injection.LifeCycle;
import org.keycloak.test.framework.injection.RequestedInstance;
import org.keycloak.test.framework.injection.Supplier;
import org.keycloak.test.framework.realm.ManagedRealm;
import org.keycloak.test.framework.realm.ManagedUser;
import org.keycloak.test.framework.server.KeycloakServer;

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

        KeycloakServer server = instanceContext.getDependency(KeycloakServer.class);
        KeycloakBuilder clientBuilder = KeycloakBuilder.builder()
                .serverUrl(server.getBaseUrl())
                .grantType(OAuth2Constants.CLIENT_CREDENTIALS);

        if (mode.equals(InjectAdminClient.Mode.BOOTSTRAP)) {
            clientBuilder.realm("master").clientId(Config.getAdminClientId()).clientSecret(Config.getAdminClientSecret());
        } else if (mode.equals(InjectAdminClient.Mode.MANAGED_REALM)) {
            ManagedRealm managedRealm = instanceContext.getDependency(ManagedRealm.class);
            clientBuilder.realm(managedRealm.getName());

            String clientId = !annotation.client().isEmpty() ? annotation.client() : null;
            String userId = !annotation.user().isEmpty() ? annotation.user() : null;

            if (clientId == null) {
                throw new TestFrameworkException("Client is required when using managed realm mode");
            }

            RealmRepresentation realmRep = managedRealm.getCreatedRepresentation();
            ClientRepresentation clientRep = realmRep.getClients().stream()
                    .filter(c -> c.getClientId().equals(annotation.client()))
                    .findFirst().orElseThrow(() -> new TestFrameworkException("Client " + annotation.client() + " not found in managed realm"));

            clientBuilder.clientId(clientId).clientSecret(clientRep.getSecret());

            if (userId != null) {
                UserRepresentation userRep = realmRep.getUsers().stream()
                        .filter(u -> u.getUsername().equals(annotation.user()))
                        .findFirst().orElseThrow(() -> new TestFrameworkException("User " + annotation.user() + " not found in managed realm"));
                String password = ManagedUser.getPassword(userRep);
                clientBuilder.username(userRep.getUsername()).password(password);
                clientBuilder.grantType(OAuth2Constants.PASSWORD);
            }
        }

        return clientBuilder.build();
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
