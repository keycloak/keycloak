package org.keycloak.federation.scim.core.exceptions;

import org.keycloak.federation.scim.core.ScimEndPointConfiguration;

public enum SkipOrStopApproach implements SkipOrStopStrategy {
    ALWAYS_SKIP_AND_CONTINUE {
        @Override
        public boolean allowPartialSynchronizationWhenPushingToScim(ScimEndPointConfiguration configuration) {
            return false;
        }

        @Override
        public boolean allowPartialSynchronizationWhenPullingFromScim(ScimEndPointConfiguration configuration) {
            return false;
        }

        @Override
        public boolean allowMissingMembersWhenPushingGroupToScim(ScimEndPointConfiguration configuration) {
            return false;
        }

        @Override
        public boolean allowInvalidEndpointConfiguration() {
            return false;
        }

        @Override
        public boolean skipInvalidDataFromScimEndpoint(ScimEndPointConfiguration configuration) {
            return false;
        }
    },
    ALWAYS_STOP {
        @Override
        public boolean allowPartialSynchronizationWhenPushingToScim(ScimEndPointConfiguration configuration) {
            return true;
        }

        @Override
        public boolean allowPartialSynchronizationWhenPullingFromScim(ScimEndPointConfiguration configuration) {
            return true;
        }

        @Override
        public boolean allowMissingMembersWhenPushingGroupToScim(ScimEndPointConfiguration configuration) {
            return true;
        }

        @Override
        public boolean allowInvalidEndpointConfiguration() {
            return true;
        }

        @Override
        public boolean skipInvalidDataFromScimEndpoint(ScimEndPointConfiguration configuration) {
            return true;
        }
    }
}
