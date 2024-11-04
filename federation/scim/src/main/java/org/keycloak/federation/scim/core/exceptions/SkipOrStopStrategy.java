package org.keycloak.federation.scim.core.exceptions;

import org.keycloak.federation.scim.core.ScrimEndPointConfiguration;

/**
 * In charge of deciding, when facing a SCIM-related issue, whether we should : - log a warning, skip the problematic element
 * and continue the rest of the operation - stop immediately the whole operation (typically, a synchronisation between SCIM and
 * Keycloack)
 */
public interface SkipOrStopStrategy {
    /**
     * Indicates if, during a synchronisation from Keycloack to a SCIM endpoint, we should : - cancel the whole synchronisation
     * if an element CRUD fail, or - keep on with synchronisation, allowing a partial synchronisation
     *
     * @param configuration the configuration of the endpoint in which the error occurred
     * @return true if a partial synchronisation is allowed, false if we should stop the whole synchronisation at first issue
     */
    boolean allowPartialSynchronizationWhenPushingToScim(ScrimEndPointConfiguration configuration);

    /**
     * Indicates if, during a synchronisation from a SCIM endpoint to Keycloack, we should : - cancel the whole synchronisation
     * if an element CRUD fail, or - keep on with synchronisation, allowing a partial synchronisation
     *
     * @param configuration the configuration of the endpoint in which the error occurred
     * @return true if a partial synchronisation is allowed, false if we should interrupt the whole synchronisation at first
     *         issue
     */
    boolean allowPartialSynchronizationWhenPullingFromScim(ScrimEndPointConfiguration configuration);

    /**
     * Indicates if, when we propagate a group creation or update to a SCIM endpoint and some of its members are not mapped to
     * SCIM, we should allow partial group update or interrupt completely.
     *
     * @param configuration the configuration of the endpoint in which the error occurred
     * @return true if a partial group update is allowed, false if we should interrupt the group update in case of any unmapped
     *         member
     */
    boolean allowMissingMembersWhenPushingGroupToScim(ScrimEndPointConfiguration configuration);

    /**
     * Indicates if, when facing an invalid SCIM endpoint configuration (resulting in a unreachable SCIM server), we should stop
     * or ignore this configuration.
     *
     * @return true the invalid endpoint should be ignored, * false if we should interrupt the rest of the synchronisation
     */
    boolean allowInvalidEndpointConfiguration();

    /**
     * Indicates if, when trying to pull User or Groups from a SCIM endpoint, we encounter a invalid data (e.g. group with empty
     * name), we should : - Skip the invalid element pull and continue - Cancel the whole synchronisation
     *
     * @param configuration the configuration of the endpoint in which the error occurred
     * @return true if we should skip the invalid data synchronisation and pursue, false if we should interrupt immediately the
     *         whole synchronisation
     */
    boolean skipInvalidDataFromScimEndpoint(ScrimEndPointConfiguration configuration);

}
