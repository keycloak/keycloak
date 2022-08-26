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

package org.keycloak.testsuite.console.realm;

import com.fasterxml.jackson.databind.JsonNode;
import org.jboss.arquillian.graphene.page.Page;
import org.junit.After;
import org.junit.Test;
import org.keycloak.authentication.authenticators.client.ClientIdAndSecretAuthenticator;
import org.keycloak.authentication.authenticators.client.JWTClientAuthenticator;
import org.keycloak.representations.idm.ClientPoliciesRepresentation;
import org.keycloak.representations.idm.ClientProfilesRepresentation;
import org.keycloak.representations.idm.ClientPolicyConditionConfigurationRepresentation;
import org.keycloak.services.clientpolicy.condition.ClientAccessTypeConditionFactory;
import org.keycloak.services.clientpolicy.executor.HolderOfKeyEnforcerExecutorFactory;
import org.keycloak.services.clientpolicy.executor.SecureClientAuthenticatorExecutorFactory;
import org.keycloak.testsuite.console.page.realm.clientpolicies.ClientPolicies;
import org.keycloak.testsuite.console.page.realm.clientpolicies.ClientPoliciesJson;
import org.keycloak.testsuite.console.page.realm.clientpolicies.ClientPolicy;
import org.keycloak.testsuite.console.page.realm.clientpolicies.ClientProfile;
import org.keycloak.testsuite.console.page.realm.clientpolicies.ClientProfiles;
import org.keycloak.testsuite.console.page.realm.clientpolicies.ClientProfilesJson;
import org.keycloak.testsuite.console.page.realm.clientpolicies.Condition;
import org.keycloak.testsuite.console.page.realm.clientpolicies.CreateClientPolicy;
import org.keycloak.testsuite.console.page.realm.clientpolicies.CreateClientProfile;
import org.keycloak.testsuite.console.page.realm.clientpolicies.CreateCondition;
import org.keycloak.testsuite.console.page.realm.clientpolicies.CreateExecutor;
import org.keycloak.testsuite.console.page.realm.clientpolicies.Executor;
import org.keycloak.testsuite.util.ClientPoliciesUtil.ClientPoliciesBuilder;
import org.keycloak.testsuite.util.ClientPoliciesUtil.ClientPolicyBuilder;
import org.keycloak.testsuite.util.ClientPoliciesUtil.ClientProfileBuilder;
import org.keycloak.testsuite.util.ClientPoliciesUtil.ClientProfilesBuilder;
import org.keycloak.util.JsonSerialization;

import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.keycloak.testsuite.util.ClientPoliciesUtil.createClientAccessTypeConditionConfig;
import static org.keycloak.testsuite.util.ClientPoliciesUtil.createHolderOfKeyEnforceExecutorConfig;
import static org.keycloak.testsuite.util.ClientPoliciesUtil.createSecureClientAuthenticatorExecutorConfig;
import static org.keycloak.testsuite.util.UIUtils.refreshPageAndWaitForLoad;

