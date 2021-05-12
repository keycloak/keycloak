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
 *
 */

package org.keycloak.services.clientpolicy;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.jboss.logging.Logger;

import org.keycloak.common.Profile;
import org.keycloak.models.Constants;
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

/**
 * Utilities for treating client policies/profiles
 *
 * @author <a href="mailto:takashi.norimatsu.ws@hitachi.com">Takashi Norimatsu</a>
 */
public class ClientPoliciesUtil {

    private static final Logger logger = Logger.getLogger(ClientPoliciesUtil.class);

    /**
     * gets existing client profiles in a realm as representation.
     * not return null.
     */
    static ClientProfilesRepresentation getClientProfilesRepresentation(KeycloakSession session, RealmModel realm) throws ClientPolicyException {
        String profilesJson = getClientProfilesJsonString(realm);

        // deserialize existing profiles (json -> representation)
        if (profilesJson == null) {
            return new ClientProfilesRepresentation();
        }
        return convertClientProfilesJsonToRepresentation(profilesJson);
    }

    /**
     * gets existing client profiles in a realm as model.
     * not return null.
     */
    static Map<String, ClientProfileModel> getClientProfilesModel(KeycloakSession session, RealmModel realm, List<ClientProfileRepresentation> globalClientProfiles) {
        // get existing profiles as json
        String profilesJson = getClientProfilesJsonString(realm);
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
        List<ClientProfileRepresentation> profiles = profilesRep.getProfiles();

        // Add global profiles as well
        profiles.addAll(globalClientProfiles);

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

            if (profileRep.getExecutors() == null) {
                profileModel.setExecutors(new ArrayList<>());
                profileMap.put(profileRep.getName(), profileModel);
                continue;
            }

            List<ClientPolicyExecutorProvider> executors = new ArrayList<>();
            if (profileRep.getExecutors() != null) {
                for (ClientPolicyExecutorRepresentation executorRep : profileRep.getExecutors()) {
                    ClientPolicyExecutorProvider provider = session.getProvider(ClientPolicyExecutorProvider.class, executorRep.getExecutorProviderId());
                    if (provider == null) {
                        // executor's provider not found. just skip it.
                        logger.warnf("Executor with provider ID %s not found", executorRep.getExecutorProviderId());
                        continue;
                    }

                    try {
                        ClientPolicyExecutorConfigurationRepresentation configuration = (ClientPolicyExecutorConfigurationRepresentation) JsonSerialization.mapper.convertValue(executorRep.getConfiguration(), provider.getExecutorConfigurationClass());
                        provider.setupConfiguration(configuration);
                        executors.add(provider);
                    } catch (IllegalArgumentException iae) {
                        logger.warnv("failed for Configuration Setup during setup provider {0} :: error = {1}", executorRep.getExecutorProviderId(), iae.getMessage());
                    }
                }
            }
            profileModel.setExecutors(executors);

            profileMap.put(profileRep.getName(), profileModel);
        }

