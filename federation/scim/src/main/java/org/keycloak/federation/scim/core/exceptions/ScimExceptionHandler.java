package org.keycloak.federation.scim.core.exceptions;

import org.jboss.logging.Logger;
import org.keycloak.models.KeycloakSession;
import org.keycloak.federation.scim.core.ScimEndPointConfiguration;

/**
 * In charge of dealing with SCIM exceptions by ignoring, logging or rollback transaction according to : - The context in which
 * it occurs (sync, user creation...) - The related SCIM endpoint and its configuration - The thrown exception itself
 */
public class ScimExceptionHandler {
    private static final Logger LOGGER = Logger.getLogger(ScimExceptionHandler.class);

    private final KeycloakSession session;
    private final RollbackStrategy rollbackStrategy;

    public ScimExceptionHandler(KeycloakSession session) {
        this(session, RollbackApproach.CRITICAL_ONLY_ROLLBACK);
    }

    public ScimExceptionHandler(KeycloakSession session, RollbackStrategy rollbackStrategy) {
        this.session = session;
        this.rollbackStrategy = rollbackStrategy;
    }

    /**
     * Handles the given exception by loggin and/or rollback transaction.
     *
     * @param scimProviderConfiguration the configuration of the endpoint for which the propagation exception occured
     * @param e the occuring exception
     */
    public void handleException(ScimEndPointConfiguration scimProviderConfiguration, ScimPropagationException e) {
        String errorMessage = "[SCIM] Error while propagating to SCIM endpoint " + scimProviderConfiguration.getName();
        if (rollbackStrategy.shouldRollback(scimProviderConfiguration, e)) {
            session.getTransactionManager().rollback();
            LOGGER.error("TRANSACTION ROLLBACK - " + errorMessage, e);
        } else {
            LOGGER.warn(errorMessage, e);
        }
    }
}
