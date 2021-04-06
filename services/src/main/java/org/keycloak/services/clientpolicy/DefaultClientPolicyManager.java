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
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.representations.idm.ClientPoliciesRepresentation;
import org.keycloak.representations.idm.ClientProfileRepresentation;
import org.keycloak.representations.idm.ClientProfilesRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.services.clientpolicy.condition.ClientPolicyConditionProvider;
import org.keycloak.services.clientpolicy.executor.ClientPolicyExecutorProvider;

/**
 * @author <a href="mailto:takashi.norimatsu.ws@hitachi.com">Takashi Norimatsu</a>
 */
public class DefaultClientPolicyManager implements ClientPolicyManager {

    private static final Logger logger = Logger.getLogger(DefaultClientPolicyManager.class);

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
        logger.tracev("POLICY OPERATION :: context realm = {0}, event = {1}", realm.getName(), context.getEvent());

        doPolicyOperation(
                (ClientPolicyConditionProvider condition) -> condition.applyPolicy(context),
                (ClientPolicyExecutorProvider executor) -> executor.executeOnEvent(context),
                realm
        );
    }

    private void doPolicyOperation(ClientConditionOperation condition, ClientExecutorOperation executor, RealmModel realm) throws ClientPolicyException {
        Map<String, ClientProfileModel> map = ClientPoliciesUtil.getClientProfilesModel(session, realm);
        List<ClientPolicyModel> list = ClientPoliciesUtil.getEnabledClientProfilesModel(session, realm).stream().collect(Collectors.toList());

        if (list == null || list.isEmpty()) {
            logger.trace("POLICY OPERATION :: No enabled policy.");
            return;
        }

        for (ClientPolicyModel policy: list) {
            logger.tracev("POLICY OPERATION :: policy name = {0}, isBuiltin = {1}", policy.getName(), policy.isBuiltin());
            if (!isSatisfied(policy, condition)) {
                logger.tracev("POLICY UNSATISFIED :: policy name = {0}, isBuiltin = {1}", policy.getName(), policy.isBuiltin());
                continue;
            }

            logger.tracev("POLICY APPLIED :: policy name = {0}, isBuiltin = {1}", policy.getName(), policy.isBuiltin());
            execute(policy, executor, map);
        }
    }

    private boolean isSatisfied(
            ClientPolicyModel policy,
            ClientConditionOperation op) throws ClientPolicyException {

        if (policy.getConditions() == null || policy.getConditions().isEmpty()) {
            logger.tracev("NO CONDITION :: policy name = {0}", policy.getName());
            return false;
        }

        boolean ret = false;
        for (Object obj : policy.getConditions()) {
            ClientPolicyConditionProvider condition = (ClientPolicyConditionProvider)obj;
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
            ClientPolicyModel policy,
            ClientExecutorOperation op,
            Map<String, ClientProfileModel> map) throws ClientPolicyException {

        if (policy.getProfiles() == null || policy.getProfiles().isEmpty()) {
            logger.tracev("NO PROFILE :: policy name = {0}", policy.getName());
        }

        for (String profileName : policy.getProfiles()) {
            ClientProfileModel profile = map.get(profileName);
            if (profile == null) {
                logger.tracev("PROFILE NOT FOUND :: policy name = {0}, profile name = {1}", policy.getName(), profileName);
                continue;
            }

            if (profile.getExecutors() == null || profile.getExecutors().isEmpty()) {
                logger.tracev("PROFILE NO EXECUTOR :: policy name = {0}, profile name = {1}", policy.getName(), profileName);
                continue;
            }

            for (Object obj : profile.getExecutors()) {
                ClientPolicyExecutorProvider executor = (ClientPolicyExecutorProvider)obj;
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
        logger.trace("LOAD BUILTIN PROFILE POLICIES ON KEYCLOAK");

        // client profile can be referred from client policy so that client profile needs to be loaded at first.
        // load builtin profiles on keycloak app
        ClientProfilesRepresentation validatedProfilesRep = null;
        try {
            validatedProfilesRep = ClientPoliciesUtil.getValidatedBuiltinClientProfilesRepresentation(session, getClass().getResourceAsStream(profilesFilePath));
        } catch (ClientPolicyException cpe) {
            logger.warnv("LOAD BUILTIN PROFILES ON KEYCLOAK FAILED :: error = {0}, error detail = {1}", cpe.getError(), cpe.getErrorDetail());
            return;
        }

        String validatedJson = null;
        try {
            validatedJson = ClientPoliciesUtil.convertClientProfilesRepresentationToJson(validatedProfilesRep);
        } catch (ClientPolicyException cpe) {
            logger.warnv("VALIDATE SERIALIZE BUILTIN PROFILES ON KEYCLOAK FAILED :: error = {0}, error detail = {1}", cpe.getError(), cpe.getErrorDetail());
            return;
        }

        builtinClientProfilesJson = validatedJson;

        // load builtin policies on keycloak app
        ClientPoliciesRepresentation validatedPoliciesRep = null;
        try {
            validatedPoliciesRep = ClientPoliciesUtil.getValidatedBuiltinClientPoliciesRepresentation(session, getClass().getResourceAsStream(policiesFilePath));
        } catch (ClientPolicyException cpe) {
            logger.warnv("LOAD BUILTIN POLICIES ON KEYCLOAK FAILED :: error = {0}, error detail = {1}", cpe.getError(), cpe.getErrorDetail());
            builtinClientProfilesJson = null;
            return;
        }

        validatedJson = null;
        try {
            validatedJson = ClientPoliciesUtil.convertClientPoliciesRepresentationToJson(validatedPoliciesRep);
        } catch (ClientPolicyException cpe) {
            logger.warnv("VALIDATE SERIALIZE BUILTIN POLICIES ON KEYCLOAK FAILED :: error = {0}, error detail = {1}", cpe.getError(), cpe.getErrorDetail());
            builtinClientProfilesJson = null;
            return;
        }

        builtinClientPoliciesJson = validatedJson;
    }

    @Override
    public void setupClientPoliciesOnCreatedRealm(RealmModel realm) {
        logger.tracev("LOAD BUILTIN PROFILE POLICIES ON CREATED REALM :: realm = {0}", realm.getName());

        // put already loaded builtin profiles/policies on keycloak app to newly created realm
        setClientProfilesJsonString(realm, builtinClientProfilesJson);
        setClientPoliciesJsonString(realm, builtinClientPoliciesJson);
    }

    @Override
    public void setupClientPoliciesOnImportedRealm(RealmModel realm, RealmRepresentation rep) {
        logger.tracev("LOAD PROFILE POLICIES ON IMPORTED REALM :: realm = {0}", realm.getName());

        // put already loaded builtin profiles/policies on keycloak app to newly created realm
        setClientProfilesJsonString(realm, builtinClientProfilesJson);
        setClientPoliciesJsonString(realm, builtinClientPoliciesJson);

        // merge imported polices/profiles with builtin policies/profiles
        String validatedJson = null;
        try {
            validatedJson = ClientPoliciesUtil.getValidatedClientProfilesJson(session, realm, rep.getClientProfiles());
        } catch (ClientPolicyException e) {
            logger.warnv("VALIDATE SERIALIZE IMPORTED REALM PROFILES FAILED :: error = {0}, error detail = {1}", e.getError(), e.getErrorDetail());
            // revert to builtin profiles
            validatedJson = builtinClientProfilesJson;
        }
        setClientProfilesJsonString(realm, validatedJson);

        try {
            validatedJson = ClientPoliciesUtil.getValidatedClientPoliciesJson(session, realm, rep.getClientPolicies());
        } catch (ClientPolicyException e) {
            logger.warnv("VALIDATE SERIALIZE IMPORTED REALM POLICIES FAILED :: error = {0}, error detail = {1}", e.getError(), e.getErrorDetail());
            // revert to builtin profiles
            validatedJson = builtinClientPoliciesJson;
        }
        setClientPoliciesJsonString(realm, validatedJson);
    }

    @Override
    public void updateClientProfiles(RealmModel realm, String json) throws ClientPolicyException {
        logger.tracev("UPDATE PROFILES :: realm = {0}, PUT = {1}", realm.getName(), json);
        String validatedJsonString = null;
        try {
            validatedJsonString = getValidatedClientProfilesJson(realm, json);
        } catch (ClientPolicyException e) {
            logger.warnv("VALIDATE SERIALIZE PROFILES FAILED :: error = {0}, error detail = {1}", e.getError(), e.getErrorDetail());
            throw e;
        }
        setClientProfilesJsonString(realm, validatedJsonString);
        logger.tracev("UPDATE PROFILES :: realm = {0}, validated and modified PUT = {1}", realm.getName(), validatedJsonString);
    }

    @Override
    public String getClientProfiles(RealmModel realm) {
        String json = getClientProfilesJsonString(realm);
        logger.tracev("GET PROFILES :: realm = {0}, GET = {1}", realm.getName(), json);
        return json;
    }

    @Override
    public void updateClientPolicies(RealmModel realm, String json) throws ClientPolicyException {
        logger.tracev("UPDATE POLICIES :: realm = {0}, PUT = {1}", realm.getName(), json);
        String validatedJsonString = null;
        try {
            validatedJsonString = getValidatedClientPoliciesJson(realm, json);
        } catch (ClientPolicyException e) {
            logger.warnv("VALIDATE SERIALIZE POLICIES FAILED :: error = {0}, error detail = {1}", e.getError(), e.getErrorDetail());
            throw e;
        }
        setClientPoliciesJsonString(realm, validatedJsonString);
        logger.tracev("UPDATE POLICIES :: realm = {0}, validated and modified PUT = {1}", realm.getName(), validatedJsonString);
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
        logger.tracev("GET POLICIES :: realm = {0}, GET = {1}", realm.getName(), json);
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
        ClientProfilesRepresentation validatedProfilesRep = ClientPoliciesUtil.getValidatedClientProfilesRepresentation(session, realm, profilesJson);
        return ClientPoliciesUtil.convertClientProfilesRepresentationToJson(validatedProfilesRep);
    }

    private String getValidatedClientPoliciesJson(RealmModel realm, String policiesJson) throws ClientPolicyException {
        ClientPoliciesRepresentation validatedPoliciesRep = ClientPoliciesUtil.getValidatedClientPoliciesRepresentation(session, realm, policiesJson);
        return ClientPoliciesUtil.convertClientPoliciesRepresentationToJson(validatedPoliciesRep);
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
