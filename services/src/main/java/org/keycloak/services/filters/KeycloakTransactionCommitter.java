/*
 */

package org.keycloak.services.filters;

import org.jboss.logging.Logger;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.keycloak.models.KeycloakTransaction;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import java.io.IOException;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class KeycloakTransactionCommitter implements ContainerResponseFilter {

    private static final Logger log = Logger.getLogger(KeycloakTransactionCommitter.class);

    @Override
    public void filter(ContainerRequestContext containerRequestContext, ContainerResponseContext containerResponseContext) throws IOException {
        KeycloakTransaction tx = ResteasyProviderFactory.getContextData(KeycloakTransaction.class);

        if (tx != null && tx.isActive()) {
            if (tx.getRollbackOnly() || containerResponseContext.getStatus() >= 400) {
                log.debugv("Rollback transaction, rollback = {0}, status = {1}", tx.getRollbackOnly(), containerResponseContext.getStatus());
                tx.rollback();
            } else {
                log.debugv("Committing transaction, status = {0}", containerResponseContext.getStatus());
                tx.commit();
            }
        }
    }

}
