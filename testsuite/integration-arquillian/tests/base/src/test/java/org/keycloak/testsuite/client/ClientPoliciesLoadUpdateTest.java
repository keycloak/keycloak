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

import org.hamcrest.Matchers;
import org.junit.Test;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.authentication.authenticators.client.ClientIdAndSecretAuthenticator;
import org.keycloak.authentication.authenticators.client.JWTClientAuthenticator;
import org.keycloak.authentication.authenticators.client.JWTClientSecretAuthenticator;
import org.keycloak.authentication.authenticators.client.X509ClientAuthenticator;
import org.keycloak.representations.idm.ClientPoliciesRepresentation;
import org.keycloak.representations.idm.ClientPolicyRepresentation;
import org.keycloak.representations.idm.ClientProfileRepresentation;
import org.keycloak.representations.idm.ClientProfilesRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.services.clientpolicy.ClientPoliciesUtil;
import org.keycloak.services.clientpolicy.ClientPolicyException;
import org.keycloak.services.clientpolicy.condition.ClientAccessTypeConditionFactory;
import org.keycloak.services.clientpolicy.condition.ClientRolesConditionFactory;
import org.keycloak.services.clientpolicy.executor.ConsentRequiredExecutorFactory;
import org.keycloak.services.clientpolicy.executor.FullScopeDisabledExecutorFactory;
import org.keycloak.services.clientpolicy.executor.PKCEEnforcerExecutorFactory;
import org.keycloak.services.clientpolicy.executor.SecureClientAuthenticatorExecutorFactory;
import org.keycloak.services.clientpolicy.executor.SecureClientUrisExecutorFactory;
import org.keycloak.services.clientpolicy.executor.SecureSessionEnforceExecutorFactory;
import org.keycloak.testsuite.Assert;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude;
import org.keycloak.testsuite.util.ClientPoliciesUtil.ClientPoliciesBuilder;
import org.keycloak.testsuite.util.ClientPoliciesUtil.ClientPolicyBuilder;
import org.keycloak.testsuite.util.ClientPoliciesUtil.ClientProfileBuilder;
import org.keycloak.testsuite.util.ClientPoliciesUtil.ClientProfilesBuilder;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.keycloak.testsuite.admin.AbstractAdminTest.loadJson;
import static org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude.AuthServer.REMOTE;
import static org.keycloak.testsuite.util.ClientPoliciesUtil.createClientAccessTypeConditionConfig;
import static org.keycloak.testsuite.util.ClientPoliciesUtil.createClientRolesConditionConfig;
import static org.keycloak.testsuite.util.ClientPoliciesUtil.createPKCEEnforceExecutorConfig;
import static org.keycloak.testsuite.util.ClientPoliciesUtil.createSecureClientAuthenticatorExecutorConfig;

/**
 * @author <a href="mailto:takashi.norimatsu.ws@hitachi.com">Takashi Norimatsu</a>
 */
