package org.keycloak.tests.cluster;

import java.util.Map;
import java.util.stream.Collectors;

import org.keycloak.component.ComponentModel;
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
                    .map(componentModel -> resolveProvider(session, componentModel).getDetails())
                    .orElseThrow(() -> new AssertionError(
                            "Component '" + componentName + "' not found in realm '" + realmName + "'"));
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
                            componentModel -> resolveProvider(session, componentModel).getDetails()));
            return new ComponentDetailsMap(details);
        }
    }

    public record ComponentDetailsMap(Map<String, TestComponentProvider.DetailsRepresentation> details) {
    }

    private static TestComponentProvider resolveProvider(KeycloakSession session, ComponentModel componentModel) {
        TestComponentProvider provider = session.getComponentProvider(TestComponentProvider.class, componentModel.getId());
        if (provider == null) {
            throw new AssertionError("Component provider '" + componentModel.getName()
                    + "' (id=" + componentModel.getId() + ") could not be resolved");
        }
        return provider;
    }
}
