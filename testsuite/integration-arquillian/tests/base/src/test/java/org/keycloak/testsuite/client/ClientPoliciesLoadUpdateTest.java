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

package org.keycloak.testsuite.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.keycloak.testsuite.admin.AbstractAdminTest.loadJson;
import static org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude.AuthServer.QUARKUS;
import static org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude.AuthServer.REMOTE;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.keycloak.authentication.authenticators.client.ClientIdAndSecretAuthenticator;
import org.keycloak.authentication.authenticators.client.JWTClientAuthenticator;
import org.keycloak.common.Profile;
import org.keycloak.representations.idm.ClientPoliciesRepresentation;
import org.keycloak.representations.idm.ClientPolicyRepresentation;
import org.keycloak.representations.idm.ClientProfileRepresentation;
import org.keycloak.representations.idm.ClientProfilesRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.services.clientpolicy.ClientPolicyException;
import org.keycloak.services.clientpolicy.ClientPoliciesUtil;
import org.keycloak.services.clientpolicy.condition.AnyClientConditionFactory;
import org.keycloak.services.clientpolicy.condition.ClientAccessTypeConditionFactory;
import org.keycloak.services.clientpolicy.condition.ClientRolesConditionFactory;
import org.keycloak.services.clientpolicy.executor.PKCEEnforceExecutorFactory;
import org.keycloak.services.clientpolicy.executor.SecureClientAuthEnforceExecutorFactory;
import org.keycloak.services.clientpolicy.executor.SecureSessionEnforceExecutorFactory;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude;
import org.keycloak.testsuite.arquillian.annotation.EnableFeature;

