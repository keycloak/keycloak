/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates
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

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Supplier;

import org.jboss.logging.Logger;
import org.keycloak.common.Profile;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.representations.idm.ClientPoliciesRepresentation;
import org.keycloak.representations.idm.ClientProfileRepresentation;
import org.keycloak.representations.idm.ClientProfilesRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.services.clientpolicy.condition.ClientPolicyConditionProvider;
import org.keycloak.services.clientpolicy.executor.ClientPolicyExecutorProvider;
import org.keycloak.util.JsonSerialization;

/**
 * @author <a href="mailto:takashi.norimatsu.ws@hitachi.com">Takashi Norimatsu</a>
 */
public class DefaultClientPolicyManager implements ClientPolicyManager {

    private static final Logger logger = Logger.getLogger(DefaultClientPolicyManager.class);

    private final KeycloakSession session;
    private final Supplier<List<ClientProfileRepresentation>> globalClientProfilesSupplier;

    public DefaultClientPolicyManager(KeycloakSession session, Supplier<List<ClientProfileRepresentation>> globalClientProfilesSupplier) {
        this.session = session;
        this.globalClientProfilesSupplier = globalClientProfilesSupplier;
    }

    @Override
    public void triggerOnEvent(ClientPolicyContext context) throws ClientPolicyException {
        if (!Profile.isFeatureEnabled(Profile.Feature.CLIENT_POLICIES)) {
            return;
        }

        RealmModel realm = session.getContext().getRealm();
        logger.tracev("POLICY OPERATION :: context realm = {0}, event = {1}", realm.getName(), context.getEvent());

        doPolicyOperation(
                (ClientPolicyConditionProvider condition) -> condition.applyPolicy(context),
                (ClientPolicyExecutorProvider executor) -> executor.executeOnEvent(context),
                realm
        );
    }

    private void doPolicyOperation(ClientConditionOperation condition, ClientExecutorOperation executor, RealmModel realm) throws ClientPolicyException {
        List<ClientPolicy> list = ClientPoliciesUtil.getEnabledClientPolicies(session, realm);

        if (list == null || list.isEmpty()) {
            logger.trace("POLICY OPERATION :: No enabled policy.");
            return;
        }

        for (ClientPolicy policy: list) {
            logger.tracev("POLICY OPERATION :: policy name = {0}", policy.getName());
            if (!isSatisfied(policy, condition)) {
                logger.tracev("POLICY UNSATISFIED :: policy name = {0}", policy.getName());
                continue;
            }

            logger.tracev("POLICY APPLIED :: policy name = {0}", policy.getName());
            execute(policy, executor, realm);
        }
    }

    private boolean isSatisfied(
            ClientPolicy policy,
            ClientConditionOperation op) throws ClientPolicyException {

        if (policy.getConditions() == null || policy.getConditions().isEmpty()) {
            logger.tracev("NO CONDITION :: policy name = {0}", policy.getName());
            return false;
        }

        boolean ret = false;
        for (ClientPolicyConditionProvider condition : policy.getConditions()) {
            logger.tracev("CONDITION OPERATION :: policy name = {0}, condition name = {1}, provider id = {2}", policy.getName(), condition.getName(), condition.getProviderId());
            try {
                ClientPolicyVote vote = op.run(condition);
                if (condition.isNegativeLogic()) {
                    if (vote == ClientPolicyVote.YES) {
                        vote = ClientPolicyVote.NO;
                    } else if (vote == ClientPolicyVote.NO) {
                        vote = ClientPolicyVote.YES;
                    }
                }
                if (vote == ClientPolicyVote.ABSTAIN) {
                    logger.tracev("CONDITION SKIP :: policy name = {0}, condition name = {1}, provider id = {2}", policy.getName(), condition.getName(), condition.getProviderId());
                    continue;
                } else if (vote == ClientPolicyVote.NO) {
                    logger.tracev("CONDITION NEGATIVE :: policy name = {0}, condition name = {1}, provider id = {2}", policy.getName(), condition.getName(), condition.getProviderId());
                    return false;
                }
                ret = true;
            } catch (ClientPolicyException e) {
                logger.tracev("CONDITION EXCEPTION :: policy name = {0}, provider id = {1}, error = {2}, error detail = {3}", condition.getName(), condition.getProviderId(), e.getError(), e.getErrorDetail());
                throw e;
            }
        }

        if (ret == true) {
            logger.tracev("CONDITIONS SATISFIED :: policy name = {0}", policy.getName());
        } else {
            logger.tracev("CONDITIONS UNSATISFIED :: policy name = {0}", policy.getName());
        }

        return ret;
    }

