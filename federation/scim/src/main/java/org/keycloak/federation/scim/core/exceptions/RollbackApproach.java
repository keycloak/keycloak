package org.keycloak.federation.scim.core.exceptions;

import com.google.common.collect.Lists;
import org.keycloak.federation.scim.core.ScrimEndPointConfiguration;

import java.util.ArrayList;

public enum RollbackApproach implements RollbackStrategy {
    ALWAYS_ROLLBACK {
        @Override
        public boolean shouldRollback(ScrimEndPointConfiguration configuration, ScimPropagationException e) {
            return true;
        }
    },
    NEVER_ROLLBACK {
        @Override
        public boolean shouldRollback(ScrimEndPointConfiguration configuration, ScimPropagationException e) {
            return false;
        }
    },
    CRITICAL_ONLY_ROLLBACK {
        @Override
        public boolean shouldRollback(ScrimEndPointConfiguration configuration, ScimPropagationException e) {
            if (e instanceof InconsistentScimMappingException) {
                // Occurs when mapping between a SCIM resource and a keycloak user failed (missing, ambiguous..)
                // Log can be sufficient here, no rollback required
                return false;
            }
            if (e instanceof UnexpectedScimDataException) {
                // Occurs when a SCIM endpoint sends invalid date (e.g. group with empty name, user without ids...)
                // No rollback required : we cannot recover. This needs to be fixed in the SCIM endpoint data
                return false;
            }
            if (e instanceof InvalidResponseFromScimEndpointException invalidResponseFromScimEndpointException) {
                return shouldRollbackBecauseOfResponse(invalidResponseFromScimEndpointException);
            }
            // Should not occur
            throw new IllegalStateException("Unkown ScimPropagationException", e);
        }

        private boolean shouldRollbackBecauseOfResponse(InvalidResponseFromScimEndpointException e) {
            // If we have a response
            return e.getResponse().map(r -> {
                // We consider that 404 are acceptable, otherwise rollback
                ArrayList<Integer> acceptableStatus = Lists.newArrayList(200, 204, 404);
                return !acceptableStatus.contains(r.getHttpStatus());
            }).orElse(
                    // Never got an answer, server was either misconfigured or unreachable
                    // No rollback in that case.
                    false);
        }
    }
}
