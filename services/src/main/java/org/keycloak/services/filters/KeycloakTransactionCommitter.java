package org.keycloak.services.filters;

import org.jboss.resteasy.core.ServerResponse;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.jboss.resteasy.spi.interception.PostProcessInterceptor;
import org.keycloak.models.KeycloakTransaction;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class KeycloakTransactionCommitter implements PostProcessInterceptor {
    @Override
    public void postProcess(ServerResponse response) {
        KeycloakTransaction tx = ResteasyProviderFactory.getContextData(KeycloakTransaction.class);
        if (tx != null && tx.isActive()) {
            if (tx.getRollbackOnly()) {
                tx.rollback();
            } else {
                tx.commit();
            }
        }

    }
}