    private void execute(
            ClientPolicy policy,
            ClientExecutorOperation op,
            RealmModel realm) throws ClientPolicyException {

        if (policy.getProfiles() == null || policy.getProfiles().isEmpty()) {
            logger.tracev("NO PROFILE :: policy name = {0}", policy.getName());
            return;
        }

        // Get profiles from realm
        ClientProfilesRepresentation clientProfiles =  ClientPoliciesUtil.getClientProfilesRepresentation(session, realm);

        for (String profileName : policy.getProfiles()) {
            ClientProfile profile = ClientPoliciesUtil.getClientProfileModel(session, realm, clientProfiles, globalClientProfilesSupplier.get(), profileName);
            if (profile == null) {
                logger.tracev("PROFILE NOT FOUND :: policy name = {0}, profile name = {1}", policy.getName(), profileName);
                continue;
            }

            if (profile.getExecutors() == null || profile.getExecutors().isEmpty()) {
                logger.tracev("PROFILE NO EXECUTOR :: policy name = {0}, profile name = {1}", policy.getName(), profileName);
                continue;
            }

            for (ClientPolicyExecutorProvider executor : profile.getExecutors()) {
                logger.tracev("EXECUTION :: policy name = {0}, profile name = {1}, executor name = {2}, provider id = {3}", policy.getName(), profileName, executor.getName(), executor.getProviderId());
                try {
                    op.run(executor);
                } catch(ClientPolicyException e) {
                    logger.tracev("EXECUTOR EXCEPTION :: executor name = {0}, provider id = {1}, error = {2}, error detail = {3}", executor.getName(), executor.getProviderId(), e.getError(), e.getErrorDetail());
                    throw e;
                }
            }

        }
    }

    private interface ClientConditionOperation {
        ClientPolicyVote run(ClientPolicyConditionProvider condition) throws ClientPolicyException;
    }

    private interface ClientExecutorOperation {
        void run(ClientPolicyExecutorProvider executor) throws ClientPolicyException;
    }

    @Override
    public void setupClientPoliciesOnCreatedRealm(RealmModel realm) {
        // For now, not create any create policies on the new realms. Administrator is supposed to add the policies if needed
    }

    @Override
    public void updateRealmModelFromRepresentation(RealmModel realm, RealmRepresentation rep) {
        logger.tracev("LOAD PROFILE POLICIES ON IMPORTED REALM :: realm = {0}", realm.getName());

        if (rep.getParsedClientProfiles() != null) {
            try {
                updateClientProfiles(realm, rep.getParsedClientProfiles());
            } catch (ClientPolicyException e) {
                logger.warnv("VALIDATE SERIALIZE IMPORTED REALM PROFILES FAILED :: error = {0}, error detail = {1}", e.getError(), e.getErrorDetail());
                throw new RuntimeException("Failed to update client profiles", e);
            }
        }

        ClientPoliciesRepresentation clientPolicies = rep.getParsedClientPolicies();
        if (clientPolicies != null) {
            try {
                updateClientPolicies(realm, clientPolicies);
            } catch (ClientPolicyException e) {
                logger.warnv("VALIDATE SERIALIZE IMPORTED REALM POLICIES FAILED :: error = {0}, error detail = {1}", e.getError(), e.getErrorDetail());
                throw new RuntimeException("Failed to update client policies", e);
            }
        } else {
            setupClientPoliciesOnCreatedRealm(realm);
        }
    }

