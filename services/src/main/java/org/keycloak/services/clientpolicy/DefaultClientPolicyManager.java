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

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.jboss.logging.Logger;
import org.keycloak.common.Profile;
import org.keycloak.models.ClientPolicyModel;
import org.keycloak.models.ClientProfileModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.representations.idm.ClientPoliciesRepresentation;
import org.keycloak.representations.idm.ClientProfileRepresentation;
import org.keycloak.representations.idm.ClientProfilesRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.services.clientpolicy.condition.ClientPolicyConditionProvider;
import org.keycloak.services.clientpolicy.executor.ClientPolicyExecutorProvider;

public class DefaultClientPolicyManager implements ClientPolicyManager {

    // log format CLIENT-POLICY@session.hashCode() :: [event] :: [detail]
    private static final Logger logger = Logger.getLogger(DefaultClientPolicyManager.class);
    private static final String LOGMSG_PREFIX = "CLIENT-POLICY";
    private String logMsgPrefix() {
        return LOGMSG_PREFIX + "@" + session.hashCode();
    }

    private final KeycloakSession session;

    public DefaultClientPolicyManager(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public void triggerOnEvent(ClientPolicyContext context) throws ClientPolicyException {
        if (!Profile.isFeatureEnabled(Profile.Feature.CLIENT_POLICIES)) {
            return;
        }

        RealmModel realm = session.getContext().getRealm();
        ClientPolicyLogger.logv(logger, "{0} :: POLICY OPERATION :: context realm = {1}, event = {2}", logMsgPrefix(), realm.getName(), context.getEvent());

        doPolicyOperation(
                (ClientPolicyConditionProvider condition) -> condition.applyPolicy(context),
                (ClientPolicyExecutorProvider executor) -> executor.executeOnEvent(context),
                realm
        );
    }

    private void doPolicyOperation(ClientConditionOperation condition, ClientExecutorOperation executor, RealmModel realm) throws ClientPolicyException {
        Map<String, ClientProfileModel> map = ClientPoliciesUtil.getClientProfilesModel(session, realm);
        List<ClientPolicyModel> list = ClientPoliciesUtil.getClientPoliciesModel(session, realm).stream()
                .filter(ClientPolicyModel::isEnable)
                .collect(Collectors.toList());

        if (list == null || list.isEmpty()) {
            ClientPolicyLogger.logv(logger, "{0} :: POLICY OPERATION :: No enabled policy.", logMsgPrefix());
            return;
        }

        for (ClientPolicyModel policy: list) {
            ClientPolicyLogger.logv(logger, "{0} :: POLICY OPERATION :: policy name = {1}, isBuiltin = {2}", logMsgPrefix(), policy.getName(), policy.isBuiltin());
            if (!isSatisfied(policy, condition)) {
                ClientPolicyLogger.logv(logger, "{0} :: POLICY UNSATISFIED :: policy name = {1}, isBuiltin = {2}", logMsgPrefix(), policy.getName(), policy.isBuiltin());
                continue;
            }

            ClientPolicyLogger.logv(logger, "{0} :: POLICY APPLIED :: policy name = {1}, isBuiltin = {2}", logMsgPrefix(), policy.getName(), policy.isBuiltin());
            execute(policy, executor, map);
        }
    }

    private boolean isSatisfied(
            ClientPolicyModel policy,
            ClientConditionOperation op) throws ClientPolicyException {

        if (policy.getConditions() == null || policy.getConditions().isEmpty()) {
            ClientPolicyLogger.logv(logger, "{0} :: NO CONDITION :: policy name = {1}", logMsgPrefix(), policy.getName());
            return false;
        }

        boolean ret = false;
        for (Object obj : policy.getConditions()) {
            ClientPolicyConditionProvider condition = (ClientPolicyConditionProvider)obj;
            ClientPolicyLogger.logv(logger, "{0} :: CONDITION OPERATION :: policy name = {1}, condition name = {2}, provider id = {3}", logMsgPrefix(), policy.getName(), condition.getName(), condition.getProviderId());
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
                    ClientPolicyLogger.logv(logger, "{0} :: CONDITION SKIP :: policy name = {1}, condition name = {2}, provider id = {3}", logMsgPrefix(), policy.getName(), condition.getName(), condition.getProviderId());
                    continue;
                } else if (vote == ClientPolicyVote.NO) {
                    ClientPolicyLogger.logv(logger, "{0} :: CONDITION NEGATIVE :: policy name = {1}, condition name = {2}, provider id = {3}", logMsgPrefix(), policy.getName(), condition.getName(), condition.getProviderId());
                    return false;
                }
                ret = true;
            } catch (ClientPolicyException e) {
                ClientPolicyLogger.logv(logger, "{0} :: CONDITION EXCEPTION :: policy name = {1}, provider id = {2}, error = {3}, error detail = {4}", logMsgPrefix(), condition.getName(), condition.getProviderId(), e.getError(), e.getErrorDetail());
                throw e;
            }
        }