@EnableFeature(value = Profile.Feature.CLIENT_POLICIES, skipRestart = true)
@AuthServerContainerExclude({REMOTE, QUARKUS})
public class ClientPoliciesLoadUpdateTest extends AbstractClientPoliciesTest {

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        RealmRepresentation realm = loadJson(getClass().getResourceAsStream("/testrealm.json"), RealmRepresentation.class);
        testRealms.add(realm);
    }

    // Invalid formatted json profiles/policies are not accepted. Existing profiles/policies remain unchanged.
    // Well-formed json but invalid semantic profiles/policies are not accepted. Existing profiles/policies remain unchanged.
    // Recognized but invalid type fields are not accepted. Existing profiles/policies remain unchanged.
    // Unrecognized fields of profiles/policies are not accepted. Existing profiles/policies are changed.
    // Unrecognized fields of executors/conditions are accepted. Existing profiles/policies are changed.
    // Duplicated fields of profiles/policies are accepted but the only last one is accepted. Existing profiles/policies are changed.

    @Test
    public void testLoadBuiltinProfilesAndPolicies() throws Exception {
        // retrieve loaded builtin profiles
        ClientProfilesRepresentation actualProfilesRep = getProfiles();

        // same profiles
        assertExpectedProfiles(actualProfilesRep, Arrays.asList("builtin-default-profile"));

        // each profile
        ClientProfileRepresentation actualProfileRep =  getProfileRepresentation(actualProfilesRep, "builtin-default-profile");
        assertExpectedProfile(actualProfileRep, "builtin-default-profile", "The built-in default profile for enforcing basic security level to clients.", true);

        // each executor
        assertExpectedExecutors(Arrays.asList(SecureSessionEnforceExecutorFactory.PROVIDER_ID), actualProfileRep);

        // retrieve loaded builtin policies
        ClientPoliciesRepresentation actualPoliciesRep = getPolicies();

        // same policies
        assertExpectedPolicies(Arrays.asList("builtin-default-policy"), actualPoliciesRep);

        // each policy
        ClientPolicyRepresentation actualPolicyRep =  getPolicyRepresentation(actualPoliciesRep, "builtin-default-policy");
        assertExpectedPolicy("builtin-default-policy", "The built-in default policy applied to all clients.", true, false, Arrays.asList("builtin-default-profile"), actualPolicyRep);

        // each condition
        assertExpectedConditions(Arrays.asList(AnyClientConditionFactory.PROVIDER_ID), actualPolicyRep);

    }

    @Test
    public void testUpdateValidProfilesAndPolicies() throws Exception {
        loadValidProfilesAndPolicies();

        assertExpectedLoadedProfiles((ClientProfilesRepresentation reps)->{
            ClientProfileRepresentation rep =  getProfileRepresentation(reps, "ordinal-test-profile");
            assertExpectedProfile(rep, "ordinal-test-profile", "The profile that can be loaded.", false);
        });

        assertExpectedLoadedPolicies((ClientPoliciesRepresentation reps)->{
            ClientPolicyRepresentation rep =  getPolicyRepresentation(reps, "new-policy");
            assertExpectedPolicy("new-policy", "not existed and duplicated profiles are ignored.", false, true, Arrays.asList("builtin-default-profile", "ordinal-test-profile", "lack-of-builtin-field-test-profile"),
                    rep);
        });

        // update existing profiles

        String modifiedProfileDescription = "The profile has been updated.";
        ClientProfilesRepresentation actualProfilesRep = getProfiles();
        ClientProfilesBuilder profilesBuilder = new ClientProfilesBuilder();
        actualProfilesRep.getProfiles().stream().forEach(i->{
            if (i.getName().equals("ordinal-test-profile")) {
                i.setDescription(modifiedProfileDescription);
            }
            profilesBuilder.addProfile(i);
        });
        updateProfiles(profilesBuilder.toString());

        assertExpectedLoadedProfiles((ClientProfilesRepresentation reps)->{
            ClientProfileRepresentation rep =  getProfileRepresentation(reps, "ordinal-test-profile");
            assertExpectedProfile(rep, "ordinal-test-profile", modifiedProfileDescription, false);
        });

        // update existing policies

        String modifiedPolicyDescription = "The policy has also been updated.";
        ClientPoliciesRepresentation actualPoliciesRep = getPolicies();
        ClientPoliciesBuilder policiesBuilder = new ClientPoliciesBuilder();
        actualPoliciesRep.getPolicies().stream().forEach(i->{
            if (i.getName().equals("new-policy")) {
                i.setDescription(modifiedPolicyDescription);
                i.setEnable(null);
            }
            policiesBuilder.addPolicy(i);
        });
        updatePolicies(policiesBuilder.toString());

        assertExpectedLoadedPolicies((ClientPoliciesRepresentation reps)->{
            ClientPolicyRepresentation rep =  getPolicyRepresentation(reps, "new-policy");
            assertExpectedPolicy("new-policy", modifiedPolicyDescription, false, false, Arrays.asList("builtin-default-profile", "ordinal-test-profile", "lack-of-builtin-field-test-profile"),
                    rep);
        });

    }

    @Test
    public void testDuplicatedProfiles() throws Exception {
        String beforeUpdateProfilesJson = ClientPoliciesUtil.convertClientProfilesRepresentationToJson(getProfiles());

        // load profiles
        ClientProfileRepresentation duplicatedProfileRep = (new ClientProfileBuilder()).createProfile("builtin-basic-security", "Enforce basic security level", Boolean.TRUE, null)
                    .addExecutor(SecureClientAuthEnforceExecutorFactory.PROVIDER_ID, 
                        createSecureClientAuthEnforceExecutorConfig(
                            Boolean.FALSE, 
                            Arrays.asList(ClientIdAndSecretAuthenticator.PROVIDER_ID, JWTClientAuthenticator.PROVIDER_ID),
                            null))
                    .addExecutor(PKCEEnforceExecutorFactory.PROVIDER_ID, 
                        createPKCEEnforceExecutorConfig(Boolean.FALSE))
                    .addExecutor("no-such-executor", 
                            createPKCEEnforceExecutorConfig(Boolean.TRUE))
                    .toRepresentation();

        ClientProfileRepresentation loadedProfileRep = (new ClientProfileBuilder()).createProfile("ordinal-test-profile", "The profile that can be loaded.", Boolean.FALSE, null)
                .addExecutor(SecureClientAuthEnforceExecutorFactory.PROVIDER_ID, 
                    createSecureClientAuthEnforceExecutorConfig(
                        Boolean.TRUE, 
                        Arrays.asList(JWTClientAuthenticator.PROVIDER_ID),
                        JWTClientAuthenticator.PROVIDER_ID))
                .toRepresentation();

        String json = (new ClientProfilesBuilder())
                .addProfile(duplicatedProfileRep)
                .addProfile(loadedProfileRep)
                .addProfile(duplicatedProfileRep)
                .toString();
        try {
            updateProfiles(json);
        } catch (ClientPolicyException cpe) {
            assertEquals("Bad Request", cpe.getErrorDetail());
            String afterFailedUpdateProfilesJson = ClientPoliciesUtil.convertClientProfilesRepresentationToJson(getProfiles());
            assertEquals(beforeUpdateProfilesJson, afterFailedUpdateProfilesJson);
            return;
        }
        fail();
    }

    @Test
    public void testNullProfiles() throws Exception {
        String beforeUpdateProfilesJson = ClientPoliciesUtil.convertClientProfilesRepresentationToJson(getProfiles());

        String json = null;
        try {
            updateProfiles(json);
        } catch (ClientPolicyException cpe) {
            assertEquals("Bad Request", cpe.getErrorDetail());
            String afterFailedUpdateProfilesJson = ClientPoliciesUtil.convertClientProfilesRepresentationToJson(getProfiles());
            assertEquals(beforeUpdateProfilesJson, afterFailedUpdateProfilesJson);
            return;
        }
        fail();
    }

    @Test
    public void testInvalidFormattedJsonProfiles() throws Exception {
        String beforeUpdateProfilesJson = ClientPoliciesUtil.convertClientProfilesRepresentationToJson(getProfiles());

        String json = "{\n"
                + "    \"profiles\": [\n"
                + "        {\n"
                + "            \"name\" : \"ordinal-test-profile\",\n"
                + "            \"description\" : \"invalid , added.\",\n"
                + "            \"builtin\" : false,\n"
                + "            \"executors\": [\n"
                + "                {\n"
                + "                    \"new-secure-client-authn-executor\": {\n"
                + "                        \"client-authns\": [ \"private-key-jwt\" ],\n"
                + "                        \"client-authns-augment\" : \"private-key-jwt\",\n"
                + "                        \"is-augment\" : true\n"
                + "                    }\n"
                + "                }\n"
                + "            ]\n"
                + "        },\n"
                + "    ]\n"
                + "}";
        try {
            updateProfiles(json);
        } catch (ClientPolicyException cpe) {
            assertEquals("Bad Request", cpe.getErrorDetail());
            String afterFailedUpdateProfilesJson = ClientPoliciesUtil.convertClientProfilesRepresentationToJson(getProfiles());
            assertEquals(beforeUpdateProfilesJson, afterFailedUpdateProfilesJson);
            return;
        }
        fail();
    }

    @Test
    public void testInvalidFieldTypeJsonProfiles() throws Exception {
        String beforeUpdateProfilesJson = ClientPoliciesUtil.convertClientProfilesRepresentationToJson(getProfiles());

        String json = "{\n"
                + "    \"profiles\": [\n"
                + "        {\n"
                + "            \"name\" : \"ordinal-test-profile\",\n"
                + "            \"description\" : \"Not builtin profile that should be skipped.\",\n"
                + "            \"builtin\" : \"no\",\n"
                + "            \"executors\": [\n"
                + "                {\n"
                + "                    \"new-secure-client-authn-executor\": {\n"
                + "                        \"client-authns\": [ \"private-key-jwt\" ],\n"
                + "                        \"client-authns-augment\" : \"private-key-jwt\",\n"
                + "                        \"is-augment\" : true\n"
                + "                    }\n"
                + "                }\n"
                + "            ]\n"
                + "        }\n"
                + "    ]\n"
                + "}";
        try {
            updateProfiles(json);
        } catch (ClientPolicyException cpe) {
            assertEquals("Bad Request", cpe.getErrorDetail());
            String afterFailedUpdateProfilesJson = ClientPoliciesUtil.convertClientProfilesRepresentationToJson(getProfiles());
            assertEquals(beforeUpdateProfilesJson, afterFailedUpdateProfilesJson);
            return;
        }
        fail();
    }

    @Test
    public void testDuplicatedPolicies() throws Exception {
        String beforeUpdatePoliciesJson = ClientPoliciesUtil.convertClientPoliciesRepresentationToJson(getPolicies());

        // load policies
        ClientPolicyRepresentation duplicatedPoliciesRep = 
                (new ClientPolicyBuilder()).createPolicy(
                        "builtin-duplicated-new-policy",
                        "builtin duplicated new policy is ignored.",
                        Boolean.FALSE,
                        Boolean.TRUE,
                        null,
                        Arrays.asList("builtin-default-profile"))
                    .addCondition(ClientRolesConditionFactory.PROVIDER_ID, 
                        createClientRolesConditionConfig(Arrays.asList(SAMPLE_CLIENT_ROLE)))
                    .toRepresentation();

        ClientPolicyRepresentation loadedPolicyRep = 
                (new ClientPolicyBuilder()).createPolicy(
                        "new-policy",
                        "not existed and duplicated profiles are ignored.",
                        Boolean.FALSE,
                        Boolean.TRUE,
                        null,
                        Arrays.asList("lack-of-builtin-field-test-profile", "ordinal-test-profile"))
                    .addCondition(ClientAccessTypeConditionFactory.PROVIDER_ID, 
                        createClientAccessTypeConditionConfig(Arrays.asList(ClientAccessTypeConditionFactory.TYPE_PUBLIC, ClientAccessTypeConditionFactory.TYPE_BEARERONLY)))
                    .toRepresentation();

        String json = (new ClientPoliciesBuilder())
                .addPolicy(duplicatedPoliciesRep)
                .addPolicy(loadedPolicyRep)
                .addPolicy(duplicatedPoliciesRep)
                .toString();
        try {
            updatePolicies(json);
        } catch (ClientPolicyException cpe) {
            assertEquals("Bad Request", cpe.getErrorDetail());
            String afterFailedUpdatePoliciesJson = ClientPoliciesUtil.convertClientPoliciesRepresentationToJson(getPolicies());
            assertEquals(beforeUpdatePoliciesJson, afterFailedUpdatePoliciesJson);
            return;
        }
        fail();
    }

    @Test
    public void testNullPolicies() throws Exception {
        String beforeUpdatePoliciesJson = ClientPoliciesUtil.convertClientPoliciesRepresentationToJson(getPolicies());

        String json = null;
        try {
            updatePolicies(json);
        } catch (ClientPolicyException cpe) {
            assertEquals("Bad Request", cpe.getErrorDetail());
            String afterFailedUpdatePoliciesJson = ClientPoliciesUtil.convertClientPoliciesRepresentationToJson(getPolicies());
            assertEquals(beforeUpdatePoliciesJson, afterFailedUpdatePoliciesJson);
            return;
        }
        fail();
    }

    @Test
    public void testInvalidFormattedJsonPolicies() throws Exception {
        String beforeUpdatePoliciesJson = ClientPoliciesUtil.convertClientPoliciesRepresentationToJson(getPolicies());

        String json = "{\n"
                + "    \"policies\": [\n"
                + "        {\n"
                + "            \"name\": \"ordinal-test-policy\",\n"
                + "            \"description\" : \"bracket not enclosed properly.\",\n"
                + "            \"builtin\": false,\n"
                + "            \"enable\": true,\n"
                + "            \"conditions\": [\n"
                + "                {\n"
                + "                    \"new-clientupdatesourcehost-condition\": {\n"
                + "                        \"trusted-hosts\": [\"myuniversity\"],\n"
                + "                        \"host-sending-request-must-match\" : [true]\n"
                + "                    }\n"
                + "                }\n"
                + "            ],\n"
                + "            \"profiles\": [ \"builtin-advanced-security\" ]\n"
                + "        }\n"
                + "}";
        try {
            updatePolicies(json);
        } catch (ClientPolicyException cpe) {
            assertEquals("Bad Request", cpe.getErrorDetail());
            String afterFailedUpdatePoliciesJson = ClientPoliciesUtil.convertClientPoliciesRepresentationToJson(getPolicies());
            assertEquals(beforeUpdatePoliciesJson, afterFailedUpdatePoliciesJson);
            return;
        }
        fail();
    }

    @Test
    public void testInvalidFieldTypeJsonPolicies() throws Exception {
        String beforeUpdatePoliciesJson = ClientPoliciesUtil.convertClientPoliciesRepresentationToJson(getPolicies());

        String json = "{    \n"
                + "    \"policies\": [    \n"
                + "        {    \n"
                + "            \"name\": \"ordinal-test-policy\",    \n"
                + "            \"description\" : \"Not builtin policy that should be skipped.\",    \n"
                + "            \"builtin\": false,    \n"
                + "            \"enable\": true,    \n"
                + "            \"conditions\": true,    \n"
                + "            \"profiles\": [ \"builtin-advanced-security\" ]    \n"
                + "        }    \n"
                + "    ]    \n"
                + "}";
        try {
            updatePolicies(json);
        } catch (ClientPolicyException cpe) {
            assertEquals("Bad Request", cpe.getErrorDetail());
            String afterFailedUpdatePoliciesJson = ClientPoliciesUtil.convertClientPoliciesRepresentationToJson(getPolicies());
            assertEquals(beforeUpdatePoliciesJson, afterFailedUpdatePoliciesJson);
            return;
        }
        fail();
    }
}
