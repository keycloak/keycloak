package org.keycloak.testutils;

import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.UserFederationProvider;
import org.keycloak.models.UserFederationProviderFactory;
import org.keycloak.models.UserFederationProviderModel;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class DummyUserFederationProviderFactory implements UserFederationProviderFactory {

    private static final Logger logger = Logger.getLogger(DummyUserFederationProviderFactory.class);
    public static final String PROVIDER_NAME = "dummy";

    private AtomicInteger fullSyncCounter = new AtomicInteger();
    private AtomicInteger changedSyncCounter = new AtomicInteger();

    @Override
    public UserFederationProvider getInstance(KeycloakSession session, UserFederationProviderModel model) {
        return new DummyUserFederationProvider();
    }

    @Override
    public Set<String> getConfigurationOptions() {
        Set<String> list = new HashSet<String>();
        list.add("important.config");
        return list;
    }

    @Override
    public UserFederationProvider create(KeycloakSession session) {
        return new DummyUserFederationProvider();
    }

    @Override
    public void init(Config.Scope config) {

    }

    @Override
    public void close() {

    }

    @Override
    public String getId() {
        return PROVIDER_NAME;
    }

    @Override
    public void syncAllUsers(KeycloakSessionFactory sessionFactory, String realmId, UserFederationProviderModel model) {
        logger.info("syncAllUsers invoked");
        fullSyncCounter.incrementAndGet();
    }

    @Override
    public void syncChangedUsers(KeycloakSessionFactory sessionFactory, String realmId, UserFederationProviderModel model, Date lastSync) {
        logger.info("syncChangedUsers invoked");
        changedSyncCounter.incrementAndGet();
    }

    public int getFullSyncCounter() {
        return fullSyncCounter.get();
    }

    public int getChangedSyncCounter() {
        return changedSyncCounter.get();
    }
}