        if (ret == true) {
            ClientPolicyLogger.logv(logger, "{0} :: CONDITIONS SATISFIED :: policy name = {1}", logMsgPrefix(), policy.getName());
        } else {
            ClientPolicyLogger.logv(logger, "{0} :: CONDITIONS UNSATISFIED :: policy name = {1}", logMsgPrefix(), policy.getName());
        }

        return ret;
    }

    private void execute(
            ClientPolicyModel policy,
            ClientExecutorOperation op,
            Map<String, ClientProfileModel> map) throws ClientPolicyException {

        if (policy.getProfiles() == null || policy.getProfiles().isEmpty()) {
            ClientPolicyLogger.logv(logger, "{0} :: NO PROFILE :: policy name = {1}", logMsgPrefix(), policy.getName());
        }

        for (String profileName : policy.getProfiles()) {
            ClientProfileModel profile = map.get(profileName);
            if (profile == null) {
                ClientPolicyLogger.logv(logger, "{0} :: PROFILE NOT FOUND :: policy name = {1}, profile name = {2}", logMsgPrefix(), policy.getName(), profileName);
                continue;
            }

            if (profile.getExecutors() == null || profile.getExecutors().isEmpty()) {
                ClientPolicyLogger.logv(logger, "{0} :: PROFILE NO EXECUTOR :: policy name = {1}, profile name = {2}", logMsgPrefix(), policy.getName(), profileName);
                continue;
            }

            for (Object obj : profile.getExecutors()) {
                ClientPolicyExecutorProvider executor = (ClientPolicyExecutorProvider)obj;
                ClientPolicyLogger.logv(logger, "{0} :: EXECUTION :: policy name = {1}, profile name = {2}, executor name = {3}, provider id = {4}", logMsgPrefix(), policy.getName(), profileName, executor.getName(), executor.getProviderId());
                try {
                    op.run(executor);
                } catch(ClientPolicyException e) {
                    ClientPolicyLogger.logv(logger, "{0} :: EXECUTOR EXCEPTION :: executor name = {1}, provider id = {2}, error = {3}, error detail = {4}", logMsgPrefix(), executor.getName(), executor.getProviderId(), e.getError(), e.getErrorDetail());
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


    // Client Polices Realm Attributes Keys
    public static final String CLIENT_PROFILES = "client-policies.profiles";
    public static final String CLIENT_POLICIES = "client-policies.policies";

    // builtin profiles and policies are loaded on booting keycloak at once.
    // therefore, their representations are kept and remain unchanged.
    // these are shared among all realms.

    // those can be null to show that no profile/policy exist
    private static String builtinClientProfilesJson;
    private static String builtinClientPoliciesJson;

    @Override
    public void setupClientPoliciesOnKeycloakApp(String profilesFilePath, String policiesFilePath) {
        ClientPolicyLogger.logv(logger, "{0} :: LOAD BUILTIN PROFILE POLICIES ON KEYCLOAK", logMsgPrefix());

        // client profile can be referred from client policy so that client profile needs to be loaded at first.
        // load builtin profiles on keycloak app
        ClientProfilesRepresentation validatedProfilesRep = null;
        try {
            validatedProfilesRep = ClientPoliciesUtil.getValidatedBuiltinClientProfilesRepresentation(session, getClass().getResourceAsStream(profilesFilePath));
        } catch (ClientPolicyException e) {
            logger.warnv("{0} :: LOAD BUILTIN PROFILES ON KEYCLOAK FAILED :: error = {1}, error detail = {2}", LOGMSG_PREFIX, e.getError(), e.getErrorDetail());
            return;
        }

        String validatedJson = null;
        try {
            validatedJson = ClientPoliciesUtil.convertClientProfilesRepresentationToJson(validatedProfilesRep);
        } catch (ClientPolicyException cpe) {
            logger.warnv("{0} :: VALIDATE SERIALIZE BUILTIN PROFILES ON KEYCLOAK FAILED :: error = {1}, error detail = {2}", LOGMSG_PREFIX, cpe.getError(), cpe.getErrorDetail());
            return;
        }

        builtinClientProfilesJson = validatedJson;

        // load builtin policies on keycloak app
        ClientPoliciesRepresentation validatedPoliciesRep = null;
        try {
            validatedPoliciesRep = ClientPoliciesUtil.getValidatedBuiltinClientPoliciesRepresentation(session, getClass().getResourceAsStream(policiesFilePath));
        } catch (ClientPolicyException cpe) {
            logger.warnv("{0} :: LOAD BUILTIN POLICIES ON KEYCLOAK FAILED :: error = {1}, error detail = {2}", LOGMSG_PREFIX, cpe.getError(), cpe.getErrorDetail());
            builtinClientProfilesJson = null;
            return;
        }

        validatedJson = null;
        try {
            validatedJson = ClientPoliciesUtil.convertClientPoliciesRepresentationToJson(validatedPoliciesRep);
        } catch (ClientPolicyException cpe) {
            logger.warnv("{0} :: VALIDATE SERIALIZE BUILTIN POLICIES ON KEYCLOAK FAILED :: error = {1}, error detail = {2}", LOGMSG_PREFIX, cpe.getError(), cpe.getErrorDetail());
            builtinClientProfilesJson = null;
            return;
        }

        builtinClientPoliciesJson = validatedJson;
    }

    @Override
    public void setupClientPoliciesOnCreatedRealm(RealmModel realm) {
        ClientPolicyLogger.logv(logger, "{0} :: LOAD BUILTIN PROFILE POLICIES ON CREATED REALM :: realm = {1}", logMsgPrefix(), realm.getName());

        // put already loaded builtin profiles/policies on keycloak app to newly created realm
        setClientProfilesJsonString(realm, builtinClientProfilesJson);
        setClientPoliciesJsonString(realm, builtinClientPoliciesJson);
    }

    @Override
    public void setupClientPoliciesOnImportedRealm(RealmModel realm, RealmRepresentation rep) {
        ClientPolicyLogger.logv(logger, "{0} :: LOAD PROFILE POLICIES ON IMPORTED REALM :: realm = {1}", logMsgPrefix(), realm.getName());

        // put already loaded builtin profiles/policies on keycloak app to newly created realm
        setClientProfilesJsonString(realm, builtinClientProfilesJson);
        setClientPoliciesJsonString(realm, builtinClientPoliciesJson);

        // merge imported polices/profiles with builtin policies/profiles
        String validatedJson = null;
        try {
            validatedJson = ClientPoliciesUtil.getValidatedClientProfilesJson(session, realm, rep.getClientProfiles());
        } catch (ClientPolicyException e) {
            logger.warnv("{0} :: VALIDATE SERIALIZE IMPORTED REALM PROFILES FAILED :: error = {1}, error detail = {2}", LOGMSG_PREFIX, e.getError(), e.getErrorDetail());
            // revert to builtin profiles
            validatedJson = builtinClientProfilesJson;
        }
        setClientProfilesJsonString(realm, validatedJson);

        try {
            validatedJson = ClientPoliciesUtil.getValidatedClientPoliciesJson(session, realm, rep.getClientPolicies());
        } catch (ClientPolicyException e) {
            logger.warnv("{0} :: VALIDATE SERIALIZE IMPORTED REALM POLICIES FAILED :: error = {1}, error detail = {2}", LOGMSG_PREFIX, e.getError(), e.getErrorDetail());
            // revert to builtin profiles
            validatedJson = builtinClientPoliciesJson;
        }
        setClientPoliciesJsonString(realm, validatedJson);
    }

    @Override
    public void updateClientProfiles(RealmModel realm, String json) throws ClientPolicyException {
        ClientPolicyLogger.logv(logger, "{0} :: UPDATE PROFILES :: realm = {1}, PUT = {2}", logMsgPrefix(), realm.getName(), json);
        String validatedJsonString = null;
        try {
            validatedJsonString = getValidatedClientProfilesJson(realm, json);
        } catch (ClientPolicyException e) {
            logger.warnv("{0} :: VALIDATE SERIALIZE PROFILES FAILED :: error = {1}, error detail = {2}", LOGMSG_PREFIX, e.getError(), e.getErrorDetail());
            throw e;
        }
        setClientProfilesJsonString(realm, validatedJsonString);
        ClientPolicyLogger.logv(logger, "{0} :: UPDATE PROFILES :: realm = {1}, validated and modified PUT = {2}", logMsgPrefix(), realm.getName(), validatedJsonString);
    }

    @Override
    public String getClientProfiles(RealmModel realm) {
        String json = getClientProfilesJsonString(realm);
        ClientPolicyLogger.logv(logger, "{0} :: GET PROFILES :: realm = {1}, GET = {2}", logMsgPrefix(), realm.getName(), json);
        return json;
    }

    @Override
    public void updateClientPolicies(RealmModel realm, String json) throws ClientPolicyException {
        ClientPolicyLogger.logv(logger, "{0} :: UPDATE POLICIES :: realm = {1}, PUT = {2}", logMsgPrefix(), realm.getName(), json);
        String validatedJsonString = null;
        try {
            validatedJsonString = getValidatedClientPoliciesJson(realm, json);
        } catch (ClientPolicyException e) {
            logger.warnv("{0} :: VALIDATE SERIALIZE POLICIES FAILED :: error = {1}, error detail = {2}", LOGMSG_PREFIX, e.getError(), e.getErrorDetail());
            throw e;
        }
        setClientPoliciesJsonString(realm, validatedJsonString);
        ClientPolicyLogger.logv(logger, "{0} :: UPDATE POLICIES :: realm = {1}, validated and modified PUT = {2}", logMsgPrefix(), realm.getName(), validatedJsonString);
    }

    @Override
    public void setupClientPoliciesOnExportingRealm(RealmModel realm, RealmRepresentation rep) {
        // client profiles  that filter out builtin profiles..
        ClientProfilesRepresentation filteredOutProfiles = null;
        try {
            filteredOutProfiles = getClientProfilesForExport(realm);
        } catch (ClientPolicyException e) {
            // set as null
        }
        rep.setClientProfiles(filteredOutProfiles);

        // client policies that filter out builtin and policies.
        ClientPoliciesRepresentation filteredOutPolicies = null;
        try {
            filteredOutPolicies = getClientPoliciesForExport(realm);
        } catch (ClientPolicyException e) {
            // set as null
        }
        rep.setClientPolicies(filteredOutPolicies);
    }

    @Override
    public String getClientPolicies(RealmModel realm) {
        String json = getClientPoliciesJsonString(realm);
        ClientPolicyLogger.logv(logger, "{0} :: GET POLICIES :: realm = {1}, GET = {2}", logMsgPrefix(), realm.getName(), json);
        return json;
    }

    @Override
    public String getClientProfilesOnKeycloakApp() {
        return builtinClientProfilesJson;
    }

    @Override
    public String getClientPoliciesOnKeycloakApp() {
        return builtinClientPoliciesJson;
    }

    @Override
    public String getClientProfilesJsonString(RealmModel realm) {
        return realm.getAttribute(CLIENT_PROFILES);
    }

    @Override
    public String getClientPoliciesJsonString(RealmModel realm) {
        return realm.getAttribute(CLIENT_POLICIES);
    }

    private void setClientProfilesJsonString(RealmModel realm, String json) {
        realm.setAttribute(CLIENT_PROFILES, json);
    }

    private void setClientPoliciesJsonString(RealmModel realm, String json) {
        realm.setAttribute(CLIENT_POLICIES, json);
    }

    private String getValidatedClientProfilesJson(RealmModel realm, String profilesJson) throws ClientPolicyException {
        ClientProfilesRepresentation validatedProfilesRep = null;
        try {
            validatedProfilesRep = ClientPoliciesUtil.getValidatedClientProfilesRepresentation(session, realm, profilesJson);
        } catch (ClientPolicyException e) {
            throw new ClientPolicyException(e.getError(), e.getErrorDetail());
        }

        String validatedJson = null;
        try {
            validatedJson = ClientPoliciesUtil.convertClientProfilesRepresentationToJson(validatedProfilesRep);
        } catch (ClientPolicyException e) {
            throw new ClientPolicyException(e.getError(), e.getErrorDetail());
        }

        return validatedJson;
    }

    private String getValidatedClientPoliciesJson(RealmModel realm, String policiesJson) throws ClientPolicyException {
        ClientPoliciesRepresentation validatedPoliciesRep = null;
        try {
            validatedPoliciesRep = ClientPoliciesUtil.getValidatedClientPoliciesRepresentation(session, realm, policiesJson);
        } catch (ClientPolicyException e) {
            throw new ClientPolicyException(e.getError(), e.getErrorDetail());
        }

        String validatedJson = null;
        try {
            validatedJson = ClientPoliciesUtil.convertClientPoliciesRepresentationToJson(validatedPoliciesRep);
        } catch (ClientPolicyException e) {
            throw new ClientPolicyException(e.getError(), e.getErrorDetail());
        }

        return validatedJson;
    }

    /**
     * not return null
     */
    private ClientProfilesRepresentation getClientProfilesForExport(RealmModel realm) throws ClientPolicyException {
        ClientProfilesRepresentation profilesRep = ClientPoliciesUtil.getClientProfilesRepresentation(session, realm);
        if (profilesRep == null || profilesRep.getProfiles() == null) {
            return new ClientProfilesRepresentation();
        }

        // not export builtin profiles
        List<ClientProfileRepresentation> filteredProfileRepList = profilesRep.getProfiles().stream().filter(profileRep->!profileRep.isBuiltin()).collect(Collectors.toList());
        profilesRep.setProfiles(filteredProfileRepList);
        return profilesRep;
    }

    /**
     * not return null
     */
    private ClientPoliciesRepresentation getClientPoliciesForExport(RealmModel realm) throws ClientPolicyException {
        ClientPoliciesRepresentation policiesRep = ClientPoliciesUtil.getClientPoliciesRepresentation(session, realm);
        if (policiesRep == null || policiesRep.getPolicies() == null) {
            return new ClientPoliciesRepresentation();
        }

        policiesRep.getPolicies().stream().forEach(policyRep->{
            if (policyRep.isBuiltin()) {
                // only keeps name, builtin and enabled fields.
                policyRep.setDescription(null);
                policyRep.setConditions(null);
                policyRep.setProfiles(null);
            }
        });
        return policiesRep;
    }
}
