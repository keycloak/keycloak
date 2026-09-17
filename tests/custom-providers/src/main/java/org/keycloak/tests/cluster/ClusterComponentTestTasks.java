package org.keycloak.tests.cluster;

import java.util.Map;
import java.util.stream.Collectors;

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
    }

    public static final class AllComponentDetails implements FetchOnServer {
        private final String realmName;

        public AllComponentDetails(String realmName) {
            this.realmName = realmName;
        }

        @Override
        public Object run(KeycloakSession session) {
            RealmModel realm = session.realms().getRealmByName(realmName);
            session.getContext().setRealm(realm);
            Map<String, TestComponentProvider.DetailsRepresentation> details = realm
                    .getComponentsStream(realm.getId(), TestComponentProvider.class.getName())
                    .collect(Collectors.toMap(
                            ComponentModel::getName,
                            componentModel -> createProvider(session, componentModel).getDetails()));
            return new ComponentDetailsMap(details);
        }
    }

    public static class ComponentDetailsMap {
        private final Map<String, TestComponentProvider.DetailsRepresentation> details;

        public ComponentDetailsMap(Map<String, TestComponentProvider.DetailsRepresentation> details) {
            this.details = details;
        }

        public Map<String, TestComponentProvider.DetailsRepresentation> getDetails() {
            return details;
        }
    }

    private static TestComponentProvider createProvider(KeycloakSession session, ComponentModel componentModel) {
        ProviderFactory<TestComponentProvider> factory = session.getKeycloakSessionFactory()
                .getProviderFactory(TestComponentProvider.class, componentModel.getProviderId());
        TestComponentProviderFactory componentFactory = (TestComponentProviderFactory) factory;
        return componentFactory.create(session, componentModel);
    }
}
