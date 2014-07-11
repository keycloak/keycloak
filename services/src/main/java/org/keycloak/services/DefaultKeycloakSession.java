package org.keycloak.services;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakTransaction;
import org.keycloak.models.ModelProvider;
import org.keycloak.models.UserProvider;
import org.keycloak.models.UserSessionProvider;
import org.keycloak.models.cache.CacheModelProvider;
import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class DefaultKeycloakSession implements KeycloakSession {

    private final DefaultKeycloakSessionFactory factory;
    private final Map<Integer, Provider> providers = new HashMap<Integer, Provider>();
    private ModelProvider model;
    private UserSessionProvider sessionProvider;
    private final List<KeycloakTransaction> managedTransactions = new ArrayList<KeycloakTransaction>();

    private final KeycloakTransaction transaction = new KeycloakTransaction() {
        protected boolean active;
        protected boolean rollback;

        @Override
        public void begin() {
            active = true;
        }

        @Override
        public void commit() {
            if (!active) throw new IllegalStateException("Transaction not active");
            try {
                if (rollback) {
                    rollback();
                    throw new RuntimeException("Transaction markedfor rollback, so rollback happend");
                }
                for (KeycloakTransaction transaction : managedTransactions) {
                    transaction.commit();
                }
            } finally {
                active = false;
            }

        }

        @Override
        public void rollback() {
            if (!active) throw new IllegalStateException("Transaction not active");
            try {
                for (KeycloakTransaction transaction : managedTransactions) {
                    transaction.rollback();
                }
            } finally {
                active = false;
            }
        }

        @Override
        public void setRollbackOnly() {
            if (!active) throw new IllegalStateException("Transaction not active");
            rollback = true;
        }

        @Override
        public boolean getRollbackOnly() {
            if (!active) throw new IllegalStateException("Transaction not active");
            return rollback;
        }

        @Override
        public boolean isActive() {
            return active;
        }
    };

    public DefaultKeycloakSession(DefaultKeycloakSessionFactory factory) {
        this.factory = factory;
    }

    private ModelProvider getModelProvider() {
        if (factory.getDefaultProvider(CacheModelProvider.class) != null) {
            return getProvider(CacheModelProvider.class);
        } else {
            return getProvider(ModelProvider.class);
        }
    }


    @Override
    public KeycloakTransaction getTransaction() {
        return transaction;
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
    public ModelProvider model() {
        if (!transaction.isActive()) throw new IllegalStateException("Transaction is not active");
        if (model == null) {
            model = getModelProvider();
            model.getTransaction().begin();
            managedTransactions.add(model.getTransaction());
        }
        return model;
    }

    @Override
    public UserSessionProvider sessions() {
        if (!transaction.isActive()) throw new IllegalStateException("Transaction is not active");
        if (sessionProvider == null) {
            sessionProvider = getProvider(UserSessionProvider.class);
            sessionProvider.getTransaction().begin();
            managedTransactions.add(sessionProvider.getTransaction());
        }
        return sessionProvider;
    }

    public void close() {
        for (Provider p : providers.values()) {
            p.close();
        }
    }

}
