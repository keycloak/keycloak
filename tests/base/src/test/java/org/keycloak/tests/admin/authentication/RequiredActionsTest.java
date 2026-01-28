/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.tests.admin.authentication;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.NotFoundException;

import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.representations.idm.RequiredActionConfigInfoRepresentation;
import org.keycloak.representations.idm.RequiredActionConfigRepresentation;
import org.keycloak.representations.idm.RequiredActionProviderRepresentation;
import org.keycloak.representations.idm.RequiredActionProviderSimpleRepresentation;
import org.keycloak.testframework.annotations.InjectAdminEvents;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.events.AdminEventAssertion;
import org.keycloak.testframework.events.AdminEvents;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;
import org.keycloak.tests.utils.admin.AdminEventPaths;
import org.keycloak.testsuite.actions.DummyConfigurableRequiredActionFactory;
import org.keycloak.testsuite.actions.DummyRequiredActionFactory;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 */
@KeycloakIntegrationTest(config = RequiredActionsTest.CustomProvidersServerConfig.class)
public class RequiredActionsTest extends AbstractAuthenticationTest {

    @InjectRealm
    ManagedRealm managedRealm;

    @InjectAdminEvents
    AdminEvents adminEvents;

    @Test
    public void testRequiredActions() {
        List<RequiredActionProviderRepresentation> result = authMgmtResource.getRequiredActions();

        List<RequiredActionProviderRepresentation> expected = new ArrayList<>();
        addRequiredAction(expected, "CONFIGURE_RECOVERY_AUTHN_CODES", "Recovery Authentication Codes", true, false, null);
        addRequiredAction(expected, "CONFIGURE_TOTP", "Configure OTP", true, false, null);
        addRequiredAction(expected, "TERMS_AND_CONDITIONS", "Terms and Conditions", false, false, null);
        addRequiredAction(expected, "UPDATE_EMAIL", "Update Email", false, false, null);
        addRequiredAction(expected, "UPDATE_PASSWORD", "Update Password", true, false, null);
        addRequiredAction(expected, "UPDATE_PROFILE", "Update Profile", true, false, null);
        addRequiredAction(expected, "VERIFY_EMAIL", "Verify Email", true, false, null);
        addRequiredAction(expected, "VERIFY_PROFILE", "Verify Profile", true, false, null);
        addRequiredAction(expected, "delete_account", "Delete Account", false, false, null);
        addRequiredAction(expected, "delete_credential", "Delete Credential", true, false, null);
        addRequiredAction(expected, "idp_link", "Linking Identity Provider", true, false, null);
        addRequiredAction(expected, "update_user_locale", "Update User Locale", true, false, null);
        addRequiredAction(expected, "webauthn-register", "Webauthn Register", true, false, null);
        addRequiredAction(expected, "webauthn-register-passwordless", "Webauthn Register Passwordless", true, false, null);

        compareRequiredActions(expected, sort(result));

        RequiredActionProviderRepresentation forUpdate = newRequiredAction("VERIFY_EMAIL", "Verify Email", false, false, null);
        authMgmtResource.updateRequiredAction(forUpdate.getAlias(), forUpdate);
        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.UPDATE, AdminEventPaths.authRequiredActionPath(forUpdate.getAlias()), ResourceType.REQUIRED_ACTION);

        result = authMgmtResource.getRequiredActions();
        RequiredActionProviderRepresentation updated = findRequiredActionByAlias(forUpdate.getAlias(), result);

        Assertions.assertNotNull(updated, "Required Action still there");
        compareRequiredAction(forUpdate, updated);