@AuthServerContainerExclude({REMOTE})
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
        // retrieve loaded global profiles
        ClientProfilesRepresentation actualProfilesRep = getProfilesWithGlobals();

        // same profiles
        assertExpectedProfiles(actualProfilesRep, Arrays.asList(FAPI1_BASELINE_PROFILE_NAME, FAPI1_ADVANCED_PROFILE_NAME), Collections.emptyList());

        // each profile - fapi-1-baseline
        ClientProfileRepresentation actualProfileRep =  getProfileRepresentation(actualProfilesRep, FAPI1_BASELINE_PROFILE_NAME, true);
        assertExpectedProfile(actualProfileRep, FAPI1_BASELINE_PROFILE_NAME, "Client profile, which enforce clients to conform 'Financial-grade API Security Profile 1.0 - Part 1: Baseline' specification.");

        // Test some executor
        assertExpectedExecutors(Arrays.asList(SecureSessionEnforceExecutorFactory.PROVIDER_ID, PKCEEnforcerExecutorFactory.PROVIDER_ID, SecureClientAuthenticatorExecutorFactory.PROVIDER_ID,
                SecureClientUrisExecutorFactory.PROVIDER_ID, ConsentRequiredExecutorFactory.PROVIDER_ID, FullScopeDisabledExecutorFactory.PROVIDER_ID), actualProfileRep);
        assertExpectedSecureSessionEnforceExecutor(actualProfileRep);

        // Check the "get" request without globals. Assert nothing loaded
        actualProfilesRep = getProfilesWithoutGlobals();
        assertExpectedProfiles(actualProfilesRep, null, Collections.emptyList());

        // retrieve loaded builtin policies
        ClientPoliciesRepresentation actualPoliciesRep = getPolicies();

        // No global policies expected
        assertExpectedPolicies(Collections.emptyList(), actualPoliciesRep);
        ClientPolicyRepresentation actualPolicyRep =  getPolicyRepresentation(actualPoliciesRep, "builtin-default-policy");
        Assert.assertNull(actualPolicyRep);
    }

    @Test
    public void testUpdateValidProfilesAndPolicies() throws Exception {
        setupValidProfilesAndPolicies();

        assertExpectedLoadedProfiles((ClientProfilesRepresentation reps)->{
            ClientProfileRepresentation rep =  getProfileRepresentation(reps, "ordinal-test-profile", false);
            assertExpectedProfile(rep, "ordinal-test-profile", "The profile that can be loaded.");
        });

        assertExpectedLoadedPolicies((ClientPoliciesRepresentation reps)->{
            ClientPolicyRepresentation rep =  getPolicyRepresentation(reps, "new-policy");
            assertExpectedPolicy("new-policy", "duplicated profiles are ignored.", true, Arrays.asList("ordinal-test-profile", "lack-of-builtin-field-test-profile"),
                    rep);
        });

        // update existing profiles

        String modifiedProfileDescription = "The profile has been updated.";
        ClientProfilesRepresentation actualProfilesRep = getProfilesWithoutGlobals();
        ClientProfilesBuilder profilesBuilder = new ClientProfilesBuilder();
        actualProfilesRep.getProfiles().stream().forEach(i->{
            if (i.getName().equals("ordinal-test-profile")) {
                i.setDescription(modifiedProfileDescription);
            }
            profilesBuilder.addProfile(i);
        });
        updateProfiles(profilesBuilder.toString());

        assertExpectedLoadedProfiles((ClientProfilesRepresentation reps)->{
            ClientProfileRepresentation rep =  getProfileRepresentation(reps, "ordinal-test-profile", false);
            assertExpectedProfile(rep, "ordinal-test-profile", modifiedProfileDescription);
        });

        // update existing policies

        String modifiedPolicyDescription = "The policy has also been updated.";
        ClientPoliciesRepresentation actualPoliciesRep = getPolicies();
        ClientPoliciesBuilder policiesBuilder = new ClientPoliciesBuilder();
        actualPoliciesRep.getPolicies().stream().forEach(i->{
            if (i.getName().equals("new-policy")) {
                i.setDescription(modifiedPolicyDescription);
                i.setEnabled(null);
            }
            policiesBuilder.addPolicy(i);
        });
        updatePolicies(policiesBuilder.toString());

        assertExpectedLoadedPolicies((ClientPoliciesRepresentation reps)->{
            ClientPolicyRepresentation rep =  getPolicyRepresentation(reps, "new-policy");
            assertExpectedPolicy("new-policy", modifiedPolicyDescription, false, Arrays.asList("ordinal-test-profile", "lack-of-builtin-field-test-profile"),
                    rep);
        });

    }

    @Test
    public void testDuplicatedProfiles() throws Exception {
        String beforeUpdateProfilesJson = ClientPoliciesUtil.convertClientProfilesRepresentationToJson(getProfilesWithGlobals());

        // load profiles
        ClientProfileRepresentation duplicatedProfileRep = (new ClientProfileBuilder()).createProfile("builtin-basic-security", "Enforce basic security level")
                    .addExecutor(SecureClientAuthenticatorExecutorFactory.PROVIDER_ID,
                        createSecureClientAuthenticatorExecutorConfig(
                            Arrays.asList(ClientIdAndSecretAuthenticator.PROVIDER_ID, JWTClientAuthenticator.PROVIDER_ID),
                            null))
                    .addExecutor(PKCEEnforcerExecutorFactory.PROVIDER_ID,
                        createPKCEEnforceExecutorConfig(Boolean.FALSE))
                    .addExecutor("no-such-executor", 
                            createPKCEEnforceExecutorConfig(Boolean.TRUE))
                    .toRepresentation();

        ClientProfileRepresentation loadedProfileRep = (new ClientProfileBuilder()).createProfile("ordinal-test-profile", "The profile that can be loaded.")
                .addExecutor(SecureClientAuthenticatorExecutorFactory.PROVIDER_ID,
                    createSecureClientAuthenticatorExecutorConfig(
                        Collections.singletonList(JWTClientAuthenticator.PROVIDER_ID),
                        JWTClientAuthenticator.PROVIDER_ID))
                .toRepresentation();

        String json = (new ClientProfilesBuilder())
                .addProfile(duplicatedProfileRep)
                .addProfile(loadedProfileRep)
                .addProfile(duplicatedProfileRep)
                .toString();
        try {
            updateProfiles(json);
            fail();
        } catch (ClientPolicyException cpe) {
            assertEquals("Bad Request", cpe.getErrorDetail());
            String afterFailedUpdateProfilesJson = ClientPoliciesUtil.convertClientProfilesRepresentationToJson(getProfilesWithGlobals());
            assertEquals(beforeUpdateProfilesJson, afterFailedUpdateProfilesJson);
        }
    }

    @Test
    public void testOverwriteBuiltinProfileNotAllowed() throws Exception {
        // register profiles
        String json =
                (new ClientProfilesBuilder()).addProfile(
                    (new ClientProfileBuilder()).createProfile(FAPI1_BASELINE_PROFILE_NAME, "Pershyy Profil")
                        .addExecutor(SecureClientAuthenticatorExecutorFactory.PROVIDER_ID,
                                createSecureClientAuthenticatorExecutorConfig(
                                        Arrays.asList(JWTClientAuthenticator.PROVIDER_ID, JWTClientSecretAuthenticator.PROVIDER_ID, X509ClientAuthenticator.PROVIDER_ID),
                                        X509ClientAuthenticator.PROVIDER_ID))
                        .toRepresentation()
                ).toRepresentation().toString();
        try {
            updateProfiles(json);
            fail();
        } catch (ClientPolicyException cpe) {
            assertEquals("update profiles failed", cpe.getError());
        }
    }

    @Test
    public void testNullProfiles() throws Exception {
        String beforeUpdateProfilesJson = ClientPoliciesUtil.convertClientProfilesRepresentationToJson(getProfilesWithGlobals());

        String json = null;
        try {
            updateProfiles(json);
        } catch (ClientPolicyException cpe) {
            assertEquals("argument \"content\" is null", cpe.getErrorDetail());
            String afterFailedUpdateProfilesJson = ClientPoliciesUtil.convertClientProfilesRepresentationToJson(getProfilesWithGlobals());
            assertEquals(beforeUpdateProfilesJson, afterFailedUpdateProfilesJson);
            return;
        }
        fail();
    }

    @Test
    public void testInvalidFormattedJsonProfiles() throws Exception {
        String beforeUpdateProfilesJson = ClientPoliciesUtil.convertClientProfilesRepresentationToJson(getProfilesWithGlobals());

        String json = "{\n"
                + "    \"profiles\": [\n"
                + "        {\n"
                + "            \"name\" : \"ordinal-test-profile\",\n"
                + "            \"description\" : \"invalid , added.\",\n"
                + "            \"builtin\" : false,\n"
                + "            \"executors\": [\n"
                + "                {\n"
                + "                    \"new-secure-client-authnenticator\": {\n"
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
            assertThat(cpe.getErrorDetail(), Matchers.startsWith("Unrecognized field"));
            String afterFailedUpdateProfilesJson = ClientPoliciesUtil.convertClientProfilesRepresentationToJson(getProfilesWithGlobals());
            assertEquals(beforeUpdateProfilesJson, afterFailedUpdateProfilesJson);
            return;
        }
        fail();
    }

    @Test
    public void testInvalidFieldTypeJsonProfiles() throws Exception {
        String beforeUpdateProfilesJson = ClientPoliciesUtil.convertClientProfilesRepresentationToJson(getProfilesWithGlobals());

        String json = "{\n"
                + "    \"profiles\": [\n"
                + "        {\n"
                + "            \"name\" : \"ordinal-test-profile\",\n"
                + "            \"description\" : \"Not builtin profile that should be skipped.\",\n"
                + "            \"builtin\" : \"no\",\n"
                + "            \"executors\": {\n"
                + "                    \"new-secure-client-authnenticator\": {\n"
                + "                        \"client-authns\": [ \"private-key-jwt\" ],\n"
                + "                        \"client-authns-augment\" : \"private-key-jwt\",\n"
                + "                        \"is-augment\" : true\n"
                + "                    }\n"
                + "            ]\n"
                + "        }\n"
                + "    ]\n"
                + "}";
        try {
            updateProfiles(json);
        } catch (ClientPolicyException cpe) {
            assertThat(cpe.getErrorDetail(), Matchers.startsWith("Unrecognized field "));
            String afterFailedUpdateProfilesJson = ClientPoliciesUtil.convertClientProfilesRepresentationToJson(getProfilesWithGlobals());
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
                        Boolean.TRUE)
                    .addCondition(ClientRolesConditionFactory.PROVIDER_ID, 
                        createClientRolesConditionConfig(Arrays.asList(SAMPLE_CLIENT_ROLE)))
                        .addProfile(FAPI1_BASELINE_PROFILE_NAME)
                    .toRepresentation();

        ClientPolicyRepresentation loadedPolicyRep = 
                (new ClientPolicyBuilder()).createPolicy(
                        "new-policy",
                        "duplicated profiles are ignored.",
                        Boolean.TRUE)
                    .addCondition(ClientAccessTypeConditionFactory.PROVIDER_ID, 
                        createClientAccessTypeConditionConfig(Arrays.asList(ClientAccessTypeConditionFactory.TYPE_PUBLIC, ClientAccessTypeConditionFactory.TYPE_BEARERONLY)))
                        .addProfile("lack-of-builtin-field-test-profile")
                        .addProfile("ordinal-test-profile")
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
                + "                    \"new-client-updater-source-host\": {\n"
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
            assertThat(cpe.getErrorDetail(), Matchers.startsWith("Unrecognized field "));
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
            assertThat(cpe.getErrorDetail(), Matchers.startsWith("Unrecognized field "));
            String afterFailedUpdatePoliciesJson = ClientPoliciesUtil.convertClientPoliciesRepresentationToJson(getPolicies());
            assertEquals(beforeUpdatePoliciesJson, afterFailedUpdatePoliciesJson);
            return;
        }
        fail();
    }

    // Test that regular CRUD of realm representation object through admin REST API does not remove
    @Test
    public void testCRUDRealmRepresentation() throws Exception {
        setupValidProfilesAndPolicies();

        // Get the realm and assert that expected policies and profiles are present
        RealmResource testRealm = realmsResouce().realm("test");
        RealmRepresentation realmRep = testRealm.toRepresentation();
        assertExpectedProfiles(realmRep.getParsedClientProfiles(), null, Arrays.asList("ordinal-test-profile", "lack-of-builtin-field-test-profile"));
        assertExpectedPolicies(Arrays.asList("new-policy", "lack-of-builtin-field-test-policy"), realmRep.getParsedClientPolicies());

        // Update the realm
        testRealm.update(realmRep);

        // Test the realm again
        realmRep = testRealm.toRepresentation();
        assertExpectedProfiles(realmRep.getParsedClientProfiles(), null, Arrays.asList("ordinal-test-profile", "lack-of-builtin-field-test-profile"));
        assertExpectedPolicies(Arrays.asList("new-policy", "lack-of-builtin-field-test-policy"), realmRep.getParsedClientPolicies());
    }
}
