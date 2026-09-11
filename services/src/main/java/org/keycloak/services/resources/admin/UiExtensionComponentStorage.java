package org.keycloak.services.resources.admin;

import java.util.Objects;
import java.util.stream.Stream;

import org.keycloak.component.ComponentFactory;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.services.ui.extend.ComponentStorageFactory;
import org.keycloak.services.ui.extend.UiExtensionSupport;
import org.keycloak.services.ui.extend.UiPageProvider;
import org.keycloak.services.ui.extend.UiTabProvider;

final class UiExtensionComponentStorage {

    private UiExtensionComponentStorage() {
    }

    static ComponentStorageFactory getStorageFactory(KeycloakSession session, String providerType, String providerId) {
        ProviderFactory<?> factory = getProviderFactory(session, providerType, providerId);
        if (factory instanceof ComponentStorageFactory storageFactory) {
            return storageFactory;
        }
        return null;
    }

    static UiExtensionSupport getExtensionFactory(KeycloakSession session, String providerType, String providerId) {
        ProviderFactory<?> factory = getProviderFactory(session, providerType, providerId);
        if (factory instanceof UiExtensionSupport extensionSupport) {
            return extensionSupport;
        }
        return null;
    }

    static Stream<ComponentModel> listComponents(
            KeycloakSession session,
            RealmModel realm,
            String parent,
            String type,
            String providerId) {
        if (providerId != null) {
            ComponentStorageFactory storageFactory = getStorageFactory(session, type, providerId);
            if (storageFactory == null) {
                storageFactory = findStorageFactoryByProviderId(session, providerId);
            }
            if (storageFactory == null) {
                return null;
            }
            String resolvedParent = parent != null ? parent : realm.getId();
            return storageFactory.listComponents(session, realm, resolvedParent, providerId);
        }
        if (type == null) {
            return null;
        }
        try {
            Class<? extends Provider> providerClass =
                    (Class<? extends Provider>) session.getProviderClass(type);
            return session.getKeycloakSessionFactory()
                    .getProviderFactoriesStream(providerClass)
                    .filter(factory -> factory instanceof ComponentStorageFactory)
                    .filter(factory -> !(factory instanceof ComponentFactory<?, ?> componentFactory
                            && componentFactory.isInternal()))
                    .flatMap(factory -> ((ComponentStorageFactory) factory)
                            .listComponents(session, realm, parent, factory.getId()));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    static ComponentModel getComponent(KeycloakSession session, RealmModel realm, ComponentModel model) {
        ComponentStorageFactory storageFactory = getStorageFactory(session, model.getProviderType(), model.getProviderId());
        if (storageFactory == null) {
            return null;
        }
        return storageFactory.getComponent(session, realm, model.getId());
    }

    static ComponentModel getComponentById(KeycloakSession session, RealmModel realm, String id) {
        return Stream.concat(
                session.getKeycloakSessionFactory().getProviderFactoriesStream(UiPageProvider.class),
                session.getKeycloakSessionFactory().getProviderFactoriesStream(UiTabProvider.class))
                .filter(factory -> factory instanceof ComponentStorageFactory)
                .filter(factory -> !(factory instanceof ComponentFactory<?, ?> componentFactory
                        && componentFactory.isInternal()))
                .map(ComponentStorageFactory.class::cast)
                .map(storage -> storage.getComponent(session, realm, id))
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    static ComponentModel createComponent(KeycloakSession session, RealmModel realm, ComponentModel model) {
        ComponentStorageFactory storageFactory = getStorageFactory(session, model.getProviderType(), model.getProviderId());
        if (storageFactory == null) {
            return null;
        }
        return storageFactory.createComponent(session, realm, model);
    }

    static ComponentModel updateComponent(
            KeycloakSession session,
            RealmModel realm,
            ComponentModel oldModel,
            ComponentModel newModel) {
        ComponentStorageFactory storageFactory = getStorageFactory(session, oldModel.getProviderType(), oldModel.getProviderId());
        if (storageFactory == null) {
            return null;
        }
        return storageFactory.updateComponent(session, realm, oldModel, newModel);
    }

    static void removeComponent(KeycloakSession session, RealmModel realm, ComponentModel model) {
        ComponentStorageFactory storageFactory = getStorageFactory(session, model.getProviderType(), model.getProviderId());
        if (storageFactory != null) {
            storageFactory.removeComponent(session, realm, model);
        }
    }

    static boolean usesCustomStorage(String providerType, String providerId, KeycloakSession session) {
        return getStorageFactory(session, providerType, providerId) != null;
    }

    private static ComponentStorageFactory findStorageFactoryByProviderId(
            KeycloakSession session, String providerId) {
        return Stream.concat(
                session.getKeycloakSessionFactory().getProviderFactoriesStream(UiPageProvider.class),
                session.getKeycloakSessionFactory().getProviderFactoriesStream(UiTabProvider.class))
                .filter(factory -> factory instanceof ComponentStorageFactory)
                .filter(factory -> !(factory instanceof ComponentFactory<?, ?> componentFactory
                        && componentFactory.isInternal()))
                .filter(factory -> providerId.equals(factory.getId()))
                .map(ComponentStorageFactory.class::cast)
                .findFirst()
                .orElse(null);
    }

    @SuppressWarnings("unchecked")
    private static ProviderFactory<?> getProviderFactory(KeycloakSession session, String providerType, String providerId) {
        try {
            Class<? extends Provider> providerClass =
                    (Class<? extends Provider>) session.getProviderClass(providerType);
            ProviderFactory<?> factory =
                    session.getKeycloakSessionFactory().getProviderFactory(providerClass, providerId);
            if (factory instanceof ComponentFactory<?, ?> componentFactory && componentFactory.isInternal()) {
                return null;
            }
            return factory;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
