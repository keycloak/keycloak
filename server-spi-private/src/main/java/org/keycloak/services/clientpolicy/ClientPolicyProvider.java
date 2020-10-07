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

package org.keycloak.services.clientpolicy;

import java.util.List;

import org.keycloak.provider.Provider;
import org.keycloak.services.clientpolicy.condition.ClientPolicyConditionProvider;
import org.keycloak.services.clientpolicy.executor.ClientPolicyExecutorProvider;

/**
 * Provides Client Policy which accommodates several Conditions and Executors.
 */
public interface ClientPolicyProvider extends Provider {

    /**
     * returns the list of conditions which this provider accommodates.
     *
     * @return list of conditions
     */
    List<ClientPolicyConditionProvider> getConditions();

    /**
     * returns the list of executors which this provider accommodates.
     *
     * @return list of executors
     */
    List<ClientPolicyExecutorProvider> getExecutors();

    String getName();

    String getProviderId();
}