        return profileMap;
    }

    /**
     * get validated and modified global (built-in) client profiles set on keycloak app as representation.
     * it is loaded from json file enclosed in keycloak's binary.
     * not return null.
     */
    static List<ClientProfileRepresentation> getValidatedGlobalClientProfilesRepresentation(KeycloakSession session, InputStream is) throws ClientPolicyException {
        // load builtin client profiles representation
        ClientProfilesRepresentation proposedProfilesRep = null;
        try {
            proposedProfilesRep = JsonSerialization.readValue(is, ClientProfilesRepresentation.class);
        } catch (Exception e) {
            throw new ClientPolicyException("failed to deserialize global proposed client profiles json string.", e.getMessage());
        }
        if (proposedProfilesRep == null) {
            return Collections.emptyList();
        }

        // no profile contained (it is valid)
        List<ClientProfileRepresentation> proposedProfileRepList = proposedProfilesRep.getProfiles();
        if (proposedProfileRepList == null || proposedProfileRepList.isEmpty()) {
            return Collections.emptyList();
        }

        // duplicated profile name is not allowed.
        if (proposedProfileRepList.size() != proposedProfileRepList.stream().map(i->i.getName()).distinct().count()) {
            throw new ClientPolicyException("proposed global client profile name duplicated.");
        }

        // construct validated and modified profiles from builtin profiles in JSON file enclosed in keycloak binary.
        List<ClientProfileRepresentation> updatingProfileList = new LinkedList<>();

        for (ClientProfileRepresentation proposedProfileRep : proposedProfilesRep.getProfiles()) {
            if (proposedProfileRep.getName() == null) {
                throw new ClientPolicyException("client profile without its name not allowed.");
            }

            ClientProfileRepresentation profileRep = new ClientProfileRepresentation();
            profileRep.setName(proposedProfileRep.getName());
            profileRep.setDescription(proposedProfileRep.getDescription());

            profileRep.setExecutors(new ArrayList<>()); // to prevent returning null
            if (proposedProfileRep.getExecutors() != null) {
                for (ClientPolicyExecutorRepresentation executorRep : proposedProfileRep.getExecutors()) {
                    // Skip the check if feature is disabled as then the executor implementations are disabled
                    if (Profile.isFeatureEnabled(Profile.Feature.CLIENT_POLICIES) && !isValidExecutor(session, executorRep.getExecutorProviderId())) {
                        throw new ClientPolicyException("proposed client profile contains the executor with its invalid configuration.");
                    }
                    profileRep.getExecutors().add(executorRep);
                }
            }

            updatingProfileList.add(profileRep);
        }

        return updatingProfileList;
    }

    /**
     * convert client profiles as representation to json.
     * can return null.
     */
    public static String convertClientProfilesRepresentationToJson(ClientProfilesRepresentation reps) throws ClientPolicyException {
        try {
            return JsonSerialization.writeValueAsString(reps);
        } catch (IOException ioe) {
            throw new ClientPolicyException(ioe.getMessage());
        }
    }

    /**
     * convert client profiles as json to representation.
     * not return null.
     */
    private static ClientProfilesRepresentation convertClientProfilesJsonToRepresentation(String json) throws ClientPolicyException {
        try {
            return JsonSerialization.readValue(json, ClientProfilesRepresentation.class);
        } catch (IOException ioe) {
            throw new ClientPolicyException(ioe.getMessage());
        }
    }

    /**
     * get validated and modified client profiles as representation.
     * it can be constructed by merging proposed client profiles with existing client profiles.
     * not return null.
     */
    static ClientProfilesRepresentation getValidatedClientProfilesForUpdate(KeycloakSession session, RealmModel realm,
                                                                                   ClientProfilesRepresentation proposedProfilesRep, List<ClientProfileRepresentation> globalClientProfiles) throws ClientPolicyException {
        if (realm == null) {
            throw new ClientPolicyException("realm not specified.");
        }

        // no profile contained (it is valid)
        List<ClientProfileRepresentation> proposedProfileRepList = proposedProfilesRep.getProfiles();
        if (proposedProfileRepList == null || proposedProfileRepList.isEmpty()) {
            proposedProfileRepList = new ArrayList<>();
            proposedProfilesRep.setProfiles(new ArrayList<>());
        }

        // Profile without name not allowed
        if (proposedProfileRepList.stream().anyMatch(clientProfile -> clientProfile.getName() == null || clientProfile.getName().isEmpty())) {
            throw new ClientPolicyException("client profile without its name not allowed.");
        }

        // duplicated profile name is not allowed.
        if (proposedProfileRepList.size() != proposedProfileRepList.stream().map(i->i.getName()).distinct().count()) {
            throw new ClientPolicyException("proposed client profile name duplicated.");
        }

        // Conflict with any global profile is not allowed
        Set<String> globalProfileNames = globalClientProfiles.stream().map(ClientProfileRepresentation::getName).collect(Collectors.toSet());
        for (ClientProfileRepresentation clientProfile : proposedProfileRepList) {
            if (globalProfileNames.contains(clientProfile.getName())) {
                throw new ClientPolicyException("Proposed profile name duplicated as the name of some global profile");
            }
        }

        // Validate executor
        for (ClientProfileRepresentation proposedProfileRep : proposedProfilesRep.getProfiles()) {
            if (proposedProfileRep.getExecutors() != null) {
                for (ClientPolicyExecutorRepresentation executorRep : proposedProfileRep.getExecutors()) {
                    if (!isValidExecutor(session, executorRep.getExecutorProviderId())) {
                        throw new ClientPolicyException("proposed client profile contains the executor, which does not have valid provider, or has invalid configuration.");
                    }
                }
            }
        }

        // Make sure to not save built-in inside realm attribute
        proposedProfilesRep.setGlobalProfiles(null);

        return proposedProfilesRep;
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
    static ClientPoliciesRepresentation getClientPoliciesRepresentation(KeycloakSession session, RealmModel realm) throws ClientPolicyException {
        // get existing policies json
        String policiesJson = getClientPoliciesJsonString(realm);

        // deserialize existing policies (json -> representation)
        if (policiesJson == null) {
            return new ClientPoliciesRepresentation();
        }
        return convertClientPoliciesJsonToRepresentation(policiesJson);
    }

    /**
     * get existing enabled client policies in a realm as model.
     * not return null.
     */
    static List<ClientPolicyModel> getEnabledClientPoliciesModel(KeycloakSession session, RealmModel realm) {
        // get existing profiles as json
        String policiesJson = getClientPoliciesJsonString(realm);
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
                logger.warnf("Ignored client policy without name in the realm %s", realm.getName());
                continue;
            }
            // pick up only enabled policy
            if (policyRep.isEnabled() == null || policyRep.isEnabled() == false) {
                continue;
            }

            ClientPolicyModel policyModel = new ClientPolicyModel();
            policyModel.setName(policyRep.getName());
            policyModel.setDescription(policyRep.getDescription());
            policyModel.setEnable(true);

            List<ClientPolicyConditionProvider> conditions = new ArrayList<>();
            if (policyRep.getConditions() != null) {
                for (ClientPolicyConditionRepresentation conditionRep : policyRep.getConditions()) {
                    ClientPolicyConditionProvider provider = session.getProvider(ClientPolicyConditionProvider.class, conditionRep.getConditionProviderId());
                    if (provider == null) {
                        // condition's provider not found. just skip it.
                        logger.warnf("Condition with provider ID %s not found", conditionRep.getConditionProviderId());
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
     * convert client policies as representation to json.
     * can return null.
     */
    public static String convertClientPoliciesRepresentationToJson(ClientPoliciesRepresentation reps) throws ClientPolicyException {
        try {
            return JsonSerialization.writeValueAsString(reps);
        } catch (IOException ioe) {
            throw new ClientPolicyException(ioe.getMessage());
        }
    }

    /**
     * convert client policies as json to representation.
     * not return null.
     */
    private static ClientPoliciesRepresentation convertClientPoliciesJsonToRepresentation(String json) throws ClientPolicyException {
        try {
            return JsonSerialization.readValue(json, ClientPoliciesRepresentation.class);
        } catch (IOException ioe) {
            throw new ClientPolicyException(ioe.getMessage());
        }
    }

    /**
     * get validated and modified client policies as representation.
     * it can be constructed by merging proposed client policies with existing client policies.
     * not return null.
     *
     * @param session
     * @param realm
     * @param proposedPoliciesRep
     */
    static ClientPoliciesRepresentation getValidatedClientPoliciesForUpdate(KeycloakSession session, RealmModel realm,
                                                                                   ClientPoliciesRepresentation proposedPoliciesRep, List<ClientProfileRepresentation> existingGlobalProfiles) throws ClientPolicyException {
        if (realm == null) {
            throw new ClientPolicyException("realm not specified.");
        }

        // no policy contained (it is valid)
        List<ClientPolicyRepresentation> proposedPolicyRepList = proposedPoliciesRep.getPolicies();
        if (proposedPolicyRepList == null || proposedPolicyRepList.isEmpty()) {
            proposedPolicyRepList = new ArrayList<>();
            proposedPoliciesRep.setPolicies(new ArrayList<>());
         }

        // Policy without name not allowed
        if (proposedPolicyRepList.stream().anyMatch(clientPolicy -> clientPolicy.getName() == null || clientPolicy.getName().isEmpty())) {
            throw new ClientPolicyException("proposed client policy name missing.");
        }

        // duplicated policy name is not allowed.
        if (proposedPolicyRepList.size() != proposedPolicyRepList.stream().map(i->i.getName()).distinct().count()) {
            throw new ClientPolicyException("proposed client policy name duplicated.");
        }

        // construct updating policies from existing policies and proposed policies
        ClientPoliciesRepresentation updatingPoliciesRep = new ClientPoliciesRepresentation();
        updatingPoliciesRep.setPolicies(new ArrayList<>());
        List<ClientPolicyRepresentation> updatingPoliciesList = updatingPoliciesRep.getPolicies();

        for (ClientPolicyRepresentation proposedPolicyRep : proposedPoliciesRep.getPolicies()) {
            // newly proposed builtin policy not allowed because builtin policy cannot added/deleted/modified.
            Boolean enabled = (proposedPolicyRep.isEnabled() != null) ? proposedPolicyRep.isEnabled() : Boolean.FALSE;

            // basically, proposed policy totally overrides existing policy except for enabled field..
            ClientPolicyRepresentation policyRep = new ClientPolicyRepresentation();
            policyRep.setName(proposedPolicyRep.getName());
            policyRep.setDescription(proposedPolicyRep.getDescription());
            policyRep.setEnabled(enabled);

            policyRep.setConditions(new ArrayList<>());
            if (proposedPolicyRep.getConditions() != null) {
                for (ClientPolicyConditionRepresentation conditionRep : proposedPolicyRep.getConditions()) {
                    if (!isValidCondition(session, conditionRep.getConditionProviderId())) {
                        throw new ClientPolicyException("the proposed client policy contains the condition with its invalid configuration.");
                    }
                    policyRep.getConditions().add(conditionRep);
                }
            }

            Set<String> existingProfileNames = existingGlobalProfiles.stream().map(ClientProfileRepresentation::getName).collect(Collectors.toSet());
            ClientProfilesRepresentation reps = getClientProfilesRepresentation(session, realm);
            policyRep.setProfiles(new ArrayList<>());
            if (reps.getProfiles() != null) {
                existingProfileNames.addAll(reps.getProfiles().stream()
                        .map(ClientProfileRepresentation::getName)
                        .collect(Collectors.toSet()));
            }
            if (proposedPolicyRep.getProfiles() != null) {
                for (String profileName : proposedPolicyRep.getProfiles()) {
                    if (!existingProfileNames.contains(profileName)) {
                        logger.warnf("Client policy %s referred not existing profile %s");
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

    static String getClientProfilesJsonString(RealmModel realm) {
        return realm.getAttribute(Constants.CLIENT_PROFILES);
    }

    static String getClientPoliciesJsonString(RealmModel realm) {
        return realm.getAttribute(Constants.CLIENT_POLICIES);
    }

    static void setClientProfilesJsonString(RealmModel realm, String json) {
        realm.setAttribute(Constants.CLIENT_PROFILES, json);
    }

    static void setClientPoliciesJsonString(RealmModel realm, String json) {
        realm.setAttribute(Constants.CLIENT_POLICIES, json);
    }

}
