/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.services.clientpolicy.condition;

import org.keycloak.provider.Provider;
import org.keycloak.services.clientpolicy.ClientPolicyContext;
import org.keycloak.services.clientpolicy.ClientPolicyEvent;
import org.keycloak.services.clientpolicy.ClientPolicyException;
import org.keycloak.services.clientpolicy.ClientPolicyVote;

/**
 * This condition determines to which client a {@link ClientPolicyProvider} is adopted.
 * The condition can be evaluated on the events defined in {@link ClientPolicyEvent}.
 * It is sufficient for the implementer of this condition to implement methods in which they are interested
 * and {@link isEvaluatedOnEvent} method.
 */
public interface ClientPolicyConditionProvider extends Provider {

    @Override
    default void close() {
    }

    /**
     * returns ABSTAIN if this condition is not evaluated due to its nature.
     * returns YES if the client satisfies this condition on the event defined in {@link ClientPolicyEvent}.
     * If not, returns NO.
     *
     * @param context - the context of the event.
     * @return returns ABSTAIN if this condition is not evaluated due to its nature.
     * @throws {@link ClientPolicyException} - thrown if the condition is not evaluated in its nature on the event specified by context.
     */
    default ClientPolicyVote applyPolicy(ClientPolicyContext context) throws ClientPolicyException {
        return ClientPolicyVote.ABSTAIN;
    }

    String getName();

    String getProviderId();
}
