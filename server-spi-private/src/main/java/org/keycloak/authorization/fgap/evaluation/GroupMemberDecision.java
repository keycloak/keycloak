/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.authorization.fgap.evaluation;

import org.keycloak.authorization.Decision.Effect;
import org.keycloak.authorization.model.Policy;
import org.keycloak.authorization.permission.ResourcePermission;

/** Contribution of a group permission to a user operation after applying policy logic. */
public enum GroupMemberDecision {
    GRANT,
    NO_GRANT,
    EXPLICIT_DENY,
    // Invalid configuration or an undecidable partial result. Never invert it or treat it as a miss.
    INDETERMINATE;

    public static GroupMemberDecision of(ResourcePermission request, Policy policy, Effect effect) {
        return switch (effect) {
            case PERMIT -> GRANT;
            case INVALID -> INDETERMINATE;
            case DENY -> request.isExplicitDenial(policy, effect) ? EXPLICIT_DENY : NO_GRANT;
        };
    }

    public boolean blocksOtherGrants() {
        return this == EXPLICIT_DENY || this == INDETERMINATE;
    }

    /** Combine conditions on the same group and scope (unanimous). */
    public GroupMemberDecision and(GroupMemberDecision other) {
        if (this == INDETERMINATE || other == INDETERMINATE) {
            return INDETERMINATE;
        }
        if (this == EXPLICIT_DENY || other == EXPLICIT_DENY) {
            return EXPLICIT_DENY;
        }
        if (this == NO_GRANT || other == NO_GRANT) {
            return NO_GRANT;
        }
        return GRANT;
    }
}
