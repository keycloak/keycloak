package org.keycloak.federation.scim.core.exceptions;

import org.keycloak.federation.scim.core.ScrimEndPointConfiguration;

public enum SkipOrStopApproach implements SkipOrStopStrategy {
    ALWAYS_SKIP_AND_CONTINUE {
        @Override
        public boolean allowPartialSynchronizationWhenPushingToScim(ScrimEndPointConfiguration configuration) {
            return false;
        }

        @Override
        public boolean allowPartialSynchronizationWhenPullingFromScim(ScrimEndPointConfiguration configuration) {
            return false;
        }

        @Override
        public boolean allowMissingMembersWhenPushingGroupToScim(ScrimEndPointConfiguration configuration) {
            return false;
        }

        @Override
        public boolean allowInvalidEndpointConfiguration() {
            return false;
        }

        @Override
        public boolean skipInvalidDataFromScimEndpoint(ScrimEndPointConfiguration configuration) {
            return false;
        }
    },
    ALWAYS_STOP {
        @Override
        public boolean allowPartialSynchronizationWhenPushingToScim(ScrimEndPointConfiguration configuration) {
            return true;
        }

        @Override
        public boolean allowPartialSynchronizationWhenPullingFromScim(ScrimEndPointConfiguration configuration) {
            return true;
        }

        @Override
        public boolean allowMissingMembersWhenPushingGroupToScim(ScrimEndPointConfiguration configuration) {
            return true;
        }

        @Override
        public boolean allowInvalidEndpointConfiguration() {
            return true;
        }

        @Override
        public boolean skipInvalidDataFromScimEndpoint(ScrimEndPointConfiguration configuration) {
            return true;
        }
    }
}
