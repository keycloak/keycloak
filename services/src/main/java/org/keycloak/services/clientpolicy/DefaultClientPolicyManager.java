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
import org.keycloak.common.Profile;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.services.clientpolicy.ClientPolicyContext;
import org.keycloak.services.clientpolicy.ClientPolicyException;
import org.keycloak.services.clientpolicy.ClientPolicyManager;
import org.keycloak.services.clientpolicy.ClientPolicyProvider;
import org.keycloak.services.clientpolicy.condition.ClientPolicyConditionProvider;
import org.keycloak.services.clientpolicy.executor.ClientPolicyExecutorProvider;

public class DefaultClientPolicyManager implements ClientPolicyManager {

    private static final Logger logger = Logger.getLogger(DefaultClientPolicyManager.class);

    private final KeycloakSession session;
    private final Map<String, List<ClientPolicyProvider>> providersMap = new HashMap<>();

    public DefaultClientPolicyManager(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public void triggerOnEvent(ClientPolicyContext context) throws ClientPolicyException {
        if (!Profile.isFeatureEnabled(Profile.Feature.CLIENT_POLICIES)) return;
        ClientPolicyLogger.logv(logger, "Client Policy Operation : event = {0}", context.getEvent());
        doPolicyOperation(
                (ClientPolicyConditionProvider condition) -> condition.applyPolicy(context),
                (ClientPolicyExecutorProvider executor) -> executor.executeOnEvent(context)
            );
    }

    private void doPolicyOperation(ClientConditionOperation condition, ClientExecutorOperation executor) throws ClientPolicyException {
        RealmModel realm = session.getContext().getRealm();
        for (ClientPolicyProvider policy : getProviders(realm)) {
            ClientPolicyLogger.logv(logger, "Policy Operation : name = {0}, provider id = {1}", policy.getName(), policy.getProviderId());
            if (!isSatisfied(policy, condition)) continue;
            execute(policy, executor);
        }
    }

    private List<ClientPolicyProvider> getProviders(RealmModel realm) {
        List<ClientPolicyProvider> providers = providersMap.get(realm.getId());
        if (providers == null) {
            providers = new LinkedList<>();
            List<ComponentModel> policyModels = realm.getComponents(realm.getId(), ClientPolicyProvider.class.getName());
            for (ComponentModel policyModel : policyModels) {
                try {
                    ClientPolicyProvider policy = session.getProvider(ClientPolicyProvider.class, policyModel);
                    ClientPolicyLogger.logv(logger, "Loaded Policy Name = {0}", policyModel.getName());
                    session.enlistForClose(policy);
                    providers.add(policy);
                } catch (Throwable t) {
                    logger.errorv(t, "Failed to load provider {0}", policyModel.getId());
                }
            }
            providersMap.put(realm.getId(), providers);
        } else {
            ClientPolicyLogger.log(logger, "Use cached policies.");
        }
        return providers;
    }

    private boolean isSatisfied(
            ClientPolicyProvider policy,
            ClientConditionOperation op) throws ClientPolicyException {

        List<ClientPolicyConditionProvider> conditions = policy.getConditions();

        if (conditions == null || conditions.isEmpty()) {
            ClientPolicyLogger.log(logger, "NEGATIVE :: This policy is not applied. No condition exists.");
            return false;
        }

        boolean ret = false;
        for (ClientPolicyConditionProvider condition : conditions) {
            try {
                ClientPolicyVote vote = op.run(condition);
                if (vote == ClientPolicyVote.ABSTAIN) {
                    ClientPolicyLogger.logv(logger, "SKIP : This condition is not evaluated due to its nature. name = {0}, provider id = {1}", condition.getName(), condition.getProviderId());
                    continue;
                } else if (vote == ClientPolicyVote.NO) {
                    ClientPolicyLogger.logv(logger, "NEGATIVE :: This policy is not applied. condition not satisfied. name = {0}, provider id = {1}, ", condition.getName(), condition.getProviderId());
                    return false;
                }
                ret = true;
            } catch (ClientPolicyException cpe) {
                ClientPolicyLogger.logv(logger, "CONDITION EXCEPTION : name = {0}, provider id = {1}, error = {2}, error_detail = {3}", condition.getName(), condition.getProviderId(), cpe.getError(), cpe.getErrorDetail());
                throw cpe;
            }
        }

        if (ret == true) {
            ClientPolicyLogger.log(logger, "POSITIVE :: This policy is applied.");
        } else {
            ClientPolicyLogger.log(logger, "NEGATIVE :: This policy is not applied. No condition is evaluated.");
        }

        return ret;
 
    }

    private void execute(
            ClientPolicyProvider policy,
            ClientExecutorOperation op) throws ClientPolicyException {

        List<ClientPolicyExecutorProvider> executors = policy.getExecutors();
        if (executors == null || executors.isEmpty()) {
            ClientPolicyLogger.log(logger, "NEGATIVE :: This executor is not executed. No executor executable.");
            return;
        }
        for (ClientPolicyExecutorProvider executor : executors) {
            try {
                op.run(executor);
            } catch(ClientPolicyException cpe) {
                ClientPolicyLogger.logv(logger, "EXECUTOR EXCEPTION : name = {0}, provider id = {1}, error = {2}, error_detail = {3}", executor.getName(), executor.getProviderId(), cpe.getError(), cpe.getErrorDetail());
                throw cpe;
            }
        }

    }

    private interface ClientConditionOperation {
        ClientPolicyVote run(ClientPolicyConditionProvider condition) throws ClientPolicyException;
    }

    private interface ClientExecutorOperation {
        void run(ClientPolicyExecutorProvider executor) throws ClientPolicyException;
    }

}
