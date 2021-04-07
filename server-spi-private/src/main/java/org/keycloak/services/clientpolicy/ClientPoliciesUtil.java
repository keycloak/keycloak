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
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.jboss.logging.Logger;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.representations.idm.ClientPoliciesRepresentation;
import org.keycloak.representations.idm.ClientPolicyConditionRepresentation;
import org.keycloak.representations.idm.ClientPolicyExecutorRepresentation;
import org.keycloak.representations.idm.ClientPolicyRepresentation;
import org.keycloak.representations.idm.ClientProfileRepresentation;
import org.keycloak.representations.idm.ClientProfilesRepresentation;
import org.keycloak.representations.idm.ClientPolicyConditionConfigurationRepresentation;
import org.keycloak.services.clientpolicy.condition.ClientPolicyConditionProvider;
import org.keycloak.representations.idm.ClientPolicyExecutorConfigurationRepresentation;
import org.keycloak.services.clientpolicy.executor.ClientPolicyExecutorProvider;
import org.keycloak.util.JsonSerialization;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Utilities for treating client policies/profiles
 *
 * @author <a href="mailto:takashi.norimatsu.ws@hitachi.com">Takashi Norimatsu</a>
 */
public class ClientPoliciesUtil {

    private static final Logger logger = Logger.getLogger(ClientPoliciesUtil.class);

    private static final ObjectMapper objectMapper = new ObjectMapper();

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
        profilesRep = convertClientProfilesJsonToRepresentation(profilesJson);
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
            logger.warnv("Failed to serialize client profiles json string. err={0}, errDetail={1}", e.getError(), e.getErrorDetail());
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

            List<ClientPolicyExecutorProvider> executors = new ArrayList<>();
            if (profileRep.getExecutors() != null) {
                for (ClientPolicyExecutorRepresentation executorRep : profileRep.getExecutors()) {
                    ClientPolicyExecutorProvider provider = session.getProvider(ClientPolicyExecutorProvider.class, executorRep.getProviderId());
                    if (provider == null) {
                        // executor's provider not found. just skip it.
                        logger.warnf("Executor with provider ID %s not found", executorRep.getProviderId());
                        continue;
                    }

                    try {
                        ClientPolicyExecutorConfigurationRepresentation configuration = (ClientPolicyExecutorConfigurationRepresentation) JsonSerialization.mapper.convertValue(executorRep.getConfiguration(), provider.getExecutorConfigurationClass());
                        provider.setupConfiguration(configuration);
                        executors.add(provider);
                    } catch (IllegalArgumentException iae) {
                        logger.warnv("failed for Configuration Setup during setup provider {0} :: error = {1}", executorRep.getProviderId(), iae.getMessage());
                    }
                }
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
            throw new ClientPolicyException("proposed builtin client profile name duplicated.");
        }

        // construct validated and modified profiles from builtin profiles in JSON file enclosed in keycloak binary.
        ClientProfilesRepresentation updatingProfilesRep = new ClientProfilesRepresentation();
        updatingProfilesRep.setProfiles(new ArrayList<>());
        List<ClientProfileRepresentation> updatingProfileList = updatingProfilesRep.getProfiles();

        for (ClientProfileRepresentation proposedProfileRep : proposedProfilesRep.getProfiles()) {
            if (proposedProfileRep.getName() == null) {
                throw new ClientPolicyException("client profile without its name not allowed.");
            }

            // ignore proposed ordinal profile because builtin profile can only be added.
            if (proposedProfileRep.isBuiltin() == null || !proposedProfileRep.isBuiltin()) {
                throw new ClientPolicyException("ordinal client profile not allowed.");
            }

            ClientProfileRepresentation profileRep = new ClientProfileRepresentation();
            profileRep.setName(proposedProfileRep.getName());
            profileRep.setDescription(proposedProfileRep.getDescription());
            profileRep.setBuiltin(Boolean.TRUE);

            profileRep.setExecutors(new ArrayList<>()); // to prevent returning null
            if (proposedProfileRep.getExecutors() != null) {
                for (ClientPolicyExecutorRepresentation executorRep : proposedProfileRep.getExecutors()) {
                    if (!isValidExecutor(session, executorRep.getProviderId())) {
                        throw new ClientPolicyException("proposed client profile contains the executor with its invalid configuration.");
                    }
                    profileRep.getExecutors().add(executorRep);
                }
            }

            updatingProfileList.add(profileRep);
        }