    @Override
    public void updateClientProfiles(RealmModel realm, ClientProfilesRepresentation clientProfiles) throws ClientPolicyException {
        try {
            if (clientProfiles == null) {
                throw new ClientPolicyException("Passing null clientProfiles not allowed");
            }
            ClientProfilesRepresentation validatedProfilesRep = ClientPoliciesUtil.getValidatedClientProfilesForUpdate(session, realm, clientProfiles, globalClientProfilesSupplier.get());
            String validatedJsonString = ClientPoliciesUtil.convertClientProfilesRepresentationToJson(validatedProfilesRep);
            ClientPoliciesUtil.setClientProfilesJsonString(realm, validatedJsonString);
            logger.tracev("UPDATE PROFILES :: realm = {0}, validated and modified PUT = {1}", realm.getName(), validatedJsonString);
        } catch (ClientPolicyException e) {
            logger.warnv("VALIDATE SERIALIZE PROFILES FAILED :: error = {0}, error detail = {1}", e.getError(), e.getErrorDetail());
            throw e;
        }
    }

    @Override
    public ClientProfilesRepresentation getClientProfiles(RealmModel realm, boolean includeGlobalProfiles) throws ClientPolicyException {
        try {
            ClientProfilesRepresentation clientProfiles = ClientPoliciesUtil.getClientProfilesRepresentation(session, realm);
            if (includeGlobalProfiles) {
                clientProfiles.setGlobalProfiles(new LinkedList<>(globalClientProfilesSupplier.get()));
            }

            if (logger.isTraceEnabled()) {
                logger.tracev("GET PROFILES :: realm = {0}, GET = {1}", realm.getName(), JsonSerialization.writeValueAsString(clientProfiles));
            }

            return clientProfiles;
        } catch (ClientPolicyException e) {
            logger.warnv("GET CLIENT PROFILES FAILED :: error = {0}, error detail = {1}", e.getError(), e.getErrorDetail());
            throw e;
        } catch (IOException ioe) {
            throw new RuntimeException("Unexpected exception when converting JSON to String", ioe);
        }
    }

    @Override
    public void updateClientPolicies(RealmModel realm, ClientPoliciesRepresentation clientPolicies) throws ClientPolicyException {
        String validatedJsonString = null;
        try {
            if (clientPolicies == null) {
                throw new ClientPolicyException("Passing null clientPolicies not allowed");
            }
            ClientPoliciesRepresentation clientPoliciesRep = ClientPoliciesUtil.getValidatedClientPoliciesForUpdate(session, realm, clientPolicies, globalClientProfilesSupplier.get());
            validatedJsonString = ClientPoliciesUtil.convertClientPoliciesRepresentationToJson(clientPoliciesRep);
        } catch (ClientPolicyException e) {
            logger.warnv("VALIDATE SERIALIZE POLICIES FAILED :: error = {0}, error detail = {1}", e.getError(), e.getErrorDetail());
            throw e;
        }
        ClientPoliciesUtil.setClientPoliciesJsonString(realm, validatedJsonString);
        logger.tracev("UPDATE POLICIES :: realm = {0}, validated and modified PUT = {1}", realm.getName(), validatedJsonString);
    }

    @Override
    public ClientPoliciesRepresentation getClientPolicies(RealmModel realm) throws ClientPolicyException {
        try {
            ClientPoliciesRepresentation clientPolicies = ClientPoliciesUtil.getClientPoliciesRepresentation(session, realm);
            if (logger.isTraceEnabled()) {
                logger.tracev("GET POLICIES :: realm = {0}, GET = {1}", realm.getName(), JsonSerialization.writeValueAsString(clientPolicies));
            }
            return clientPolicies;
        } catch (ClientPolicyException e) {
            logger.warnv("GET CLIENT POLICIES FAILED :: error = {0}, error detail = {1}", e.getError(), e.getErrorDetail());
            throw e;
        } catch (IOException ioe) {
            throw new RuntimeException("Unexpected exception when converting JSON to String", ioe);
        }
    }

    @Override
    public void updateRealmRepresentationFromModel(RealmModel realm, RealmRepresentation rep) {
        try {
            // client profiles  that filter out global profiles..
            ClientProfilesRepresentation filteredOutProfiles = getClientProfiles(realm, false);
            rep.setParsedClientProfiles(filteredOutProfiles);

            ClientPoliciesRepresentation filteredOutPolicies = getClientPolicies(realm);
            rep.setParsedClientPolicies(filteredOutPolicies);
        } catch (ClientPolicyException cpe) {
            throw new IllegalStateException("Exception during export client profiles or client policies", cpe);
        }
    }

    @Override
    public void close() {
    }
}