        forUpdate.setConfig(Collections.<String, String>emptyMap());
        authMgmtResource.updateRequiredAction(forUpdate.getAlias(), forUpdate);
        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.UPDATE, AdminEventPaths.authRequiredActionPath(forUpdate.getAlias()), ResourceType.REQUIRED_ACTION);

        result = authMgmtResource.getRequiredActions();
        updated = findRequiredActionByAlias(forUpdate.getAlias(), result);

        Assertions.assertNotNull(updated, "Required Action still there");
        compareRequiredAction(forUpdate, updated);
    }

    @Test
    public void testCRUDRequiredAction() {
        int lastPriority = authMgmtResource.getRequiredActions().get(authMgmtResource.getRequiredActions().size() - 1).getPriority();

        // Dummy RequiredAction is not registered in the realm and WebAuthn actions
        List<RequiredActionProviderSimpleRepresentation> result = authMgmtResource.getUnregisteredRequiredActions();
        Assertions.assertEquals(2, result.size());
        RequiredActionProviderSimpleRepresentation action = result.stream().filter(
                a -> a.getProviderId().equals(DummyRequiredActionFactory.PROVIDER_ID)
        ).findFirst().get();
        Assertions.assertEquals(DummyRequiredActionFactory.PROVIDER_ID, action.getProviderId());
        Assertions.assertEquals("Dummy Action", action.getName());

        // Register it
        authMgmtResource.registerRequiredAction(action);
        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.CREATE, AdminEventPaths.authMgmtBasePath() + "/register-required-action", action, ResourceType.REQUIRED_ACTION);

        // Try to register 2nd time
        try {
            authMgmtResource.registerRequiredAction(action);
        } catch (ClientErrorException ex) {
            // Expected
        }

        // Try to register required action with fake providerId
        RequiredActionProviderSimpleRepresentation requiredAction = new RequiredActionProviderSimpleRepresentation();
        requiredAction.setName("not-existent");
        requiredAction.setProviderId("not-existent");
        try {
            authMgmtResource.registerRequiredAction(requiredAction);
            Assertions.fail("Didn't expect to register requiredAction with providerId: 'not-existent'");
        } catch (Exception ex) {
            // Expected
        }

        // Try to find not-existent action - should fail
        try {
            authMgmtResource.getRequiredAction("not-existent");
            Assertions.fail("Didn't expect to find requiredAction of alias 'not-existent'");
        } catch (NotFoundException nfe) {
            // Expected
        }

        // Find existent
        RequiredActionProviderRepresentation rep = authMgmtResource.getRequiredAction(DummyRequiredActionFactory.PROVIDER_ID);
        compareRequiredAction(rep, newRequiredAction(DummyRequiredActionFactory.PROVIDER_ID, "Dummy Action",
                true, false, Collections.<String, String>emptyMap()));

        // Confirm the registered priority - should be N + 1
        Assertions.assertEquals(lastPriority + 1, rep.getPriority());

        // Update not-existent - should fail
        try {
            authMgmtResource.updateRequiredAction("not-existent", rep);
            Assertions.fail("Not expected to update not-existent requiredAction");
        } catch (NotFoundException nfe) {
            // Expected
        }

        // Update (set it as defaultAction)
        rep.setDefaultAction(true);
        authMgmtResource.updateRequiredAction(DummyRequiredActionFactory.PROVIDER_ID, rep);
        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.UPDATE, AdminEventPaths.authRequiredActionPath(rep.getAlias()), rep, ResourceType.REQUIRED_ACTION);
        compareRequiredAction(rep, newRequiredAction(DummyRequiredActionFactory.PROVIDER_ID, "Dummy Action",
                true, true, Collections.<String, String>emptyMap()));

        // Remove unexistent - should fail
        try {
            authMgmtResource.removeRequiredAction("not-existent");
            Assertions.fail("Not expected to remove not-existent requiredAction");
        } catch (NotFoundException nfe) {
            // Expected
        }

        // Remove success
        authMgmtResource.removeRequiredAction(DummyRequiredActionFactory.PROVIDER_ID);
        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.DELETE, AdminEventPaths.authRequiredActionPath(rep.getAlias()), ResourceType.REQUIRED_ACTION);

    }

    @Test
    public void testConfigurableRequiredActionMetadata() {

        String providerId = DummyConfigurableRequiredActionFactory.PROVIDER_ID;

        // query configurable properties
        RequiredActionConfigInfoRepresentation requiredActionConfigDescription = authMgmtResource.getRequiredActionConfigDescription(providerId);
        Assertions.assertNotNull(requiredActionConfigDescription);
        Assertions.assertNotNull(requiredActionConfigDescription.getProperties());
        Assertions.assertEquals(3, requiredActionConfigDescription.getProperties().size());
    }

    @Test
    public void testCRUDConfigurableRequiredAction() {
        int lastPriority = authMgmtResource.getRequiredActions().get(authMgmtResource.getRequiredActions().size() - 1).getPriority();

        // Dummy RequiredAction is not registered in the realm and WebAuthn actions
        List<RequiredActionProviderSimpleRepresentation> result = authMgmtResource.getUnregisteredRequiredActions();
        Assertions.assertEquals(2, result.size());
        String providerId = DummyConfigurableRequiredActionFactory.PROVIDER_ID;
        RequiredActionProviderSimpleRepresentation action = result.stream().filter(
                a -> providerId.equals(a.getProviderId())
        ).findFirst().get();
        Assertions.assertEquals(providerId, action.getProviderId());
        Assertions.assertEquals("Configurable Test Action", action.getName());

        // Register it
        authMgmtResource.registerRequiredAction(action);
        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.CREATE, AdminEventPaths.authMgmtBasePath() + "/register-required-action", action, ResourceType.REQUIRED_ACTION);

        RequiredActionConfigRepresentation requiredActionConfigRep = new RequiredActionConfigRepresentation();
        Map<String, String> newActionConfig = Map.ofEntries(Map.entry("setting1", "value1"), Map.entry("setting2", "false"));
        requiredActionConfigRep.setConfig(newActionConfig);

        authMgmtResource.updateRequiredActionConfig(providerId, requiredActionConfigRep);
        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.UPDATE, AdminEventPaths.authRequiredActionConfigPath(providerId), ResourceType.REQUIRED_ACTION_CONFIG);

        RequiredActionConfigRepresentation savedRequiredActionConfigRep = authMgmtResource.getRequiredActionConfig(providerId);
        Assertions.assertNotNull(savedRequiredActionConfigRep);
        Assertions.assertNotNull(savedRequiredActionConfigRep.getConfig());
        Assertions.assertTrue(savedRequiredActionConfigRep.getConfig().entrySet().containsAll(newActionConfig.entrySet()));

        // delete config
        authMgmtResource.removeRequiredActionConfig(providerId);
        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.DELETE, AdminEventPaths.authRequiredActionConfigPath(providerId), ResourceType.REQUIRED_ACTION_CONFIG);

        RequiredActionProviderRepresentation rep = authMgmtResource.getRequiredAction(providerId);

        // Remove success
        authMgmtResource.removeRequiredAction(providerId);
        AdminEventAssertion.assertEvent(adminEvents.poll(), OperationType.DELETE, AdminEventPaths.authRequiredActionPath(rep.getAlias()), ResourceType.REQUIRED_ACTION);

        // Retrieval after deletion should throw a NotFound exception
        try {
            authMgmtResource.getRequiredActionConfig(providerId);
        } catch (Exception ex) {
            Assertions.assertTrue(NotFoundException.class.isInstance(ex));
        }
    }

    private RequiredActionProviderRepresentation findRequiredActionByAlias(String alias, List<RequiredActionProviderRepresentation> list) {
        for (RequiredActionProviderRepresentation a: list) {
            if (alias.equals(a.getAlias())) {
                return a;
            }
        }
        return null;
    }

    private List<RequiredActionProviderRepresentation> sort(List<RequiredActionProviderRepresentation> list) {
        ArrayList<RequiredActionProviderRepresentation> sorted = new ArrayList<>(list);
        Collections.sort(sorted, new RequiredActionProviderComparator());
        return sorted;
    }

    private void compareRequiredActions(List<RequiredActionProviderRepresentation> expected, List<RequiredActionProviderRepresentation> actual) {
        Assertions.assertNotNull(actual, "Actual null");
        Assertions.assertEquals(expected.size(), actual.size(), "Required actions count");

        Iterator<RequiredActionProviderRepresentation> ite = expected.iterator();
        Iterator<RequiredActionProviderRepresentation> ita = actual.iterator();
        while (ite.hasNext()) {
            compareRequiredAction(ite.next(), ita.next());
        }
    }

    private void compareRequiredAction(RequiredActionProviderRepresentation expected, RequiredActionProviderRepresentation actual) {
        Assertions.assertEquals(expected.getAlias(), actual.getAlias(), "alias - " + expected.getAlias());
        Assertions.assertEquals(expected.getName(), actual.getName(), "name - "  + expected.getAlias());
        Assertions.assertEquals(expected.isEnabled(), actual.isEnabled(), "enabled - "  + expected.getAlias());
        Assertions.assertEquals(expected.isDefaultAction(), actual.isDefaultAction(), "defaultAction - "  + expected.getAlias());
        Assertions.assertEquals(expected.getConfig() != null ? expected.getConfig() : Collections.<String, String>emptyMap(), actual.getConfig(), "config - " + expected.getAlias());
    }

    private void addRequiredAction(List<RequiredActionProviderRepresentation> target, String alias, String name, boolean enabled, boolean defaultAction, Map<String, String> conf) {
        target.add(newRequiredAction(alias, name, enabled, defaultAction, conf));
    }

    private RequiredActionProviderRepresentation newRequiredAction(String alias, String name, boolean enabled, boolean defaultAction, Map<String, String> conf) {
        RequiredActionProviderRepresentation action = new RequiredActionProviderRepresentation();
        action.setAlias(alias);
        action.setName(name);
        action.setEnabled(enabled);
        action.setDefaultAction(defaultAction);
        action.setConfig(conf);
        return action;
    }

    private static class RequiredActionProviderComparator implements Comparator<RequiredActionProviderRepresentation> {
        @Override
        public int compare(RequiredActionProviderRepresentation o1, RequiredActionProviderRepresentation o2) {
            return o1.getAlias().compareTo(o2.getAlias());
        }
    }

    public static class CustomProvidersServerConfig implements KeycloakServerConfig {

        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            return config.dependency("org.keycloak.tests", "keycloak-tests-custom-providers");
        }

    }

}
