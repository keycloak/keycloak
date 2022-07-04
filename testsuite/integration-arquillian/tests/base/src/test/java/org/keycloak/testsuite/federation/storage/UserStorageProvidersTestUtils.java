package org.keycloak.testsuite.federation.storage;

import org.jboss.logging.Logger;
import org.keycloak.common.util.reflections.Types;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.LegacyRealmModel;
import org.keycloak.models.ModelException;
import org.keycloak.models.RealmModel;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.UserStorageProviderFactory;
import org.keycloak.storage.UserStorageProviderModel;

import java.util.stream.Stream;

public class UserStorageProvidersTestUtils {

    private static final Logger logger = Logger.getLogger(UserStorageProvidersTestUtils.class);


    public static boolean isStorageProviderEnabled(RealmModel realm, String providerId) {
        UserStorageProviderModel model = getStorageProviderModel(realm, providerId);
        return model.isEnabled();
    }

    private static UserStorageProviderFactory getUserStorageProviderFactory(UserStorageProviderModel model, KeycloakSession session) {
        return (UserStorageProviderFactory) session.getKeycloakSessionFactory()
                .getProviderFactory(UserStorageProvider.class, model.getProviderId());
    }

    public static <T> Stream<T> getEnabledStorageProviders(KeycloakSession session, RealmModel realm, Class<T> type) {
        return getStorageProviders(realm, session, type)
                .filter(UserStorageProviderModel::isEnabled)
                .map(model -> type.cast(getStorageProviderInstance(session, model, getUserStorageProviderFactory(model, session))));
    }

    public static UserStorageProvider getStorageProviderInstance(KeycloakSession session, UserStorageProviderModel model, UserStorageProviderFactory factory) {
        UserStorageProvider instance = (UserStorageProvider)session.getAttribute(model.getId());
        if (instance != null) return instance;
        instance = factory.create(session, model);
        if (instance == null) {
            throw new IllegalStateException("UserStorageProvideFactory (of type " + factory.getClass().getName() + ") produced a null instance");
        }
        session.enlistForClose(instance);
        session.setAttribute(model.getId(), instance);
        return instance;
    }

    public static <T> Stream<UserStorageProviderModel> getStorageProviders(RealmModel realm, KeycloakSession session, Class<T> type) {
        return ((LegacyRealmModel) realm).getUserStorageProvidersStream()
                .filter(model -> {
                    UserStorageProviderFactory factory = getUserStorageProviderFactory(model, session);
                    if (factory == null) {
                        logger.warnv("Configured UserStorageProvider {0} of provider id {1} does not exist in realm {2}",
                                model.getName(), model.getProviderId(), realm.getName());
                        return false;
                    } else {
                        return Types.supports(type, factory, UserStorageProviderFactory.class);
                    }
                });
    }

    public static UserStorageProvider getStorageProvider(KeycloakSession session, RealmModel realm, String componentId) {
        ComponentModel model = realm.getComponent(componentId);
        if (model == null) return null;
        UserStorageProviderModel storageModel = new UserStorageProviderModel(model);
        UserStorageProviderFactory factory = (UserStorageProviderFactory)session.getKeycloakSessionFactory().getProviderFactory(UserStorageProvider.class, model.getProviderId());
        if (factory == null) {
            throw new ModelException("Could not find UserStorageProviderFactory for: " + model.getProviderId());
        }
        return getStorageProviderInstance(session, storageModel, factory);
    }

    public static <T> Stream<T> getStorageProviders(KeycloakSession session, RealmModel realm, Class<T> type) {
        return getStorageProviders(realm, session, type)
                .map(model -> type.cast(getStorageProviderInstance(session, model, getUserStorageProviderFactory(model, session))));
    }

    public static UserStorageProviderModel getStorageProviderModel(RealmModel realm, String componentId) {
        ComponentModel model = realm.getComponent(componentId);
        if (model == null) return null;
        return new UserStorageProviderModel(model);
    }

}
