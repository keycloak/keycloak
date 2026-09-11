package org.keycloak.tests.admin;

import java.io.Serializable;
import java.util.List;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;

import org.keycloak.admin.client.resource.ComponentsResource;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.models.RealmModel;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.services.ui.extend.UiPageProvider;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.remote.providers.runonserver.FetchOnServer;
import org.keycloak.testframework.remote.providers.runonserver.FetchOnServerWrapper;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;
import org.keycloak.testframework.util.ApiUtil;
import org.keycloak.tests.providers.ui.TestCustomStorageUiPageProviderFactory;
import org.keycloak.tests.suites.DatabaseTest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

@KeycloakIntegrationTest(config = UiExtensionComponentStorageTest.ServerConfig.class)
@DatabaseTest
public class UiExtensionComponentStorageTest {

    @InjectRealm(lifecycle = LifeCycle.METHOD)
    ManagedRealm managedRealm;

    @InjectRunOnServer
    RunOnServerClient runOnServer;

    private ComponentsResource components;

    @BeforeEach
    public void before() {
        components = managedRealm.admin().components();
    }

    @Test
    public void testCustomStorageCrud() {
        ComponentRepresentation created = createComponentRepresentation("custom-storage-item");
        created.getConfig().addFirst("value", "initial");

        String id = createComponent(created);

        assertFalse(isStoredInRealmComponentTable(id));

        List<ComponentRepresentation> listed = components.query(
                managedRealm.getId(),
                UiPageProvider.class.getName());
        assertThat(listed, hasSize(1));
        assertEquals(id, listed.get(0).getId());
        assertEquals("initial", listed.get(0).getConfig().getFirst("value"));

        ComponentRepresentation fetched = components.component(id).toRepresentation();
        assertEquals("custom-storage-item", fetched.getName());
        assertEquals("initial", fetched.getConfig().getFirst("value"));

        fetched.getConfig().putSingle("value", "updated");
        components.component(id).update(fetched);

        ComponentRepresentation updated = components.component(id).toRepresentation();
        assertEquals("updated", updated.getConfig().getFirst("value"));

        components.component(id).remove();

        assertThrows(NotFoundException.class, () -> components.component(id).toRepresentation());
        assertThat(components.query(managedRealm.getId(), UiPageProvider.class.getName()), hasSize(0));
    }

    @Test
    public void testRejectProviderChangeForCustomStorage() {
        ComponentRepresentation created = createComponentRepresentation("custom-storage-item");
        String id = createComponent(created);

        ComponentRepresentation fetched = components.component(id).toRepresentation();
        fetched.setProviderId("another-provider");

        assertThrows(BadRequestException.class, () -> components.component(id).update(fetched));
    }

    private String createComponent(ComponentRepresentation rep) {
        try (Response response = components.add(rep)) {
            return ApiUtil.getCreatedId(response);
        }
    }

    private ComponentRepresentation createComponentRepresentation(String name) {
        ComponentRepresentation rep = new ComponentRepresentation();
        rep.setName(name);
        rep.setParentId(managedRealm.getId());
        rep.setProviderId(TestCustomStorageUiPageProviderFactory.PROVIDER_ID);
        rep.setProviderType(UiPageProvider.class.getName());
        rep.setConfig(new MultivaluedHashMap<>());
        return rep;
    }

    private boolean isStoredInRealmComponentTable(String componentId) {
        return runOnServer.fetch(new RealmComponentExists(componentId));
    }

    public static class ServerConfig implements KeycloakServerConfig {

        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            return config.dependency("org.keycloak.tests", "keycloak-tests-custom-providers")
                    .features(org.keycloak.common.Profile.Feature.DECLARATIVE_UI);
        }
    }

    private record RealmComponentExists(String componentId)
            implements FetchOnServerWrapper<Boolean>, Serializable {

        @Override
        public FetchOnServer getRunOnServer() {
            return session -> {
                RealmModel realm = session.getContext().getRealm();
                return realm.getComponent(componentId) != null;
            };
        }

        @Override
        public Class<Boolean> getResultClass() {
            return Boolean.class;
        }
    }
}
