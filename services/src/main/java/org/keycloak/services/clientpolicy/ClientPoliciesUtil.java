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
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.keycloak.common.Profile;
import org.keycloak.component.ComponentModel;
import org.keycloak.component.JsonConfigComponentModel;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.representations.idm.ClientPoliciesRepresentation;
import org.keycloak.representations.idm.ClientPolicyConditionConfigurationRepresentation;
import org.keycloak.representations.idm.ClientPolicyConditionRepresentation;
import org.keycloak.representations.idm.ClientPolicyExecutorConfigurationRepresentation;
import org.keycloak.representations.idm.ClientPolicyExecutorRepresentation;
import org.keycloak.representations.idm.ClientPolicyRepresentation;
import org.keycloak.representations.idm.ClientProfileRepresentation;
import org.keycloak.representations.idm.ClientProfilesRepresentation;
import org.keycloak.securityprofile.SecurityProfileProvider;
import org.keycloak.services.clientpolicy.condition.ClientPolicyConditionProvider;
import org.keycloak.services.clientpolicy.condition.ClientPolicyConditionProviderFactory;
import org.keycloak.services.clientpolicy.executor.ClientPolicyExecutorProvider;
import org.keycloak.util.JsonSerialization;
import org.keycloak.utils.FileUtils;

import com.fasterxml.jackson.databind.JsonNode;
import org.jboss.logging.Logger;

/**
 * Utilities for treating client policies/profiles
 *
 * @author <a href="mailto:takashi.norimatsu.ws@hitachi.com">Takashi Norimatsu</a>
 */
public class ClientPoliciesUtil {

    private static final Logger logger = Logger.getLogger(ClientPoliciesUtil.class);

    public static InputStream getJsonFileFromClasspathOrConfFolder(String name) throws IOException {
        final String fileName = name + ".json";
        return FileUtils.getJsonFileFromClasspathOrConfFolder(fileName);
    }

    public static List<ClientProfileRepresentation> readGlobalClientProfilesRepresentation(KeycloakSession session, String name) throws ClientPolicyException {
        if (name == null) {
            return Collections.emptyList();
        }
        try (InputStream is = getJsonFileFromClasspathOrConfFolder(name)) {
            return getValidatedGlobalClientProfilesRepresentation(session, is);
        } catch (IOException e) {
            throw new ClientPolicyException("Error reading profiles from " + name, e.getMessage(), e);
        }
    }

    public static List<ClientPolicyRepresentation> readGlobalClientPoliciesRepresentation(KeycloakSession session, String name,
            List<ClientProfileRepresentation> profiles) throws ClientPolicyException {
        if (name == null) {
            return Collections.emptyList();
        }
        try (InputStream is = getJsonFileFromClasspathOrConfFolder(name)) {
            return getValidatedGlobalClientPoliciesRepresentation(session, is, profiles);
        } catch (IOException e) {
            throw new ClientPolicyException("Error reading profiles from " + name, e.getMessage(), e);
        }
    }

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
     * Gets existing client profile of given name with resolved executor providers. It can be profile from realm or from global client profiles.
     */
    static ClientProfile getClientProfileModel(KeycloakSession session, RealmModel realm, ClientProfilesRepresentation profilesRep, List<ClientProfileRepresentation> globalClientProfiles, String profileName) throws ClientPolicyException {
        // Obtain profiles from realm
        List<ClientProfileRepresentation> profiles = profilesRep.getProfiles();
        if (profiles == null) {
            profiles = new ArrayList<>();
        }

        // Add global profiles as well
        profiles.addAll(globalClientProfiles);

        ClientProfileRepresentation profileRep = profiles.stream()
                .filter(clientProfile -> profileName.equals(clientProfile.getName()))
                .findFirst().orElse(null);
        if (profileRep == null) {
            return null;
        }

        ClientProfile profileModel = new ClientProfile();
        profileModel.setName(profileRep.getName());
        profileModel.setDescription(profileRep.getDescription());

        if (profileRep.getExecutors() == null) {
            profileModel.setExecutors(new ArrayList<>());
            return profileModel;
        }

        List<ClientPolicyExecutorProvider> executors = new ArrayList<>();
        if (profileRep.getExecutors() != null) {
            for (ClientPolicyExecutorRepresentation executorRep : profileRep.getExecutors()) {
                ClientPolicyExecutorProvider provider = getExecutorProvider(session, realm, executorRep.getExecutorProviderId(), executorRep.getConfiguration());
                executors.add(provider);
            }
        }
        profileModel.setExecutors(executors);

        return profileModel;
    }

