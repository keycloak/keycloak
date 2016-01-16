package org.keycloak.models;

import org.keycloak.provider.ProviderFactory;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface UserSessionProviderFactory extends ProviderFactory<UserSessionProvider> {

    // This is supposed to prefill all userSessions and clientSessions from userSessionPersister to the userSession infinispan/memory storage
    void loadPersistentSessions(KeycloakSessionFactory sessionFactory, final int maxErrors, final int sessionsPerSegment);

}
