package org.keycloak.federation.scim.core.exceptions;

import org.keycloak.federation.scim.core.ScrimEndPointConfiguration;

/**
 * In charge of deciding, when facing a SCIM-related issue during an operation (e.g User creation), whether we should : - Log
 * the issue and let the operation succeed in Keycloack database (potentially unsynchronising Keycloack with the SCIM servers) -
 * Rollback the whole operation
 */
public interface RollbackStrategy {

    /**
     * Indicates whether we should rollback the whole transaction because of the given exception.
     *
     * @param configuration The SCIM Endpoint configuration for which the exception occured
     * @param e the exception that we have to handle
     * @return true if transaction should be rolled back, false if we should log and continue operation
     */
    boolean shouldRollback(ScrimEndPointConfiguration configuration, ScimPropagationException e);
}