    private static ClientPolicyExecutorProvider getExecutorProvider(KeycloakSession session, RealmModel realm, String providerId, JsonNode config) {
        ComponentModel componentModel = new JsonConfigComponentModel(ClientPolicyExecutorProvider.class, realm.getId(), providerId, config);
        ClientPolicyExecutorProvider executorProvider = session.getComponentProvider(ClientPolicyExecutorProvider.class, componentModel.getId(), sessionFactory -> componentModel);
        if (executorProvider == null) {
            // condition's provider not found. just skip it.
            throw new IllegalStateException("Executor with provider ID " + providerId + " not found");
        }

        ClientPolicyExecutorConfigurationRepresentation configuration =  (ClientPolicyExecutorConfigurationRepresentation) JsonSerialization.mapper.convertValue(config, executorProvider.getExecutorConfigurationClass());
        executorProvider.setupConfiguration(configuration);
        return executorProvider;
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
                    if (Profile.isFeatureEnabled(Profile.Feature.CLIENT_POLICIES) && !isValidExecutor(session, executorRep)) {
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
     * get validated and modified global (built-in) client policies set on keycloak app as representation.
     * it is loaded from json file enclosed in keycloak's binary.
     * not return null.
     */
    static List<ClientPolicyRepresentation> getValidatedGlobalClientPoliciesRepresentation(KeycloakSession session, InputStream is, List<ClientProfileRepresentation> profiles) throws ClientPolicyException {
        ClientPoliciesRepresentation proposedPoliciesRep = null;
        try {
            proposedPoliciesRep = JsonSerialization.readValue(is, ClientPoliciesRepresentation.class);
        } catch (Exception e) {
            throw new ClientPolicyException("failed to deserialize global proposed client profiles json string.", e.getMessage());
        }
        if (proposedPoliciesRep == null) {
            return Collections.emptyList();
        }
        return validatePolicies(session, proposedPoliciesRep.getPolicies(), profiles, Collections.emptyList());
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

        // validate global profiles not changed
        if (isGlobalProfilesUpdated(proposedProfilesRep.getGlobalProfiles(), globalClientProfiles)) {
            throw new ClientPolicyException("Global profiles cannot be updated");
        }

        // Conflict with any global profile is not allowed
        Set<String> globalProfileNames = globalClientProfiles.stream().map(ClientProfileRepresentation::getName).collect(Collectors.toSet());
        for (ClientProfileRepresentation clientProfile : proposedProfileRepList) {
            if (globalProfileNames.contains(clientProfile.getName())) {
                throw new ClientPolicyException("Proposed profile name '" + clientProfile.getName() + "' is duplicated as a global profile");
            }
        }

        // Validate executor
        for (ClientProfileRepresentation proposedProfileRep : proposedProfilesRep.getProfiles()) {
            if (proposedProfileRep.getExecutors() != null) {
                for (ClientPolicyExecutorRepresentation executorRep : proposedProfileRep.getExecutors()) {
                    if (!isValidExecutor(session, executorRep)) {
                        proposedProfileRep.getExecutors().remove(executorRep);
                        throw new ClientPolicyException("proposed client profile contains the executor, which does not have valid provider, or has invalid configuration.");
                    }
                }
            }
        }

        // Make sure to not save built-in inside realm attribute
        proposedProfilesRep.setGlobalProfiles(null);

        return proposedProfilesRep;
    }

    private static boolean isGlobalProfilesUpdated(List<ClientProfileRepresentation> proposedGlobalProfiles, List<ClientProfileRepresentation> origGlobalProfiles) {
        // if globalProfiles were not sent, we can skip this
        if (proposedGlobalProfiles == null || proposedGlobalProfiles.isEmpty()) return false;
        return !proposedGlobalProfiles.equals(origGlobalProfiles);
    }

    private static boolean isGlobalPoliciesUpdated(List<ClientPolicyRepresentation> proposedGlobalPolicies, List<ClientPolicyRepresentation> origGlobalPolicies) {
        // if globalPolicies were not sent, we can skip this
        if (proposedGlobalPolicies == null || proposedGlobalPolicies.isEmpty()) return false;
        return !proposedGlobalPolicies.equals(origGlobalPolicies);
    }

    /**
     * check whether the proposed executor's provider can be found in keycloak's ClientPolicyExecutorProvider list.
     * not return null.
     */
    private static boolean isValidExecutor(KeycloakSession session, ClientPolicyExecutorRepresentation executorRep) {
        String executorProviderId = executorRep.getExecutorProviderId();
        Set<String> providerSet = session.listProviderIds(ClientPolicyExecutorProvider.class);
        if (providerSet != null && providerSet.contains(executorProviderId)) {
            if (Objects.nonNull(session.getContext().getRealm())){
                ClientPolicyExecutorProvider provider = getExecutorProvider(session, session.getContext().getRealm(), executorProviderId, executorRep.getConfiguration());
                ClientPolicyExecutorConfigurationRepresentation configuration =  (ClientPolicyExecutorConfigurationRepresentation) JsonSerialization.mapper.convertValue(executorRep.getConfiguration(), provider.getExecutorConfigurationClass());
                return configuration.validateConfig();
            } else {
                return true;
            }
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

    static List<ClientProfileRepresentation> getGlobalClientProfiles(KeycloakSession session) {
        SecurityProfileProvider securityProfile = session.getProvider(SecurityProfileProvider.class);
        return securityProfile.getDefaultClientProfiles();
    }

    static List<ClientPolicyRepresentation> getGlobalClientPolicies(KeycloakSession session) {
        SecurityProfileProvider securityProfile = session.getProvider(SecurityProfileProvider.class);
        return securityProfile.getDefaultClientPolicies();
    }

    /**
     * Gets existing enabled client policies in a realm.
     * not return null.
     */
    static List<ClientPolicy> getEnabledClientPolicies(KeycloakSession session, RealmModel realm) {
        // get the global policies defined in the security profile
        List<ClientPolicyRepresentation> policiesRep = new ArrayList<>(getGlobalClientPolicies(session));

        // get existing profiles as json
        String policiesJson = getClientPoliciesJsonString(realm);
        if (policiesJson != null) {
            // deserialize existing policies (json -> representation)
            try {
                policiesRep.addAll(convertClientPoliciesJsonToRepresentation(policiesJson).getPolicies());
            } catch (ClientPolicyException e) {
                logger.warnv("Failed to serialize client policies json string. err={0}, errDetail={1}", e.getError(), e.getErrorDetail());
                return Collections.emptyList();
            }
        }
        if (policiesRep.isEmpty()) {
            return Collections.emptyList();
        }

        // constructing existing policies (representation -> model)
        List<ClientPolicy> policyList = new ArrayList<>();
        for (ClientPolicyRepresentation policyRep: policiesRep) {
            // ignore policy without name
            if (policyRep.getName() == null) {
                logger.warnf("Ignored client policy without name in the realm %s", realm.getName());
                continue;
            }
            // pick up only enabled policy
            if (policyRep.isEnabled() == null || policyRep.isEnabled() == false) {
                continue;
            }

            ClientPolicy policyModel = new ClientPolicy();
            policyModel.setName(policyRep.getName());
            policyModel.setDescription(policyRep.getDescription());
            policyModel.setEnable(true);

            List<ClientPolicyConditionProvider> conditions = new ArrayList<>();
            if (policyRep.getConditions() != null) {
                for (ClientPolicyConditionRepresentation conditionRep : policyRep.getConditions()) {
                    ClientPolicyConditionProvider provider = getConditionProvider(session, realm, conditionRep.getConditionProviderId(), conditionRep.getConfiguration());
                    conditions.add(provider);
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

    private static ClientPolicyConditionProvider getConditionProvider(KeycloakSession session, RealmModel realm, String providerId, JsonNode config) {
        ComponentModel componentModel = new JsonConfigComponentModel(ClientPolicyConditionProvider.class, realm.getId(), providerId, config);
        ClientPolicyConditionProvider conditionProvider = session.getComponentProvider(ClientPolicyConditionProvider.class, componentModel.getId(), sessionFactory -> componentModel);
        if (conditionProvider == null) {
            // condition's provider not found. just skip it.
            throw new IllegalStateException("Condition with provider ID " + providerId + " not found");
        }

        ClientPolicyConditionConfigurationRepresentation configuration =  (ClientPolicyConditionConfigurationRepresentation) JsonSerialization.mapper.convertValue(config, conditionProvider.getConditionConfigurationClass());
        conditionProvider.setupConfiguration(configuration);
        return conditionProvider;
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
     * Validates the policies passed with the profiles.
     * @param session The session
     * @param proposedPoliciesRepList The policies to validate
     * @param profiles The already validated profiles used by the policies
     * @param globalPolicies The global policies defined already validated
     * @return The validated policies
     * @throws ClientPolicyException Some error in the policies
     */
    static private List<ClientPolicyRepresentation> validatePolicies(KeycloakSession session, List<ClientPolicyRepresentation> proposedPoliciesRepList,
            List<ClientProfileRepresentation> profiles, List<ClientPolicyRepresentation> globalPolicies) throws ClientPolicyException {

        // empty policies is valid
        if (proposedPoliciesRepList == null || proposedPoliciesRepList.isEmpty()) {
            return Collections.emptyList();
        }

        // check for duplicated names
        if (proposedPoliciesRepList.size() != proposedPoliciesRepList.stream().map(i->i.getName()).distinct().count()) {
            throw new ClientPolicyException("proposed client policy name duplicated");
        }

        // Conflict with any global policy is not allowed
        Set<String> globalPolicyNames = globalPolicies.stream().map(ClientPolicyRepresentation::getName).collect(Collectors.toSet());
        for (ClientPolicyRepresentation clientPolicy : proposedPoliciesRepList) {
            if (globalPolicyNames.contains(clientPolicy.getName())) {
                throw new ClientPolicyException("Proposed policy name '" + clientPolicy.getName() + "' is duplicated as a global policy");
            }
        }

        // construct validated and modified profiles from builtin profiles in JSON file enclosed in keycloak binary.
        List<ClientPolicyRepresentation> updatingPolicyList = new LinkedList<>();

        Set<String> profileNames = profiles.stream().map(ClientProfileRepresentation::getName).collect(Collectors.toSet());

        for (ClientPolicyRepresentation proposedPolicyRep : proposedPoliciesRepList) {
            if (proposedPolicyRep.getName() == null) {
                throw new ClientPolicyException("client policy without name not allowed");
            }

            ClientPolicyRepresentation policyRep = new ClientPolicyRepresentation();
            policyRep.setName(proposedPolicyRep.getName());
            policyRep.setDescription(proposedPolicyRep.getDescription());
            policyRep.setEnabled(proposedPolicyRep.isEnabled() != null ? proposedPolicyRep.isEnabled() : Boolean.FALSE);

            policyRep.setConditions(new ArrayList<>());
            if (proposedPolicyRep.getConditions() != null) {
                for (ClientPolicyConditionRepresentation condition : proposedPolicyRep.getConditions()) {
                    if (Profile.isFeatureEnabled(Profile.Feature.CLIENT_POLICIES) && !isValidCondition(session, condition.getConditionProviderId())) {
                        throw new ClientPolicyException("Policy " + proposedPolicyRep.getName() + " contains invalid condition " + condition.getConditionProviderId());
                    }
                    if (Profile.isFeatureEnabled(Profile.Feature.CLIENT_POLICIES)) {
                        validateConditionConfig(session, condition);
                    }
                    policyRep.getConditions().add(condition);
                }
            }

            policyRep.setProfiles(new ArrayList<>());
            if (proposedPolicyRep.getProfiles() != null) {
                if (proposedPolicyRep.getProfiles().size() != proposedPolicyRep.getProfiles().stream().distinct().count()) {
                    throw new ClientPolicyException("Policy " + proposedPolicyRep.getName() + " contains duplicated profiles");
                }
                for (String profile : proposedPolicyRep.getProfiles()) {
                    if (!profileNames.contains(profile)) {
                        throw new ClientPolicyException("Policy " + proposedPolicyRep.getName() + " contains invalid profile " + profile);
                    }
                    policyRep.getProfiles().add(profile);
                }
            }

            updatingPolicyList.add(policyRep);
        }

        return updatingPolicyList;
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
            ClientPoliciesRepresentation proposedPoliciesRep, List<ClientProfileRepresentation> existingGlobalProfiles,
            List<ClientPolicyRepresentation> existingGlobalPolicies) throws ClientPolicyException {
        if (realm == null) {
            throw new ClientPolicyException("realm not specified.");
        }

        // validate global profiles not changed
        if (isGlobalPoliciesUpdated(proposedPoliciesRep.getGlobalPolicies(), existingGlobalPolicies)) {
            throw new ClientPolicyException("Global policies cannot be updated");
        }

        ClientPoliciesRepresentation updatingPoliciesRep = new ClientPoliciesRepresentation();

        List<ClientProfileRepresentation> allProfiles = new ArrayList<>(getClientProfilesRepresentation(session, realm).getProfiles());
        allProfiles.addAll(existingGlobalProfiles);
        updatingPoliciesRep.setPolicies(validatePolicies(session, proposedPoliciesRep.getPolicies(), allProfiles, existingGlobalPolicies));

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

    private static void validateConditionConfig(KeycloakSession session, ClientPolicyConditionRepresentation conditionRep) throws ClientPolicyException {
        ClientPolicyConditionProviderFactory factory = getClientPolicyConditionFactory(session, conditionRep.getConditionProviderId());
        try {
            factory.validateConfiguration(session, session.getContext().getRealm(), conditionRep);
        }
        catch (ClientPolicyException e) {
            throw new ClientPolicyException("Invalid " + conditionRep.getConditionProviderId() + " configuration - " + e.getMessage());
        }
    }

    private static ClientPolicyConditionProviderFactory getClientPolicyConditionFactory(KeycloakSession session, String providerId) {
        Class<? extends Provider> provider = session.getProviderClass(ClientPolicyConditionProvider.class.getName());
        if (provider == null) {
            throw new IllegalArgumentException("Invalid provider type '" + ClientPolicyConditionProvider.class.getName() + "'");
        }

        ProviderFactory<? extends Provider> f = session.getKeycloakSessionFactory().getProviderFactory(provider, providerId);
        if (f == null) {
            throw new IllegalArgumentException("No such provider '" + providerId + "'");
        }

        return (ClientPolicyConditionProviderFactory) f;
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
