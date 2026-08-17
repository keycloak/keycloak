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

package org.keycloak.tests.actions;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.AuthenticationManagementResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.models.credential.PasswordCredentialModel;
import org.keycloak.representations.IDToken;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.EventRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RequiredActionProviderRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testframework.annotations.InjectAdminClient;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.testframework.remote.timeoffset.InjectTimeOffSet;
import org.keycloak.testframework.remote.timeoffset.TimeOffSet;
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.annotations.InjectWebDriver;
import org.keycloak.testframework.ui.page.LoginPasswordUpdatePage;
import org.keycloak.testframework.ui.webdriver.ManagedWebDriver;
import org.keycloak.tests.utils.PasswordGenerateUtil;
import org.keycloak.tests.utils.admin.AdminApiUtil;
import org.keycloak.testsuite.util.DroneUtils;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;

public abstract class AbstractActionsTest {

    protected static final String TEST_REALM_NAME = "test";
    protected static final String clientId = "test-app";

    protected ManagedRealm managedRealm;

    @InjectRealm(ref = "master", attachTo = "master")
    protected ManagedRealm masterRealm;

    @InjectAdminClient
    protected Keycloak adminClient;

    @InjectWebDriver
    protected ManagedWebDriver driver;

    @InjectOAuthClient
    protected OAuthClient oauth;

    @InjectRunOnServer
    protected RunOnServerClient runOnServer;

    @InjectRunOnServer(ref = "master", realmRef = "master")
    protected RunOnServerClient runOnServerMaster;

    @InjectTimeOffSet(enableForCaches = true)
    protected TimeOffSet timeOffSet;

    @InjectPage
    protected LoginPasswordUpdatePage passwordUpdatePage;

    private final Map<String, String> userPasswords = new HashMap<>();

    @BeforeEach
    public void setCurrentDriver() {
        if (managedRealm == null) {
            syncManagedRealmFromSubclass();
        }
        if (driver != null) {
            driver.manage().deleteAllCookies();
        }
        DroneUtils.setCurrentDriver(driver != null ? driver.driver() : null);
    }

    private void syncManagedRealmFromSubclass() {
        Class<?> current = getClass();
        while (current != null && current != AbstractActionsTest.class) {
            try {
                Field f = current.getDeclaredField("managedRealm");
                if (ManagedRealm.class.isAssignableFrom(f.getType())) {
                    f.setAccessible(true);
                    Object value = f.get(this);
                    if (value instanceof ManagedRealm realm) {
                        managedRealm = realm;
                    }
                }
                return;
            } catch (NoSuchFieldException e) {
                current = current.getSuperclass();
            } catch (IllegalAccessException ignored) {
                return;
            }
        }
    }

    protected UserRepresentation findUser(String userNameOrEmail) {
        List<UserRepresentation> users = managedRealm.admin().users().search(userNameOrEmail, -1, -1);
        if (users.size() != 1) {
            throw new IllegalStateException("Expected a single user for '" + userNameOrEmail + "', found " + users.size());
        }
        return users.get(0);
    }

    protected void updateUser(UserRepresentation user) {
        managedRealm.admin().users().get(user.getId()).update(user);
    }

    protected AccessTokenResponse sendTokenRequestAndGetResponse(EventRepresentation loginEvent) {
        String code = oauth.parseLoginResponse().getCode();
        AccessTokenResponse response = oauth.doAccessTokenRequest(code);
        Assertions.assertEquals(200, response.getStatusCode());
        return response;
    }

    protected IDToken sendTokenRequestAndGetIDToken(EventRepresentation loginEvent) {
        AccessTokenResponse response = sendTokenRequestAndGetResponse(loginEvent);
        return oauth.verifyIDToken(response.getIdToken());
    }

    protected String changePassword(String username) {
        UserResource userResource = AdminApiUtil.findUserByUsernameId(managedRealm.admin(), username);
        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setType(PasswordCredentialModel.TYPE);
        credential.setTemporary(Boolean.FALSE);
        credential.setValue(generatePassword());
        userResource.resetPassword(credential);
        userPasswords.put(username, credential.getValue());
        return credential.getValue();
    }

    protected void changePasswords(String... usernames) {
        if (usernames == null) {
            return;
        }
        for (String username : usernames) {
            changePassword(username);
        }
    }

    protected void generatePasswords(String... usernames) {
        if (usernames == null) {
            return;
        }
        for (String username : usernames) {
            generatePassword(username);
        }
    }

    protected String generatePassword(String username) {
        String password = generatePassword();
        userPasswords.put(username, password);
        return password;
    }

    protected String getPassword(String username) {
        String password = userPasswords.get(username);
        if (password == null) {
            password = "password";
            userPasswords.put(username, password);
        }
        return password;
    }

    protected String generatePassword() {
        return PasswordGenerateUtil.generatePassword();
    }

    protected ActionCleanup getCleanup() {
        return new ActionCleanup();
    }

    protected boolean isImportAfterEachMethod() {
        return false;
    }

    protected boolean removeVerifyProfileAtImport() {
        return true;
    }

    public void configureTestRealm(RealmRepresentation testRealm) {
    }

    protected void setRequiredActionEnabled(String realm, String requiredAction, boolean enabled, boolean defaultAction) {
        AuthenticationManagementResource flows = adminClient.realm(realm).flows();
        RequiredActionProviderRepresentation action = flows.getRequiredAction(requiredAction);
        action.setEnabled(enabled);
        action.setDefaultAction(defaultAction);
        flows.updateRequiredAction(requiredAction, action);
    }

    protected void setRequiredActionEnabled(String realm, String userId, String requiredAction, boolean enabled) {
        UserResource userResource = adminClient.realm(realm).users().get(userId);
        UserRepresentation userRepresentation = userResource.toRepresentation();
        List<String> requiredActions = userRepresentation.getRequiredActions();

        if (enabled && !requiredActions.contains(requiredAction)) {
            requiredActions.add(requiredAction);
        } else if (!enabled && requiredActions.contains(requiredAction)) {
            requiredActions.remove(requiredAction);
        }

        userResource.update(userRepresentation);
    }

    protected class ActionCleanup {

        public void addCleanup(Runnable cleanupTask) {
            managedRealm.cleanup().add(realm -> cleanupTask.run());
        }

        public void addUserId(String userId) {
            managedRealm.cleanup().add(realm -> realm.users().get(userId).remove());
        }
    }

}
