package org.keycloak.tests.cluster;

import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.testframework.remote.providers.runonserver.FetchOnServer;
import org.keycloak.tests.providers.components.TestComponentProvider;
import org.keycloak.tests.providers.components.TestComponentProviderFactory;

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
                    .map(componentModel -> createProvider(session, componentModel).getDetails())
                    .orElse(null);
        }

        private TestComponentProvider createProvider(KeycloakSession session, ComponentModel componentModel) {
            ProviderFactory<TestComponentProvider> factory = session.getKeycloakSessionFactory()
                    .getProviderFactory(TestComponentProvider.class, componentModel.getProviderId());
            TestComponentProviderFactory componentFactory = (TestComponentProviderFactory) factory;
            return (TestComponentProvider) componentFactory.create(session, componentModel);
        }
    }
}
