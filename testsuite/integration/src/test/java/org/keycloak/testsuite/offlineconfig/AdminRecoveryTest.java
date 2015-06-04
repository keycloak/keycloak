/*
 * Copyright 2015 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @author tags. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.keycloak.testsuite.offlineconfig;

import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserCredentialValueModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserModel.RequiredAction;
import org.keycloak.offlineconfig.AdminRecovery;
import org.keycloak.testsuite.rule.KeycloakRule;
import org.keycloak.testsuite.rule.WebRule;

/**
 * Test the AdminRecovery class.
 *
 * @author Stan Silvert ssilvert@redhat.com (C) 2015 Red Hat Inc.
 */
public class AdminRecoveryTest {
    @ClassRule
    public static KeycloakRule keycloakRule = new KeycloakRule();

    @Rule
    public WebRule webRule = new WebRule(this);

    @Test
    public void testAdminDeletedRecovery() {
        KeycloakSession session = keycloakRule.startSession();
        RealmModel masterRealm = session.realms().getRealmByName("master");
        UserModel adminUser = session.users().getUserByUsername("admin", masterRealm);
        session.users().removeUser(masterRealm, adminUser);
        adminUser = session.users().getUserByUsername("admin", masterRealm);
        keycloakRule.stopSession(session, true);

        Assert.assertNull(adminUser);

        doAdminRecovery(session);

        session = keycloakRule.startSession();
        adminUser = session.users().getUserByUsername("admin", masterRealm);
        Assert.assertNotNull(adminUser);
        Assert.assertTrue(adminUser.getRequiredActions().contains(RequiredAction.UPDATE_PASSWORD.toString()));
    }

    @Test
    public void testAdminPasswordRecovery() {
        KeycloakSession session = keycloakRule.startSession();
        RealmModel masterRealm = session.realms().getRealmByName("master");
        UserModel adminUser = session.users().getUserByUsername("admin", masterRealm);
        UserCredentialValueModel password = adminUser.getCredentialsDirectly().get(0);
        password.setValue("forgotten-password");
        adminUser.updateCredentialDirectly(password);
        keycloakRule.stopSession(session, true);

        Assert.assertEquals("forgotten-password", getAdminPassword());

        doAdminRecovery(session);

        Assert.assertNotEquals("forgotten-password", getAdminPassword());
    }

    private void doAdminRecovery(KeycloakSession session) {
        System.setProperty(AdminRecovery.RECOVER_ADMIN_ACCOUNT, "true");
        AdminRecovery.recover(session.getKeycloakSessionFactory());
    }

    private String getAdminPassword() {
        KeycloakSession session = keycloakRule.startSession();
        RealmModel masterRealm = session.realms().getRealmByName("master");
        UserModel adminUser = session.users().getUserByUsername("admin", masterRealm);
        UserCredentialValueModel password = adminUser.getCredentialsDirectly().get(0);
        keycloakRule.stopSession(session, true);
        return password.getValue();
    }
}
