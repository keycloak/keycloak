package org.keycloak.testframework.remote.infinispan;

import org.keycloak.connections.infinispan.InfinispanConnectionProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;

import org.infinispan.manager.EmbeddedCacheManager;
import org.jboss.logging.Logger;

import static org.keycloak.connections.infinispan.InfinispanUtil.setTimeServiceToKeycloakTime;

public class InfinispanTimeService {

    private static final Logger logger = Logger.getLogger(InfinispanTimeService.class);

    private final RunOnServerClient runOnServer;

    private static Runnable origTimeService = null;
    private boolean remoteServiceSet = false;

    public InfinispanTimeService(RunOnServerClient runOnServer) {
        this.runOnServer = runOnServer;
    }

    public void setTestingInfinispanTimeService() {
        runOnServer.run(InfinispanTimeService::setTestingTimeService);
        remoteServiceSet = true;
    }

    void revertTestingInfinispanTimeService() {
        if (remoteServiceSet) {
            runOnServer.run(InfinispanTimeService::revertTestingTimeService);
            remoteServiceSet = false;
        }
    }

    /**
     * Set Keycloak test TimeService to infinispan cacheManager. This will cause that infinispan will be aware of Keycloak Time offset, which is useful
     * for testing that infinispan entries are expired after moving Keycloak time forward with {@link org.keycloak.common.util.Time#setOffset} .
     */
    public static void setTestingTimeService(KeycloakSession session) {
        // Testing timeService already set. This shouldn't happen if this utility is properly used
        if (origTimeService != null) {
            throw new IllegalStateException("Calling setTestingTimeService when testing TimeService was already set");
        }

        InfinispanConnectionProvider ispnProvider = session.getProvider(InfinispanConnectionProvider.class);
        if (ispnProvider != null) {
            logger.info("Will set KeycloakIspnTimeService to the infinispan cacheManager");
            EmbeddedCacheManager cacheManager = ispnProvider.getCache(InfinispanConnectionProvider.USER_CACHE_NAME).getCacheManager();
            origTimeService = setTimeServiceToKeycloakTime(cacheManager);
        }
    }

    public static void revertTestingTimeService(KeycloakSession session) {
        // Testing timeService not set. This shouldn't happen if this utility is properly used
        InfinispanConnectionProvider ispnProvider = session.getProvider(InfinispanConnectionProvider.class);
        if (ispnProvider != null) {
            if (origTimeService == null) {
                throw new IllegalStateException("Calling revertTimeService when testing TimeService was not set");
            }

            origTimeService.run();
            origTimeService = null;
        }
    }
}
