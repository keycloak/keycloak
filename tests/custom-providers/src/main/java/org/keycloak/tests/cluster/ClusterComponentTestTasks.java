package org.keycloak.tests.cluster;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.testframework.remote.providers.runonserver.FetchOnServer;
import org.keycloak.tests.providers.components.TestComponentProvider;

public final class ClusterComponentTestTasks {

    private ClusterComponentTestTasks() {
    }

    public static final class ComponentProviderDetails implements FetchOnServer {
        private final String realmName;
        private final String componentName;

        public ComponentProviderDetails(String realmName, String componentName) {
            this.realmName = realmName;
            this.componentName = componentName;
        }

        @Override
        public Object run(KeycloakSession session) {
            RealmModel realm = session.realms().getRealmByName(realmName);
            session.getContext().setRealm(realm);
            return realm.getComponentsStream(realm.getId(), TestComponentProvider.class.getName())
                    .filter(componentModel -> componentName.equals(componentModel.getName()))
                    .findFirst()
                    .map(componentModel -> {
                        TestComponentProvider provider = session.getComponentProvider(TestComponentProvider.class, componentModel.getId());
                        return provider == null ? null : provider.getDetails();
                    })
                    .orElse(null);
        }
    }
}
