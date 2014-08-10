package org.keycloak.services;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.KeycloakTransactionManager;
import org.keycloak.models.RealmProvider;
import org.keycloak.models.UserFederationManager;
import org.keycloak.models.UserProvider;
import org.keycloak.models.UserSessionProvider;
import org.keycloak.models.cache.CacheRealmProvider;
import org.keycloak.models.cache.CacheUserProvider;
import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderFactory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class DefaultKeycloakSession implements KeycloakSession {

    private final DefaultKeycloakSessionFactory factory;
    private final Map<Integer, Provider> providers = new HashMap<Integer, Provider>();
    private final List<Provider> closable = new LinkedList<Provider>();
    private final DefaultKeycloakTransactionManager transactionManager;
    private RealmProvider model;
    private UserProvider userModel;
    private UserSessionProvider sessionProvider;
    private UserFederationManager federationManager;

    public DefaultKeycloakSession(DefaultKeycloakSessionFactory factory) {
        this.factory = factory;
        this.transactionManager = new DefaultKeycloakTransactionManager();
        federationManager = new UserFederationManager(this);
    }

    private RealmProvider getRealmProvider() {
        if (factory.getDefaultProvider(CacheRealmProvider.class) != null) {
            return getProvider(CacheRealmProvider.class);
        } else {
            return getProvider(RealmProvider.class);
        }
    }

    private UserProvider getUserProvider() {
        if (factory.getDefaultProvider(CacheUserProvider.class) != null) {
            return getProvider(CacheUserProvider.class);
        } else {
            return getProvider(UserProvider.class);
        }
    }

    @Override
    public void enlistForClose(Provider provider) {
        closable.add(provider);
    }

    @Override
    public KeycloakTransactionManager getTransaction() {
        return transactionManager;
    }

    @Override
    public KeycloakSessionFactory getKeycloakSessionFactory() {
        return factory;
    }

    @Override
    public UserProvider userStorage() {
        if (userModel == null) {
            userModel = getUserProvider();
        }
        return userModel;

    }

    public <T extends Provider> T getProvider(Class<T> clazz) {
        Integer hash = clazz.hashCode();
        T provider = (T) providers.get(hash);
        if (provider == null) {
            ProviderFactory<T> providerFactory = factory.getProviderFactory(clazz);
            if (providerFactory != null) {
                provider = providerFactory.create(this);
                providers.put(hash, provider);
            }
        }
        return provider;
    }

    public <T extends Provider> T getProvider(Class<T> clazz, String id) {
        Integer hash = clazz.hashCode() + id.hashCode();
        T provider = (T) providers.get(hash);
        if (provider == null) {
            ProviderFactory<T> providerFactory = factory.getProviderFactory(clazz, id);
            if (providerFactory != null) {
                provider = providerFactory.create(this);
                providers.put(hash, provider);
            }
        }
        return provider;
    }

    public <T extends Provider> Set<String> listProviderIds(Class<T> clazz) {
        return factory.getAllProviderIds(clazz);
    }

    @Override
    public <T extends Provider> Set<T> getAllProviders(Class<T> clazz) {
        Set<T> providers = new HashSet<T>();
        for (String id : listProviderIds(clazz)) {
            providers.add(getProvider(clazz, id));
        }
        return providers;
    }

    @Override
    public RealmProvider realms() {
        if (model == null) {
            model = getRealmProvider();
        }
        return model;
    }

    @Override
    public UserFederationManager users() {
        return federationManager;
    }

    @Override
    public UserSessionProvider sessions() {
        if (sessionProvider == null) {
            sessionProvider = getProvider(UserSessionProvider.class);
        }
        return sessionProvider;
    }

    public void close() {
        for (Provider p : providers.values()) {
            try {
                p.close();
            } catch (Exception e) {
            }
        }
        for (Provider p : closable) {
            try {
                p.close();
            } catch (Exception e) {
            }
        }
    }

}
