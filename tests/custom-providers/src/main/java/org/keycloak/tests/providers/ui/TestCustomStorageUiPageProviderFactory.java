package org.keycloak.tests.providers.ui;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import org.keycloak.Config;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;
import org.keycloak.services.ui.extend.ComponentStorageFactory;
import org.keycloak.services.ui.extend.UiPageProvider;
import org.keycloak.services.ui.extend.UiPageProviderFactory;

import static org.keycloak.provider.ProviderConfigProperty.STRING_TYPE;

public class TestCustomStorageUiPageProviderFactory
        implements UiPageProviderFactory<UiPageProvider>, ComponentStorageFactory {

    public static final String PROVIDER_ID = "test-custom-storage-ui-page";

    private static final Map<String, Map<String, ComponentModel>> STORAGE = new ConcurrentHashMap<>();

    private final List<ProviderConfigProperty> config = ProviderConfigurationBuilder.create()
            .property("value", "Value", "Stored configuration value", STRING_TYPE, null, null, false)
            .build();

    @Override
    public UiPageProvider create(KeycloakSession session, ComponentModel model) {
        return null;
    }

    @Override
    public String getHelpText() {
        return "Test UiPage provider with custom component storage";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return config;
    }

    @Override
    public Stream<ComponentModel> listComponents(
            KeycloakSession session,
            RealmModel realm,
            String parentId,
            String providerId) {
        return realmStorage(realm).values().stream()
                .filter(model -> Objects.equals(parentId, model.getParentId()))
                .filter(model -> providerId.equals(model.getProviderId()));
    }

    @Override
    public ComponentModel getComponent(KeycloakSession session, RealmModel realm, String id) {
        ComponentModel model = realmStorage(realm).get(id);
        return model == null ? null : new ComponentModel(model);
    }

    @Override
    public ComponentModel createComponent(KeycloakSession session, RealmModel realm, ComponentModel model) {
        if (model.getId() == null) {
            model.setId(KeycloakModelUtils.generateId());
        }
        ComponentModel stored = new ComponentModel(model);
        realmStorage(realm).put(stored.getId(), stored);
        return new ComponentModel(stored);
    }

    @Override
    public ComponentModel updateComponent(
            KeycloakSession session,
            RealmModel realm,
            ComponentModel oldModel,
            ComponentModel newModel) {
        ComponentModel stored = new ComponentModel(newModel);
        realmStorage(realm).put(stored.getId(), stored);
        return new ComponentModel(stored);
    }

    @Override
    public void removeComponent(KeycloakSession session, RealmModel realm, ComponentModel model) {
        realmStorage(realm).remove(model.getId());
    }

    @Override
    public void init(Config.Scope config) {
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
    }

    @Override
    public void close() {
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    private Map<String, ComponentModel> realmStorage(RealmModel realm) {
        return STORAGE.computeIfAbsent(realm.getId(), id -> new ConcurrentHashMap<>());
    }
}