/**
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public class ClientPoliciesTest extends AbstractRealmTest {
    private static final String GLOBAL_PROFILE = "fapi-1-baseline";
    private static final String GLOBAL_EXECUTOR = "secure-session";

    @Page
    private ClientPolicies clientPoliciesPage;

    @Page
    private ClientPoliciesJson clientPoliciesJsonPage;

    @Page
    private ClientPolicy clientPolicyPage;

    @Page
    private ClientProfiles clientProfilesPage;

    @Page
    private ClientProfilesJson clientProfilesJsonPage;

    @Page
    private ClientProfile clientProfilePage;

    @Page
    private Condition conditionPage;

    @Page
    private Executor executorPage;

    @Page
    private CreateClientPolicy createClientPolicyPage;

    @Page
    private CreateClientProfile createClientProfilePage;

    @Page
    private CreateCondition createConditionPage;

    @Page
    private CreateExecutor createExecutorPage;

    @After
    public void cleanup() {
        testRealmResource().clientPoliciesPoliciesResource().updatePolicies(new ClientPoliciesRepresentation());
        testRealmResource().clientPoliciesProfilesResource().updateProfiles(new ClientProfilesRepresentation());
    }

    @Test
    public void testGlobalProfiles() {
        clientProfilesPage.navigateTo();
        clientProfilesPage.assertCurrent();

        assertTrue(clientProfilesPage.profilesTable().isGlobal(GLOBAL_PROFILE));
        assertFalse(clientProfilesPage.profilesTable().isDeleteBtnPresent(GLOBAL_PROFILE));

        clientProfilesPage.profilesTable().clickEditProfile(GLOBAL_PROFILE);

        clientProfilePage.setProfileName(GLOBAL_PROFILE);
        clientProfilePage.assertCurrent();
        assertTrue(clientProfilePage.form().isInputDisabled());
        assertFalse(clientProfilePage.executorsTable().isDeleteBtnPresent(GLOBAL_EXECUTOR));

        clientProfilePage.executorsTable().clickEditExecutor(GLOBAL_EXECUTOR);

        executorPage.setUriParameters(GLOBAL_PROFILE, 0);
        executorPage.assertCurrent();
    }

    @Test
    public void testProfilesFormView() throws Exception {
        final String profileName = "mega-profile";
        final String profileName2 = "mega-profile^2";
        final String profileDesc = "mega-desc";

        clientProfilesPage.navigateTo();
        clientProfilesPage.assertCurrent();

        clientProfilesPage.profilesTable().clickCreateProfile();
        createClientProfilePage.assertCurrent();

        // create profile
        createClientProfilePage.form().setProfileName(profileName);
        createClientProfilePage.form().setDescription(profileDesc);
        createClientProfilePage.form().save();
        assertAlertSuccess();

        clientProfilePage.setProfileName(profileName);
        clientProfilePage.assertCurrent();

        assertEquals(profileName, clientProfilePage.form().getProfileName());
        clientProfilePage.executorsTable().clickCreateExecutor();

        // create executors
        createExecutorPage.setProfileName(profileName);
        createExecutorPage.assertCurrent();
        createExecutorPage.form().setExecutorType(SecureClientAuthenticatorExecutorFactory.PROVIDER_ID);
        assertTrue(createExecutorPage.form().getSelect2SelectedItems().isEmpty());
        createExecutorPage.form().selectSelect2Item(JWTClientAuthenticator.PROVIDER_ID);
        createExecutorPage.form().selectSelect2Item(ClientIdAndSecretAuthenticator.PROVIDER_ID);
        createExecutorPage.form().save();
        assertAlertSuccess();

        clientProfilePage.assertCurrent();
        clientProfilePage.executorsTable().clickEditExecutor(SecureClientAuthenticatorExecutorFactory.PROVIDER_ID);
        executorPage.setUriParameters(profileName, 0);
        executorPage.assertCurrent();
        assertEquals(Stream.of(JWTClientAuthenticator.PROVIDER_ID, ClientIdAndSecretAuthenticator.PROVIDER_ID).collect(Collectors.toSet()), executorPage.form().getSelect2SelectedItems());

        createExecutorPage.navigateTo();
        createExecutorPage.form().setExecutorType(HolderOfKeyEnforcerExecutorFactory.PROVIDER_ID);
        assertFalse(createExecutorPage.form().isAutoConfigure());
        createExecutorPage.form().setAutoConfigure(true);
        createExecutorPage.form().save();

        clientProfilePage.executorsTable().clickEditExecutor(HolderOfKeyEnforcerExecutorFactory.PROVIDER_ID);
        executorPage.setUriParameters(profileName, 1);
        executorPage.assertCurrent();
        assertTrue(executorPage.form().isAutoConfigure());

        // assert JSON
        ClientProfilesRepresentation expected = new ClientProfilesBuilder()
                .addProfile(new ClientProfileBuilder()
                        .createProfile(profileName, profileDesc)
                        .addExecutor(SecureClientAuthenticatorExecutorFactory.PROVIDER_ID,
                                createSecureClientAuthenticatorExecutorConfig(Arrays.asList(JWTClientAuthenticator.PROVIDER_ID, ClientIdAndSecretAuthenticator.PROVIDER_ID), JWTClientAuthenticator.PROVIDER_ID))
                        .addExecutor(HolderOfKeyEnforcerExecutorFactory.PROVIDER_ID,
                                createHolderOfKeyEnforceExecutorConfig(true))
                        .toRepresentation())
                .toRepresentation();

        assertClientProfile(expected, false);

        // remove executor
        clientProfilePage.navigateTo();
        clientProfilePage.executorsTable().clickDeleteExecutor(SecureClientAuthenticatorExecutorFactory.PROVIDER_ID);
        modalDialog.confirmDeletion();
        assertAlertSuccess();
        expected.getProfiles().get(0).getExecutors().remove(0);
        assertClientProfile(expected, false);
        assertFalse(clientProfilePage.executorsTable().isRowPresent(SecureClientAuthenticatorExecutorFactory.PROVIDER_ID));

        // edit executor
        clientProfilePage.executorsTable().clickEditExecutor(HolderOfKeyEnforcerExecutorFactory.PROVIDER_ID);
        executorPage.form().setAutoConfigure(false);
        executorPage.form().save();
        expected.getProfiles().get(0).getExecutors().get(0).setConfiguration(JsonSerialization.mapper.readValue(JsonSerialization.mapper.writeValueAsBytes(createHolderOfKeyEnforceExecutorConfig(false)), JsonNode.class));
        assertClientProfile(expected, false);

        // edit profile
        clientProfilePage.form().setProfileName(profileName2);
        clientProfilePage.form().save();
        assertAlertSuccess();
        clientProfilesPage.navigateTo();
        assertEquals(profileDesc, clientProfilesPage.profilesTable().getDescription(profileName2));

        // remove profile
        clientProfilesPage.profilesTable().clickDeleteProfile(profileName2);
        modalDialog.confirmDeletion();
        assertAlertSuccess();
        assertClientProfile(new ClientProfilesRepresentation(), false);
        assertFalse(clientProfilesPage.profilesTable().isRowPresent(profileName2));
    }

    @Test
    public void testProfilesJsonView() throws Exception {
        clientProfilesJsonPage.navigateTo();

        ClientProfilesRepresentation profiles = testRealmResource().clientPoliciesProfilesResource().getProfiles(true);
        assertEquals(profiles, clientProfilesJsonPage.form().getProfiles());

        profiles.getProfiles().add(new ClientProfileBuilder()
                .createProfile("prof", "desc")
                .addExecutor(HolderOfKeyEnforcerExecutorFactory.PROVIDER_ID,
                        createHolderOfKeyEnforceExecutorConfig(true))
                .toRepresentation());

        testRealmResource().clientPoliciesProfilesResource().updateProfiles(profiles);
        refreshPageAndWaitForLoad();
        assertEquals(profiles, clientProfilesJsonPage.form().getProfiles());

        profiles.getProfiles().add(new ClientProfileBuilder().createProfile("prof2", "desc2").toRepresentation());
        clientProfilesJsonPage.form().setProfiles(profiles);
        clientProfilesJsonPage.form().save();
        assertAlertSuccess();
        assertClientProfile(profiles, true);

        clientProfilesJsonPage.form().setProfilesAsString("aaa");
        clientProfilesJsonPage.form().save();
        assertAlertDanger();
    }

    @Test
    public void testPoliciesFormView() throws Exception {
        final String profileName = "mega-profile";
        final String policyName = "mega-policy";
        final String policyName2 = "mega-policy^2";
        final String policyDesc = "mega-desc";

        clientPoliciesPage.navigateTo();
        clientPoliciesPage.assertCurrent();

        clientPoliciesPage.policiesTable().clickCreatePolicy();
        createClientPolicyPage.assertCurrent();

        // create policy
        createClientPolicyPage.form().setPolicyName(policyName);
        createClientPolicyPage.form().setDescription(policyDesc);
        assertTrue(createClientPolicyPage.form().isEnabled());
        createClientPolicyPage.form().save();
        assertAlertSuccess();

        clientPolicyPage.setPolicyName(policyName);
        clientPolicyPage.assertCurrent();

        assertEquals(policyName, clientPolicyPage.form().getPolicyName());
        clientPolicyPage.conditionsTable().clickCreateCondition();

        // create condition
        createConditionPage.setPolicyName(policyName);
        createConditionPage.assertCurrent();
        createConditionPage.form().setConditionType(ClientAccessTypeConditionFactory.PROVIDER_ID);
        assertEquals(Stream.of(ClientAccessTypeConditionFactory.TYPE_CONFIDENTIAL).collect(Collectors.toSet()), conditionPage.form().getSelect2SelectedItems());
        createConditionPage.form().selectSelect2Item(ClientAccessTypeConditionFactory.TYPE_BEARERONLY);
        createConditionPage.form().save();
        assertAlertSuccess();

        // edit condition
        clientPolicyPage.assertCurrent();
        clientPolicyPage.conditionsTable().clickEditCondition(ClientAccessTypeConditionFactory.PROVIDER_ID);
        conditionPage.setUriParameters(policyName, 0);
        conditionPage.assertCurrent();
        assertEquals(Stream.of(ClientAccessTypeConditionFactory.TYPE_CONFIDENTIAL, ClientAccessTypeConditionFactory.TYPE_BEARERONLY).collect(Collectors.toSet()), conditionPage.form().getSelect2SelectedItems());
        createConditionPage.form().selectSelect2Item(ClientAccessTypeConditionFactory.TYPE_PUBLIC);
        createConditionPage.form().save();

        // create profile via REST
        ClientProfilesRepresentation profiles = new ClientProfilesBuilder()
        .addProfile(new ClientProfileBuilder()
                .createProfile(profileName, "desc")
                .addExecutor(HolderOfKeyEnforcerExecutorFactory.PROVIDER_ID,
                        createHolderOfKeyEnforceExecutorConfig(true))
                .toRepresentation())
        .toRepresentation();
        testRealmResource().clientPoliciesProfilesResource().updateProfiles(profiles);
        refreshPageAndWaitForLoad();

        // add profile to policy
        clientPolicyPage.profilesTable().addProfile(GLOBAL_PROFILE);
        clientPolicyPage.profilesTable().addProfile(profileName);
        assertEquals(Arrays.asList(GLOBAL_PROFILE, profileName), clientPolicyPage.profilesTable().getProfiles());

        // remove profile
        clientPolicyPage.profilesTable().clickDeleteProfile(GLOBAL_PROFILE);
        assertAlertSuccess();

        // assert JSON
        ClientPolicyConditionConfigurationRepresentation conditionConfig =
                createClientAccessTypeConditionConfig(Arrays.asList(ClientAccessTypeConditionFactory.TYPE_CONFIDENTIAL, ClientAccessTypeConditionFactory.TYPE_BEARERONLY, ClientAccessTypeConditionFactory.TYPE_PUBLIC));
        conditionConfig.setNegativeLogic(Boolean.FALSE);

        ClientPoliciesRepresentation expected = new ClientPoliciesBuilder()
            .addPolicy(new ClientPolicyBuilder()
                .createPolicy(policyName, policyDesc, true)
                .addCondition(ClientAccessTypeConditionFactory.PROVIDER_ID, conditionConfig)
                .addProfile(profileName)
                .toRepresentation())
            .toRepresentation();
        assertClientPolicy(expected);

        // remove condition
        clientPolicyPage.navigateTo();
        clientPolicyPage.conditionsTable().clickDeleteCondition(ClientAccessTypeConditionFactory.PROVIDER_ID);
        modalDialog.confirmDeletion();
        assertAlertSuccess();
        expected.getPolicies().get(0).getConditions().remove(0);
        assertClientPolicy(expected);
        assertFalse(clientPolicyPage.conditionsTable().isRowPresent(ClientAccessTypeConditionFactory.PROVIDER_ID));

        // edit policy
        clientPolicyPage.form().setPolicyName(policyName2);
        clientPolicyPage.form().setEnabled(false);
        clientPolicyPage.form().save();
        assertAlertSuccess();
        clientPoliciesPage.navigateTo();
        assertEquals(policyDesc, clientPoliciesPage.policiesTable().getDescription(policyName2));
        assertFalse(clientPoliciesPage.policiesTable().isEnabled(policyName2));

        // remove policy
        clientPoliciesPage.policiesTable().clickDeletePolicy(policyName2);
        modalDialog.confirmDeletion();
        assertAlertSuccess();
        assertClientPolicy(new ClientPoliciesRepresentation());
        assertFalse(clientPoliciesPage.policiesTable().isRowPresent(policyName2));
    }

    @Test
    public void testPoliciesJsonView() throws Exception {
        clientPoliciesJsonPage.navigateTo();
        assertEquals(new ClientPoliciesRepresentation(), clientPoliciesJsonPage.form().getPolicies());

        ClientPoliciesRepresentation policies = new ClientPoliciesBuilder()
                .addPolicy(new ClientPolicyBuilder()
                        .createPolicy("prof", "desc", false)
                        .addCondition(ClientAccessTypeConditionFactory.PROVIDER_ID,
                                createClientAccessTypeConditionConfig(Arrays.asList(ClientAccessTypeConditionFactory.TYPE_CONFIDENTIAL, ClientAccessTypeConditionFactory.TYPE_BEARERONLY, ClientAccessTypeConditionFactory.TYPE_PUBLIC)))
                        .toRepresentation())
                .toRepresentation();

        testRealmResource().clientPoliciesPoliciesResource().updatePolicies(policies);
        refreshPageAndWaitForLoad();
        assertEquals(policies, clientPoliciesJsonPage.form().getPolicies());

        policies.getPolicies().add(new ClientPolicyBuilder().createPolicy("prof2", "desc2", true).toRepresentation());
        clientPoliciesJsonPage.form().setPolicies(policies);
        clientPoliciesJsonPage.form().save();
        assertAlertSuccess();
        assertClientPolicy(policies);

        clientPoliciesJsonPage.form().setPoliciesAsString("aaa");
        clientPoliciesJsonPage.form().save();
        assertAlertDanger();
    }

    private void assertClientProfile(ClientProfilesRepresentation expected, boolean includeGlobalProfiles) {
        ClientProfilesRepresentation actual = testRealmResource().clientPoliciesProfilesResource().getProfiles(includeGlobalProfiles);
        assertEquals(expected, actual);
    }

    private void assertClientPolicy(ClientPoliciesRepresentation expected) {
        ClientPoliciesRepresentation actual = testRealmResource().clientPoliciesPoliciesResource().getPolicies();
        assertEquals(expected, actual);
    }
}
