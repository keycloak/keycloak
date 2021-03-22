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

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.jboss.logging.Logger;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.services.clientpolicy.ClientPolicyProvider;
import org.keycloak.services.clientpolicy.condition.ClientPolicyConditionProvider;
import org.keycloak.services.clientpolicy.executor.ClientPolicyExecutorProvider;

public class DefaultClientPolicyProvider implements ClientPolicyProvider {

    private static final Logger logger = Logger.getLogger(DefaultClientPolicyProvider.class);

    private final KeycloakSession session;
    private final ComponentModel componentModel;
    private final Map<String, List<ClientPolicyConditionProvider>> conditionsMap = new HashMap<>();
    private final Map<String, List<ClientPolicyExecutorProvider>> executorsMap = new HashMap<>();

    public DefaultClientPolicyProvider(KeycloakSession session, ComponentModel componentModel) {
        this.session = session;
        this.componentModel = componentModel;
    }
 
    @Override
    public void close() {
    }

    @Override
    public List<ClientPolicyConditionProvider> getConditions() {
        return getConditions(session.getContext().getRealm());
    }

    @Override
    public List<ClientPolicyExecutorProvider> getExecutors() {
        return getExecutors(session.getContext().getRealm());
    }

    @Override
    public String getName() {
        return componentModel.getName();
    }

    @Override
    public String getProviderId() {
        return componentModel.getProviderId();
    }

    private List<String> getConditionIds() {
        return componentModel.getConfig().getList(DefaultClientPolicyProviderFactory.CONDITION_IDS);
    }

    private List<String> getExecutorIds() {
        return componentModel.getConfig().getList(DefaultClientPolicyProviderFactory.EXECUTOR_IDS);
    }

    private List<ClientPolicyConditionProvider> getConditions(RealmModel realm) {
        List<ClientPolicyConditionProvider> providers = conditionsMap.get(realm.getId());
        if (providers == null) {
            providers = new LinkedList<>();
            List<String> conditionIds = getConditionIds();
            if (conditionIds == null || conditionIds.isEmpty()) return null;
            for(String conditionId : conditionIds) {
                ComponentModel cm = session.getContext().getRealm().getComponent(conditionId);
                try {
                    ClientPolicyConditionProvider provider = session.getProvider(ClientPolicyConditionProvider.class, cm);
                    providers.add(provider);
                    session.enlistForClose(provider);
                    ClientPolicyLogger.logv(logger, "Loaded Condition id = {0}, name = {1}, provider id = {2}", conditionId, cm.getName(), cm.getProviderId());
                } catch (Throwable t) {
                    logger.errorv(t, "Failed to load condition {0}", cm.getId());
                }
            }
            conditionsMap.put(realm.getId(), providers);
        } else {
            ClientPolicyLogger.log(logger, "Use cached conditions.");
        }
        return providers;
    }

    private List<ClientPolicyExecutorProvider> getExecutors(RealmModel realm) {
        List<ClientPolicyExecutorProvider> providers = executorsMap.get(realm.getId());
        if (providers == null) {
            providers = new LinkedList<>();
            List<String> executorIds = getExecutorIds();
            if (executorIds == null || executorIds.isEmpty()) return null;
            for(String executorId : executorIds) {
                ComponentModel cm = session.getContext().getRealm().getComponent(executorId);
                try {
                    ClientPolicyExecutorProvider provider = session.getProvider(ClientPolicyExecutorProvider.class, cm);
                    providers.add(provider);
                    session.enlistForClose(provider);
                    ClientPolicyLogger.logv(logger, "Loaded Executor id = {0}, name = {1}, provider id = {2}", executorId, cm.getName(), cm.getProviderId());
                } catch (Throwable t) {
                    logger.errorv(t, "Failed to load executor {0}", cm.getId());
                }
            }
            executorsMap.put(realm.getId(), providers);
        } else {
            ClientPolicyLogger.log(logger, "Use cached executors.");
        }
        return providers;
    }

}
