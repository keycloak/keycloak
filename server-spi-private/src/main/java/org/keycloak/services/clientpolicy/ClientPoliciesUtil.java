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
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.jboss.logging.Logger;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ClientPolicyModel;
import org.keycloak.models.ClientProfileModel;
import org.keycloak.models.RealmModel;
import org.keycloak.representations.idm.ClientPoliciesRepresentation;
import org.keycloak.representations.idm.ClientPolicyRepresentation;
import org.keycloak.representations.idm.ClientProfileRepresentation;
import org.keycloak.representations.idm.ClientProfilesRepresentation;
import org.keycloak.services.clientpolicy.condition.ClientPolicyConditionProvider;
import org.keycloak.services.clientpolicy.executor.ClientPolicyExecutorProvider;
import org.keycloak.util.JsonSerialization;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ClientPoliciesUtil {

    private static final Logger logger = Logger.getLogger(ClientPoliciesUtil.class);

    private static final String LOGMSG_PREFIX = "CLIENT-POLICIES";

    /**
     * gets existing client profiles in a realm as representation.
     * not return null.
     */
    public static ClientProfilesRepresentation getClientProfilesRepresentation(KeycloakSession session, RealmModel realm) throws ClientPolicyException {
        ClientProfilesRepresentation profilesRep = null;
        String profilesJson = null;

        // get existing profiles json
        if (realm != null) {
            profilesJson = session.clientPolicy().getClientProfilesJsonString(realm);
        } else {
            // if realm not specified, use builtin profiles set in keycloak's binary.
            profilesJson = session.clientPolicy().getClientProfilesOnKeycloakApp();
        }

        // deserialize existing profiles (json -> representation)
        if (profilesJson == null) {
            return new ClientProfilesRepresentation();
        }
        try {
            profilesRep = convertClientProfilesJsonToRepresentation(profilesJson);
        } catch (ClientPolicyException e) {
            throw new ClientPolicyException(e.getError(), e.getErrorDetail());
        }
        if (profilesRep == null) {
            return new ClientProfilesRepresentation();
        }

        return profilesRep;
    }

    /**
     * gets existing client profiles in a realm as model.
     * not return null.
     */
    public static Map<String, ClientProfileModel> getClientProfilesModel(KeycloakSession session, RealmModel realm) {
        // get existing profiles as json
        String profilesJson = session.clientPolicy().getClientProfilesJsonString(realm);
        if (profilesJson == null) {
            return Collections.emptyMap();
        }

        // deserialize existing profiles (json -> representation)
        ClientProfilesRepresentation profilesRep = null;
        try {
            profilesRep = convertClientProfilesJsonToRepresentation(profilesJson);
        } catch (ClientPolicyException e) {
            logger.warnv("{0} Failed to serialize client profiles json string. err={1}, errDetail={2}", LOGMSG_PREFIX, e.getError(), e.getErrorDetail());
            return Collections.emptyMap();
        }
        if (profilesRep == null || profilesRep.getProfiles() == null) {
            return Collections.emptyMap();
        }

        // constructing existing profiles (representation -> model)
        Map<String, ClientProfileModel> profileMap = new HashMap<>();
        for (ClientProfileRepresentation profileRep : profilesRep.getProfiles()) {
            // ignore profile without name
            if (profileRep.getName() == null) {
                continue;
            }

            ClientProfileModel profileModel = new ClientProfileModel();
            profileModel.setName(profileRep.getName());
            profileModel.setDescription(profileRep.getDescription());
            if (profileRep.isBuiltin() != null) {
                profileModel.setBuiltin(profileRep.isBuiltin().booleanValue());
            } else {
                profileModel.setBuiltin(false);
            }

            if (profileRep.getExecutors() == null) {
                profileModel.setExecutors(new ArrayList<>());
                profileMap.put(profileRep.getName(), profileModel);
                continue;
            }

            List<Object> executors = new ArrayList<>();
            if (profileRep.getExecutors() != null) {
                profileRep.getExecutors().stream().forEach(obj->{
                    ObjectMapper mp = new ObjectMapper();
                    JsonNode node = mp.convertValue(obj, JsonNode.class);
                    node.fields().forEachRemaining(executor->{
                        ClientPolicyExecutorProvider provider = session.getProvider(ClientPolicyExecutorProvider.class, executor.getKey());
                        if (provider == null) {
                            // executor's provider not found. just skip it.
                            return;
                        }
                        provider.setupConfiguration(executor.getValue());
                        executors.add(provider);
                    });
                });
            }
            profileModel.setExecutors(executors);

            profileMap.put(profileRep.getName(), profileModel);
        }

        return profileMap;
    }

    /**
     * get validated and modified builtin client profiles set on keycloak app as representation.
     * it is loaded from json file enclosed in keycloak's binary.
     * not return null.
     */
    public static ClientProfilesRepresentation getValidatedBuiltinClientProfilesRepresentation(KeycloakSession session, InputStream is) throws ClientPolicyException {
        // load builtin client profiles representation
        ClientProfilesRepresentation proposedProfilesRep = null;
        try {
            proposedProfilesRep = JsonSerialization.readValue(is, ClientProfilesRepresentation.class);
        } catch (Exception e) {
            throw new ClientPolicyException("failed to deserialize builtin proposed client profiles json string.", e.getMessage());
        }
        if (proposedProfilesRep == null) {
            return new ClientProfilesRepresentation();
        }

        // no profile contained (it is valid)
        List<ClientProfileRepresentation> proposedProfileRepList = proposedProfilesRep.getProfiles();
        if (proposedProfileRepList == null || proposedProfileRepList.isEmpty()) {
            return new ClientProfilesRepresentation();
        }

        // duplicated profile name is not allowed.
        if (proposedProfileRepList.size() != proposedProfileRepList.stream().map(i->i.getName()).distinct().count()) {
            throw new ClientPolicyException("proposed builtin client profile name duplicated.", "NA");
        }

        // construct validated and modified profiles from builtin profiles in JSON file enclosed in keycloak binary.
        ClientProfilesRepresentation updatingProfilesRep = new ClientProfilesRepresentation();
        updatingProfilesRep.setProfiles(new ArrayList<>());
        List<ClientProfileRepresentation> updatingProfileList = updatingProfilesRep.getProfiles();

        for (ClientProfileRepresentation proposedProfileRep : proposedProfilesRep.getProfiles()) {
            if (proposedProfileRep.getName() == null) {
                continue;
            }

            // ignore proposed ordinal profile because builtin profile can only be added.
            if (proposedProfileRep.isBuiltin() == null || !proposedProfileRep.isBuiltin()) {
                logger.warnv("{0} ordinal profile included in proposed builtin profiles. Just ignore it. profile name = {1}", LOGMSG_PREFIX, proposedProfileRep.getName());
                continue;
            }

            ClientProfileRepresentation profileRep = new ClientProfileRepresentation();
            profileRep.setName(proposedProfileRep.getName());
            profileRep.setDescription(proposedProfileRep.getDescription());
            profileRep.setBuiltin(Boolean.TRUE);

            profileRep.setExecutors(new ArrayList<>()); // to prevent returning null
            if (proposedProfileRep.getExecutors() != null) {
                proposedProfileRep.getExecutors().stream().filter(executor->isValidExecutor(session, executor)).forEach(executor->profileRep.getExecutors().add(executor));
            }

            updatingProfileList.add(profileRep);
        }

        return updatingProfilesRep;
    }

    /**
     * converts client profiles as representation to json.
     * can return null.
     */
    public static String convertClientProfilesRepresentationToJson(ClientProfilesRepresentation reps) throws ClientPolicyException {
        return convertRepresentationToJson(reps);
    }

    /**
     * converts client profiles as json to representation.
     * not return null.
     */
    private static ClientProfilesRepresentation convertClientProfilesJsonToRepresentation(String json) throws ClientPolicyException {
        return convertJsonToRepresentation(json, ClientProfilesRepresentation.class);
    }

    /**
     * get validated and modified client profiles as json.
     * it can be constructed by merging proposed client profiles with existing client profiles.
     * can return null.
     */
    public static String getValidatedClientProfilesJson(KeycloakSession session, RealmModel realm, ClientProfilesRepresentation proposedProfilesRep) throws ClientPolicyException {
        return convertClientProfilesRepresentationToJson(getValidatedClientProfilesRepresentation(session, realm, proposedProfilesRep));
    }

    /**
     * get validated and modified client profiles as representation.
     * it can be constructed by merging proposed client profiles with existing client profiles.
     * not return null.
     */
    private static ClientProfilesRepresentation getValidatedClientProfilesRepresentation(KeycloakSession session, RealmModel realm, ClientProfilesRepresentation proposedProfilesRep) throws ClientPolicyException {
        if (proposedProfilesRep == null) {
            proposedProfilesRep = new ClientProfilesRepresentation();
        }
        if (realm == null) {
            throw new ClientPolicyException("realm not specified.", "NA");
        }

        // deserialize existing profiles (json -> representation)
        ClientProfilesRepresentation existingProfilesRep = null;
        String existingProfilesJson = session.clientPolicy().getClientProfilesJsonString(realm);
        if (existingProfilesJson != null) {
            try {
                existingProfilesRep = convertClientProfilesJsonToRepresentation(existingProfilesJson);
            } catch (ClientPolicyException e) {
                throw new ClientPolicyException(e.getError(), e.getErrorDetail());
            }
            if (existingProfilesRep == null) {
                existingProfilesRep = new ClientProfilesRepresentation();
            }
        } else {
            existingProfilesRep = new ClientProfilesRepresentation();
        }

        // no profile contained (it is valid)
        // back to initial builtin profiles
        List<ClientProfileRepresentation> proposedProfileRepList = proposedProfilesRep.getProfiles();
        if (proposedProfileRepList == null || proposedProfileRepList.isEmpty()) {
            proposedProfileRepList = new ArrayList<>();
            proposedProfilesRep.setProfiles(new ArrayList<>());
        }

        // duplicated profile name is not allowed.
        if (proposedProfileRepList.size() != proposedProfileRepList.stream().map(i->i.getName()).distinct().count()) {
            throw new ClientPolicyException("proposed client profile name duplicated.", "NA");
        }

        // construct updating profiles from existing profiles and proposed profiles
        ClientProfilesRepresentation updatingProfilesRep = new ClientProfilesRepresentation();
        updatingProfilesRep.setProfiles(new ArrayList<>());
        List<ClientProfileRepresentation> updatingProfileList = updatingProfilesRep.getProfiles();

        // add existing builtin profiles to updating profiles
        List<ClientProfileRepresentation> existingProfileList = existingProfilesRep.getProfiles();
        if (existingProfileList != null && !existingProfileList.isEmpty()) {
            existingProfileList.stream().filter(i->i.isBuiltin()).forEach(i->updatingProfileList.add(i));
        }

        for (ClientProfileRepresentation proposedProfileRep : proposedProfilesRep.getProfiles()) {
            if (proposedProfileRep.getName() == null) {
                continue;
            }

            // ignore proposed builtin profile because builtin profile cannot added/deleted/modified.
            if (proposedProfileRep.isBuiltin() != null && proposedProfileRep.isBuiltin()) {
                logger.warnv("{0} builtin profile included in proposed profiles. Just ignore it. profile name = {1}", LOGMSG_PREFIX, proposedProfileRep.getName());
                continue;
            }

            // basically, proposed profile totally overrides existing profile
            ClientProfileRepresentation profileRep = new ClientProfileRepresentation();
            profileRep.setName(proposedProfileRep.getName());
            profileRep.setDescription(proposedProfileRep.getDescription());
            profileRep.setBuiltin(Boolean.FALSE);
            profileRep.setExecutors(new ArrayList<>());
            if (proposedProfileRep.getExecutors() != null) {
                proposedProfileRep.getExecutors().stream().filter(executor->isValidExecutor(session, executor)).forEach(executor->profileRep.getExecutors().add(executor));
            }

            updatingProfileList.add(profileRep);
        }

        return updatingProfilesRep;
    }

    /**
     * get validated and modified builtin client profiles in a realm as representation.
     * it can be constructed by merging proposed client profiles with existing client profiles.
     * not return null.
     */
    public static ClientProfilesRepresentation getValidatedClientProfilesRepresentation(KeycloakSession session, RealmModel realm, String profilesJson) throws ClientPolicyException {
        if (profilesJson == null) {
            throw new ClientPolicyException("no client profiles json.", "NA");
        }

        // deserialize existing profiles (json -> representation)
        ClientProfilesRepresentation proposedProfilesRep = null;
        try {
            proposedProfilesRep = convertClientProfilesJsonToRepresentation(profilesJson);
        } catch (ClientPolicyException cpe) {
            throw new ClientPolicyException(cpe.getError(), cpe.getErrorDetail());
        }

        return getValidatedClientProfilesRepresentation(session, realm, proposedProfilesRep);
    }

    /**
     * check whether the propsed executor's provider can be found in keycloak's ClientPolicyExecutorProvider list.
     * not return null.
     */
    private static boolean isValidExecutor(KeycloakSession session, Object executor) {
        return isValidComponent(session, executor, "executor", (String providerId) -> {
            Set<String> providerSet = session.listProviderIds(ClientPolicyExecutorProvider.class);
            if (providerSet != null && providerSet.contains(providerId)) {
                return true;
            }
            logger.warnv("{0} no executor provider found. providerId = {1}", LOGMSG_PREFIX, providerId);
            return false;
        });
    }


    /**
     * gets existing client policies in a realm as representation.
     * not return null.
     */
    public static ClientPoliciesRepresentation getClientPoliciesRepresentation(KeycloakSession session, RealmModel realm) throws ClientPolicyException {
        ClientPoliciesRepresentation policiesRep = null;
        String policiesJson = null;

        // get existing policies json
        if (realm != null) {
            policiesJson = session.clientPolicy().getClientPoliciesJsonString(realm);
        } else {
            // if realm not specified, use builtin policies set in keycloak's binary.
            policiesJson = session.clientPolicy().getClientPoliciesOnKeycloakApp();
        }

        // deserialize existing policies (json -> representation)
        if (policiesJson == null) {
            // vacant reps
            return new ClientPoliciesRepresentation();
        }
        try {
            policiesRep = convertClientPoliciesJsonToRepresentation(policiesJson);
        } catch (ClientPolicyException e) {
            throw new ClientPolicyException(e.getError(), e.getErrorDetail());
        }
        if (policiesRep == null) {
            return new ClientPoliciesRepresentation();
        }

        return policiesRep;
    }

    /**
     * gets existing client policies in a realm as model.
     * not return null.
     */
    public static List<ClientPolicyModel> getClientPoliciesModel(KeycloakSession session, RealmModel realm) {
        // get existing profiles as json
        String policiesJson = session.clientPolicy().getClientPoliciesJsonString(realm);
        if (policiesJson == null) {
            return Collections.emptyList();
        }

        // deserialize existing policies (json -> representation)
        ClientPoliciesRepresentation policiesRep = null;
        try {
            policiesRep = convertClientPoliciesJsonToRepresentation(policiesJson);
        } catch (ClientPolicyException e) {
            logger.warnv("{0} Failed to serialize client policies json string. err={1}, errDetail={2}", LOGMSG_PREFIX, e.getError(), e.getErrorDetail());
            return Collections.emptyList();
        }
        if (policiesRep == null || policiesRep.getPolicies() == null) {
            return Collections.emptyList();
        }

        // constructing existing policies (representation -> model)
        List<ClientPolicyModel> policyList = new ArrayList<>();
        for (ClientPolicyRepresentation policyRep: policiesRep.getPolicies()) {
            // ignore policy without name
            if (policyRep.getName() == null) {
                continue;
            }

            ClientPolicyModel policyModel = new ClientPolicyModel();
            policyModel.setName(policyRep.getName());
            policyModel.setDescription(policyRep.getDescription());
            if (policyRep.isBuiltin() != null) {
                policyModel.setBuiltin(policyRep.isBuiltin().booleanValue());
            } else {
                policyModel.setBuiltin(false);
            }
            if (policyRep.isEnable() != null) {
                policyModel.setEnable(policyRep.isEnable().booleanValue());
            } else {
                policyModel.setEnable(false);
            }

            List<Object> conditions = new ArrayList<>();
            if (policyRep.getConditions() != null) {
                policyRep.getConditions().stream().forEach(obj->{
                    ObjectMapper mp = new ObjectMapper();
                    JsonNode node = mp.convertValue(obj, JsonNode.class);
                    node.fields().forEachRemaining(condition->{
                        ClientPolicyConditionProvider provider = session.getProvider(ClientPolicyConditionProvider.class, condition.getKey());
                        if (provider == null) {
                            // condition's provider not found. just skip it.
                            return;
                        }
                        provider.setupConfiguration(condition.getValue());
                        conditions.add(provider);
                    });
                });
            }
            policyModel.setConditions(conditions);

            if (policyRep.getProfiles() != null) {
                policyModel.setProfiles(policyRep.getProfiles().stream().collect(Collectors.toList()));
            }

            policyList.add(policyModel);
        }

        return policyList;
    }

    /**
     * get validated and modified builtin client policies set on keycloak app as representation.
     * it is loaded from json file enclosed in keycloak's binary.
     * not return null.
     */
    public static ClientPoliciesRepresentation getValidatedBuiltinClientPoliciesRepresentation(KeycloakSession session, InputStream is) throws ClientPolicyException {
        // load builtin client policies representation
        ClientPoliciesRepresentation proposedPoliciesRep = null;
        try {
            proposedPoliciesRep = JsonSerialization.readValue(is, ClientPoliciesRepresentation.class);
        } catch (Exception e) {
            throw new ClientPolicyException("failed to deserialize builtin proposed client policies json string.", e.getMessage());
        }
        if (proposedPoliciesRep == null) {
            proposedPoliciesRep = new ClientPoliciesRepresentation();
        }

        // no policy contained (it is valid)
        List<ClientPolicyRepresentation> proposedPolicyRepList = proposedPoliciesRep.getPolicies();
        if (proposedPolicyRepList == null || proposedPolicyRepList.isEmpty()) {
            return new ClientPoliciesRepresentation();
        }

        // duplicated policy name is not allowed.
        if (proposedPolicyRepList.size() != proposedPolicyRepList.stream().map(i->i.getName()).distinct().count()) {
            throw new ClientPolicyException("proposed builtin client policy name duplicated.", "NA");
        }

        // construct validated and modified policies from builtin profiles in JSON file enclosed in keycloak binary.
        ClientPoliciesRepresentation updatingPoliciesRep = new ClientPoliciesRepresentation();
        updatingPoliciesRep.setPolicies(new ArrayList<>());
        List<ClientPolicyRepresentation> updatingPoliciesList = updatingPoliciesRep.getPolicies();

        for (ClientPolicyRepresentation proposedPolicyRep : proposedPoliciesRep.getPolicies()) {
            if (proposedPolicyRep.getName() == null) {
                continue;
            }

            // ignore proposed ordinal policy because builtin policy can only be added.
            if (proposedPolicyRep.isBuiltin() == null || !proposedPolicyRep.isBuiltin()) {
                logger.warnv("{0} ordinal policy included in proposed builtin policies. Just ignore it. policy name = {1}", LOGMSG_PREFIX, proposedPolicyRep.getName());
                continue;
            }

            ClientPolicyRepresentation policyRep = new ClientPolicyRepresentation();
            policyRep.setName(proposedPolicyRep.getName());
            policyRep.setDescription(proposedPolicyRep.getDescription());
            policyRep.setBuiltin(Boolean.TRUE);
            Boolean enabled = (proposedPolicyRep.isEnable() != null) ? proposedPolicyRep.isEnable() : Boolean.FALSE;
            policyRep.setEnable(enabled);

            policyRep.setConditions(new ArrayList<>());
            if (proposedPolicyRep.getConditions() != null) {
                proposedPolicyRep.getConditions().stream().filter(condition->isValidCondition(session, condition)).forEach(condition->policyRep.getConditions().add(condition));
            }

            Set<String> existingProfileNames = new HashSet<>();
            ClientProfilesRepresentation reps = getClientProfilesRepresentation(session, null);
            reps.getProfiles().stream().map(profile->profile.getName()).forEach(profileName->existingProfileNames.add(profileName));
            policyRep.setProfiles(new ArrayList<>());
            if (proposedPolicyRep.getProfiles() != null) {
                proposedPolicyRep.getProfiles().stream().filter(profileName->existingProfileNames.contains(profileName)).distinct().forEach(profileName->policyRep.getProfiles().add(profileName));
            }

            updatingPoliciesList.add(policyRep);
        }

        return updatingPoliciesRep;
    }

    /**
     * converts client policies as representation to json.
     * can return null.
     */
    public static String convertClientPoliciesRepresentationToJson(ClientPoliciesRepresentation reps) throws ClientPolicyException {
        return convertRepresentationToJson(reps);
    }

    /**
     * converts client policies as json to representation.
     * not return null.
     */
    private static ClientPoliciesRepresentation convertClientPoliciesJsonToRepresentation(String json) throws ClientPolicyException {
        return convertJsonToRepresentation(json, ClientPoliciesRepresentation.class);
    }

    /**
     * get validated and modified client policies as json.
     * it can be constructed by merging proposed client policies with existing client policies.
     * can return null.
     */
    public static String getValidatedClientPoliciesJson(KeycloakSession session, RealmModel realm, ClientPoliciesRepresentation proposedPoliciesRep) throws ClientPolicyException {
        return convertClientPoliciesRepresentationToJson(getValidatedClientPoliciesRepresentation(session, realm, proposedPoliciesRep));
    }

    /**
     * get validated and modified client policies as representation.
     * it can be constructed by merging proposed client policies with existing client policies.
     * not return null.
     */
    private static ClientPoliciesRepresentation getValidatedClientPoliciesRepresentation(KeycloakSession session, RealmModel realm, ClientPoliciesRepresentation proposedPoliciesRep) throws ClientPolicyException {
        if (proposedPoliciesRep == null) {
            proposedPoliciesRep = new ClientPoliciesRepresentation();
        }
        if (realm == null) {
            throw new ClientPolicyException("realm not specified.", "NA");
        }

        // deserialize existing profiles (json -> represetation)
        ClientPoliciesRepresentation existingPoliciesRep = null;
        String existingPoliciesJson = session.clientPolicy().getClientPoliciesJsonString(realm);
        if (existingPoliciesJson != null) {
            try {
                existingPoliciesRep = convertClientPoliciesJsonToRepresentation(existingPoliciesJson);
            } catch (ClientPolicyException cpe) {
                throw new ClientPolicyException(cpe.getError(), cpe.getErrorDetail());
            }
            if (existingPoliciesRep == null) {
                existingPoliciesRep = new ClientPoliciesRepresentation();
            }
        } else {
            existingPoliciesRep = new ClientPoliciesRepresentation();
        }

        // no policy contained (it is valid)
        // back to initial builtin policies
        List<ClientPolicyRepresentation> proposedPolicyRepList = proposedPoliciesRep.getPolicies();
        if (proposedPolicyRepList == null || proposedPolicyRepList.isEmpty()) {
            proposedPolicyRepList = new ArrayList<>();
            proposedPoliciesRep.setPolicies(new ArrayList<>());
         }

        // duplicated policy name is not allowed.
        if (proposedPolicyRepList.size() != proposedPolicyRepList.stream().map(i->i.getName()).distinct().count()) {
            throw new ClientPolicyException("proposed client policy name duplicated.", "NA");
        }

        // construct updating policies from existing policies and proposed policies
        ClientPoliciesRepresentation updatingPoliciesRep = new ClientPoliciesRepresentation();
        updatingPoliciesRep.setPolicies(new ArrayList<>());
        List<ClientPolicyRepresentation> updatingPoliciesList = updatingPoliciesRep.getPolicies();

        // add existing builtin policies to updating policies
        List<ClientPolicyRepresentation> existingPoliciesList = existingPoliciesRep.getPolicies();
        if (existingPoliciesList != null && !existingPoliciesList.isEmpty()) {
            existingPoliciesList.stream().filter(i->i.isBuiltin()).forEach(i->updatingPoliciesList.add(i));
        }

        for (ClientPolicyRepresentation proposedPolicyRep : proposedPoliciesRep.getPolicies()) {
            if (proposedPolicyRep.getName() == null) {
                continue;
            }

            // ignore newly proposed builtin policy because builtin policy cannot added/deleted/modified.
            Boolean enabled = (proposedPolicyRep.isEnable() != null) ? proposedPolicyRep.isEnable() : Boolean.FALSE;
            if (proposedPolicyRep.isBuiltin() != null && proposedPolicyRep.isBuiltin()) {
                // only enable field of the existing builtin policy can be overridden.
                if (updatingPoliciesList.stream().anyMatch(i->i.getName().equals(proposedPolicyRep.getName()))) {
                    updatingPoliciesList.stream().filter(i->i.getName().equals(proposedPolicyRep.getName())).forEach(i->i.setEnable(enabled));
                    continue;
                }
                logger.warnv("{0} builtin policy included in proposed policies. Just ignore it. policy name = {1}", LOGMSG_PREFIX, proposedPolicyRep.getName());
                continue;
            }

            // basically, proposed policy totally overrides existing policy except for enabled field..
            ClientPolicyRepresentation policyRep = new ClientPolicyRepresentation();
            policyRep.setName(proposedPolicyRep.getName());
            policyRep.setDescription(proposedPolicyRep.getDescription());
            policyRep.setBuiltin(Boolean.FALSE);
            policyRep.setEnable(enabled);

            policyRep.setConditions(new ArrayList<>());
            if (proposedPolicyRep.getConditions() != null) {
                proposedPolicyRep.getConditions().stream().filter(condition->isValidCondition(session, condition)).forEach(condition->policyRep.getConditions().add(condition));
            }

            Set<String> existingProfileNames = new HashSet<>();
            ClientProfilesRepresentation reps = getClientProfilesRepresentation(session, realm);
            if (reps.getProfiles() != null) {
                reps.getProfiles().stream().map(profile->profile.getName()).forEach(profileName->existingProfileNames.add(profileName));
            }
            policyRep.setProfiles(new ArrayList<>());
            if (proposedPolicyRep.getProfiles() != null) {
                proposedPolicyRep.getProfiles().stream().filter(profileName->existingProfileNames.contains(profileName)).distinct().forEach(profileName->policyRep.getProfiles().add(profileName));
            }

            updatingPoliciesList.add(policyRep);
        }

        return updatingPoliciesRep;
    }

    /**
     * get validated and modified builtin client policies in a realm as representation.
     * it can be constructed by merging proposed client policies with existing client policies.
     * not return null.
     */
    public static ClientPoliciesRepresentation getValidatedClientPoliciesRepresentation(KeycloakSession session, RealmModel realm, String policiesJson) throws ClientPolicyException {
        if (policiesJson == null) {
            throw new ClientPolicyException("no client policies json.", "NA");
        }

        // deserialize existing policies (json -> representation)
        ClientPoliciesRepresentation proposedPoliciesRep = null;
        try {
            proposedPoliciesRep = convertClientPoliciesJsonToRepresentation(policiesJson);
        } catch (ClientPolicyException cpe) {
            throw new ClientPolicyException(cpe.getError(), cpe.getErrorDetail());
        }

        return getValidatedClientPoliciesRepresentation(session, realm, proposedPoliciesRep);
    }

    /**
     * check whether the propsed condition's provider can be found in keycloak's ClientPolicyConditionProvider list.
     * not return null.
     */
    private static boolean isValidCondition(KeycloakSession session, Object condition) {
        return isValidComponent(session, condition, "condition", (String providerId) -> {
            Set<String> providerSet = session.listProviderIds(ClientPolicyConditionProvider.class);
            if (providerSet != null && providerSet.contains(providerId)) {
                return true;
            }
            logger.warnv("{0} no executor provider found. providerId = {1}", LOGMSG_PREFIX, providerId);
            return false;
        });
    }


    private static boolean isValidComponent(KeycloakSession session, Object obj, String type, Predicate<String> f) {
        ObjectMapper mp = new ObjectMapper();
        JsonNode node = null;

        try {
            node = mp.convertValue(obj, JsonNode.class);
        } catch (IllegalArgumentException iae) {
            logger.warnv("{0} invalid json string representating {1}. err={2}", LOGMSG_PREFIX, type, iae.getMessage());
            return false;
        }

        Iterator<Entry<String, JsonNode>> it = node.fields();
        while (it.hasNext()) {
            Entry<String, JsonNode> entry = it.next();
            // whether find provider
            if(!f.test(entry.getKey())) return false;
        }
        return true;
    }

    private static String convertRepresentationToJson(Object reps) throws ClientPolicyException {
        if (reps == null) return null;

        ObjectMapper mapper = new ObjectMapper();
        String json = null;
        try {
            json = mapper.writeValueAsString(reps);
        } catch (JsonProcessingException jpe) {
            throw new ClientPolicyException(jpe.getMessage(), "NA");
        }

        return json;
    }

    private static <T> T convertJsonToRepresentation(String json, Class<T> type) throws ClientPolicyException {
        if (json == null) {
            throw new ClientPolicyException("no json.", "NA");
        }

        T rep = null;
        try {
            rep = JsonSerialization.readValue(json, type);
        } catch (IOException ioe) {
            throw new ClientPolicyException("failed to deserialize.", ioe.getMessage());
        }

        return rep;
    }

}