        return updatingProfilesRep;
    }

    /**
     * convert client profiles as representation to json.
     * can return null.
     */
    public static String convertClientProfilesRepresentationToJson(ClientProfilesRepresentation reps) throws ClientPolicyException {
        return convertRepresentationToJson(reps);
    }

    /**
     * convert client profiles as json to representation.
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
    public static ClientProfilesRepresentation getValidatedClientProfilesRepresentation(KeycloakSession session, RealmModel realm, ClientProfilesRepresentation proposedProfilesRep) throws ClientPolicyException {
        if (proposedProfilesRep == null) {
            proposedProfilesRep = new ClientProfilesRepresentation();
        }
        if (realm == null) {
            throw new ClientPolicyException("realm not specified.");
        }

        // deserialize existing profiles (json -> representation)
        ClientProfilesRepresentation existingProfilesRep = null;
        String existingProfilesJson = session.clientPolicy().getClientProfilesJsonString(realm);
        if (existingProfilesJson != null) {
            existingProfilesRep = convertClientProfilesJsonToRepresentation(existingProfilesJson);
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
            throw new ClientPolicyException("proposed client profile name duplicated.");
        }

        // construct updating profiles from existing profiles and proposed profiles
        ClientProfilesRepresentation updatingProfilesRep = new ClientProfilesRepresentation();
        updatingProfilesRep.setProfiles(new ArrayList<>());
        List<ClientProfileRepresentation> updatingProfileList = updatingProfilesRep.getProfiles();

        // add existing builtin profiles to updating profiles
        List<ClientProfileRepresentation> existingProfileList = existingProfilesRep.getProfiles();
        Map<String, ClientProfileRepresentation> existingProfilesMap = new HashMap<>();
        if (existingProfileList != null && !existingProfileList.isEmpty()) {
            existingProfileList.stream().filter(i -> i.isBuiltin()).forEach(i -> {
                updatingProfileList.add(i);
                existingProfilesMap.put(i.getName(), i);
            });
        }

        for (ClientProfileRepresentation proposedProfileRep : proposedProfilesRep.getProfiles()) {
            if (proposedProfileRep.getName() == null || proposedProfileRep.getName().isEmpty()) {
                throw new ClientPolicyException("client profile without its name not allowed.");
            }

            // newly proposed builtin profile not allowed because builtin profile cannot added/deleted/modified.
            if (proposedProfileRep.isBuiltin() != null && proposedProfileRep.isBuiltin()) {
                // Check if already existing profile was sent in JSON. In this case, check if none of the properties was changed, because update of builtin not allowed
                ClientProfileRepresentation existingProfile = existingProfilesMap.get(proposedProfileRep.getName());
                if (existingProfile == null) {
                    throw new ClientPolicyException("newly builtin proposed client profile not allowed.");
                }

                JsonNode existingProfileNode = objectMapper.convertValue(existingProfile, JsonNode.class);
                JsonNode proposedProfileNode = objectMapper.convertValue(proposedProfileRep, JsonNode.class);
                if (!existingProfileNode.equals(proposedProfileNode)) {
                    throw new ClientPolicyException("Updating builtin profile not allowed");
                }

            } else {
                // not allow to overwrite builtin profiles or have duplicated profiles
                if (updatingProfileList.stream().anyMatch(i -> proposedProfileRep.getName().equals(i.getName()))) {
                    throw new ClientPolicyException("proposed client profile name is the same one of the existing profiles.");
                }


                // basically, proposed profile totally overrides existing profile
                ClientProfileRepresentation profileRep = new ClientProfileRepresentation();
                profileRep.setName(proposedProfileRep.getName());
                profileRep.setDescription(proposedProfileRep.getDescription());
                profileRep.setBuiltin(Boolean.FALSE);
                profileRep.setExecutors(new ArrayList<>());
                if (proposedProfileRep.getExecutors() != null) {
                    for (ClientPolicyExecutorRepresentation executorRep : proposedProfileRep.getExecutors()) {
                        if (!isValidExecutor(session, executorRep.getProviderId())) {
                            throw new ClientPolicyException("proposed client profile contains the executor with its invalid configuration.");
                        }
                        profileRep.getExecutors().add(executorRep);
                    }
                }

                updatingProfileList.add(profileRep);
            }
        }

        return updatingProfilesRep;
    }

    /**
     * check whether the proposed executor's provider can be found in keycloak's ClientPolicyExecutorProvider list.
     * not return null.
     */
    private static boolean isValidExecutor(KeycloakSession session, String executorProviderId) {
        Set<String> providerSet = session.listProviderIds(ClientPolicyExecutorProvider.class);
        if (providerSet != null && providerSet.contains(executorProviderId)) {
            return true;
        }
        logger.warnv("no executor provider found. providerId = {0}", executorProviderId);
        return false;
    }


    /**
     * get existing client policies in a realm as representation.
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
            return new ClientPoliciesRepresentation();
        }
        policiesRep = convertClientPoliciesJsonToRepresentation(policiesJson);
        if (policiesRep == null) {
            return new ClientPoliciesRepresentation();
        }

        return policiesRep;
    }

    /**
     * get existing enabled client policies in a realm as model.
     * not return null.
     */
    public static List<ClientPolicyModel> getEnabledClientPoliciesModel(KeycloakSession session, RealmModel realm) {
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
            logger.warnv("Failed to serialize client policies json string. err={0}, errDetail={1}", e.getError(), e.getErrorDetail());
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
            // pick up only enabled policy
            if (policyRep.isEnable() == null || policyRep.isEnable() == false) {
                continue;
            }

            ClientPolicyModel policyModel = new ClientPolicyModel();
            policyModel.setName(policyRep.getName());
            policyModel.setDescription(policyRep.getDescription());
            policyModel.setEnable(true);
            if (policyRep.isBuiltin() != null) {
                policyModel.setBuiltin(policyRep.isBuiltin().booleanValue());
            } else {
                policyModel.setBuiltin(false);
            }

            List<ClientPolicyConditionProvider> conditions = new ArrayList<>();
            if (policyRep.getConditions() != null) {
                for (ClientPolicyConditionRepresentation conditionRep : policyRep.getConditions()) {
                    ClientPolicyConditionProvider provider = session.getProvider(ClientPolicyConditionProvider.class, conditionRep.getProviderId());
                    if (provider == null) {
                        // condition's provider not found. just skip it.
                        logger.warnf("Condition with provider ID %s not found", conditionRep.getProviderId());
                        continue;
                    }

                    try {
                        ClientPolicyConditionConfigurationRepresentation configuration =  (ClientPolicyConditionConfigurationRepresentation) JsonSerialization.mapper.convertValue(conditionRep.getConfiguration(), provider.getConditionConfigurationClass());
                        provider.setupConfiguration(configuration);
                        conditions.add(provider);
                    } catch (IllegalArgumentException iae) {
                        logger.warnv("failed for Configuration Setup :: error = {0}", iae.getMessage());
                    }
                }
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
            throw new ClientPolicyException("proposed builtin client policy name duplicated.");
        }

        // construct validated and modified policies from builtin profiles in JSON file enclosed in keycloak binary.
        ClientPoliciesRepresentation updatingPoliciesRep = new ClientPoliciesRepresentation();
        updatingPoliciesRep.setPolicies(new ArrayList<>());
        List<ClientPolicyRepresentation> updatingPoliciesList = updatingPoliciesRep.getPolicies();

        for (ClientPolicyRepresentation proposedPolicyRep : proposedPoliciesRep.getPolicies()) {
            if (proposedPolicyRep.getName() == null) {
                throw new ClientPolicyException("proposed client policy name missing.");
            }

            // ignore proposed ordinal policy because builtin policy can only be added.
            if (proposedPolicyRep.isBuiltin() == null || !proposedPolicyRep.isBuiltin()) {
                throw new ClientPolicyException("ordinal client policy not allowed.");
            }

            ClientPolicyRepresentation policyRep = new ClientPolicyRepresentation();
            policyRep.setName(proposedPolicyRep.getName());
            policyRep.setDescription(proposedPolicyRep.getDescription());
            policyRep.setBuiltin(Boolean.TRUE);
            Boolean enabled = (proposedPolicyRep.isEnable() != null) ? proposedPolicyRep.isEnable() : Boolean.FALSE;
            policyRep.setEnable(enabled);

            policyRep.setConditions(new ArrayList<>());
            if (proposedPolicyRep.getConditions() != null) {
                for (ClientPolicyConditionRepresentation conditionRep : proposedPolicyRep.getConditions()) {
                    if (!isValidCondition(session, conditionRep.getProviderId())) {
                        throw new ClientPolicyException("the proposed client policy contains the condition with its invalid configuration.");
                    }
                    policyRep.getConditions().add(conditionRep);
                }
            }

            Set<String> existingProfileNames = new HashSet<>();
            ClientProfilesRepresentation reps = getClientProfilesRepresentation(session, null);
            reps.getProfiles().stream().map(profile->profile.getName()).forEach(profileName->existingProfileNames.add(profileName));
            policyRep.setProfiles(new ArrayList<>());
            if (proposedPolicyRep.getProfiles() != null) {
                for (String profileName : proposedPolicyRep.getProfiles()) {
                    if (existingProfileNames.contains(profileName) == false) {
                        throw new ClientPolicyException("referring not existing client profile not allowed.");
                    }
                }
                proposedPolicyRep.getProfiles().stream().distinct().forEach(profileName->policyRep.getProfiles().add(profileName));
            }

            updatingPoliciesList.add(policyRep);
        }

        return updatingPoliciesRep;
    }

    /**
     * convert client policies as representation to json.
     * can return null.
     */
    public static String convertClientPoliciesRepresentationToJson(ClientPoliciesRepresentation reps) throws ClientPolicyException {
        return convertRepresentationToJson(reps);
    }

    /**
     * convert client policies as json to representation.
     * not return null.
     */
    private static ClientPoliciesRepresentation convertClientPoliciesJsonToRepresentation(String json) throws ClientPolicyException {
        return convertJsonToRepresentation(json, ClientPoliciesRepresentation.class);
    }

    /**
     * get validated and modified client policies as representation.
     * it can be constructed by merging proposed client policies with existing client policies.
     * not return null.
     *
     * @param session
     * @param realm
     * @param proposedPoliciesRep
     * @param skipBuiltinValidationUpdate if true, then the exception won't be thrown in case that builtin representation of "proposedPoliciesRep" is not 100% same as the builtin.
     *                                    If false, then exception will be thrown in case they are not the same (with the exception of "enabled" field, which is only field allowed to update on builtin policies)
     */
    public static ClientPoliciesRepresentation getValidatedClientPoliciesRepresentation(KeycloakSession session, RealmModel realm,
                                                                                        ClientPoliciesRepresentation proposedPoliciesRep, boolean skipBuiltinValidationUpdate) throws ClientPolicyException {
        if (proposedPoliciesRep == null) {
            proposedPoliciesRep = new ClientPoliciesRepresentation();
        }
        if (realm == null) {
            throw new ClientPolicyException("realm not specified.");
        }

        // deserialize existing profiles (json -> represetation)
        ClientPoliciesRepresentation existingPoliciesRep = null;
        String existingPoliciesJson = session.clientPolicy().getClientPoliciesJsonString(realm);
        if (existingPoliciesJson != null) {
            existingPoliciesRep = convertClientPoliciesJsonToRepresentation(existingPoliciesJson);
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
            throw new ClientPolicyException("proposed client policy name duplicated.");
        }

        // construct updating policies from existing policies and proposed policies
        ClientPoliciesRepresentation updatingPoliciesRep = new ClientPoliciesRepresentation();
        updatingPoliciesRep.setPolicies(new ArrayList<>());
        List<ClientPolicyRepresentation> updatingPoliciesList = updatingPoliciesRep.getPolicies();

        // add existing builtin policies to updating policies
        Map<String, ClientPolicyRepresentation> existingBuiltinPolicies = new HashMap<>();
        List<ClientPolicyRepresentation> existingPoliciesList = existingPoliciesRep.getPolicies();
        if (existingPoliciesList != null && !existingPoliciesList.isEmpty()) {
            existingPoliciesList.stream().filter(i->i.isBuiltin()).forEach(builtinPolicy -> {
                updatingPoliciesList.add(builtinPolicy);
                existingBuiltinPolicies.put(builtinPolicy.getName(), builtinPolicy);
            });
        }

        for (ClientPolicyRepresentation proposedPolicyRep : proposedPoliciesRep.getPolicies()) {
            if (proposedPolicyRep.getName() == null) {
                throw new ClientPolicyException("proposed client policy name missing.");
            }

            // newly proposed builtin policy not allowed because builtin policy cannot added/deleted/modified.
            Boolean enabled = (proposedPolicyRep.isEnable() != null) ? proposedPolicyRep.isEnable() : Boolean.FALSE;
            if (proposedPolicyRep.isBuiltin() != null && proposedPolicyRep.isBuiltin()) {
                // Check if we are trying to add builtin policy. This is not allowed
                if (!existingBuiltinPolicies.keySet().contains(proposedPolicyRep.getName())) {
                    throw new ClientPolicyException("newly builtin proposed client policy not allowed.");
                }

                // Updating existing builtin policy is not allowed - with the exception of the "enabled" field
                ClientPolicyRepresentation existingBuiltinPolicy = existingBuiltinPolicies.get(proposedPolicyRep.getName());
                existingBuiltinPolicy.setEnable(enabled);

                if (!skipBuiltinValidationUpdate) {
                    JsonNode existingPolicyNode = objectMapper.convertValue(existingBuiltinPolicy, JsonNode.class);
                    JsonNode proposedPolicyNode = objectMapper.convertValue(proposedPolicyRep, JsonNode.class);
                    if (!existingPolicyNode.equals(proposedPolicyNode)) {
                        throw new ClientPolicyException("Updating builtin policy not allowed - with the exception of 'enabled' status");
                    }
                }

                continue;
            } else {
                // Check if we are trying to add the policy of same name as some builtin policy. This is not allowed
                if (existingBuiltinPolicies.keySet().contains(proposedPolicyRep.getName())) {
                    throw new ClientPolicyException("Not allowed to add new policy of same name as some existing policy");
                }
            }

            // basically, proposed policy totally overrides existing policy except for enabled field..
            ClientPolicyRepresentation policyRep = new ClientPolicyRepresentation();
            policyRep.setName(proposedPolicyRep.getName());
            policyRep.setDescription(proposedPolicyRep.getDescription());
            policyRep.setBuiltin(Boolean.FALSE);
            policyRep.setEnable(enabled);

            policyRep.setConditions(new ArrayList<>());
            if (proposedPolicyRep.getConditions() != null) {
                for (ClientPolicyConditionRepresentation conditionRep : proposedPolicyRep.getConditions()) {
                    if (!isValidCondition(session, conditionRep.getProviderId())) {
                        throw new ClientPolicyException("the proposed client policy contains the condition with its invalid configuration.");
                    }
                    policyRep.getConditions().add(conditionRep);
                }
            }

            Set<String> existingProfileNames = new HashSet<>();
            ClientProfilesRepresentation reps = getClientProfilesRepresentation(session, realm);
            if (reps.getProfiles() != null) {
                reps.getProfiles().stream().map(profile->profile.getName()).forEach(profileName->existingProfileNames.add(profileName));
            }
            policyRep.setProfiles(new ArrayList<>());
            if (proposedPolicyRep.getProfiles() != null) {
                for (String profileName : proposedPolicyRep.getProfiles()) {
                    if (existingProfileNames.contains(profileName) == false) {
                        throw new ClientPolicyException("referring not existing client profile not allowed.");
                    }
                }
                proposedPolicyRep.getProfiles().stream().distinct().forEach(profileName->policyRep.getProfiles().add(profileName));
            }

            updatingPoliciesList.add(policyRep);
        }

        return updatingPoliciesRep;
    }

    /**
     * check whether the proposed condition's provider can be found in keycloak's ClientPolicyConditionProvider list.
     * not return null.
     */
    private static boolean isValidCondition(KeycloakSession session, String conditionProviderId) {
        Set<String> providerSet = session.listProviderIds(ClientPolicyConditionProvider.class);
        if (providerSet != null && providerSet.contains(conditionProviderId)) {
            return true;
        }
        logger.warnv("no condition provider found. providerId = {0}", conditionProviderId);
        return false;
    }

    private static String convertRepresentationToJson(Object reps) throws ClientPolicyException {
        if (reps == null) return null;

        String json = null;
        try {
            json = objectMapper.writeValueAsString(reps);
        } catch (JsonProcessingException jpe) {
            throw new ClientPolicyException(jpe.getMessage());
        }

        return json;
    }

    private static <T> T convertJsonToRepresentation(String json, Class<T> type) throws ClientPolicyException {
        if (json == null) {
            throw new ClientPolicyException("no json.");
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
