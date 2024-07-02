/*
 * Copyright 2022 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.testsuite.federation.ldap;

import org.junit.Rule;
import org.junit.Test;
import org.keycloak.models.RealmModel;
import org.keycloak.storage.ldap.idm.model.LDAPObject;
import org.keycloak.testsuite.util.LDAPRule;
import org.keycloak.testsuite.util.LDAPTestConfiguration;
import org.keycloak.testsuite.util.LDAPTestUtils;
import org.keycloak.testsuite.util.LDAPRule.LDAPPasswordPolicy;

public class LDAPPasswordPolicyTest extends AbstractLDAPTest {

    @Rule
    // Start an embedded LDAP server with configuration derived from test annotations before each test.
    public LDAPRule ldapRule = new LDAPRule()
        .assumeTrue((LDAPTestConfiguration ldapConfig) -> {
            return ldapConfig.isStartEmbeddedLdapServer();
        });

    @Override
    protected LDAPRule getLDAPRule() {
        return ldapRule;
    }

    @Override
    protected void afterImportTestRealm() {
        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel appRealm = ctx.getRealm();

            LDAPObject user = LDAPTestUtils.addLDAPUser(ctx.getLdapProvider(), appRealm, "mustchange", "John", "Doe",
                    "john_old@email.org", null, "1234");
            LDAPTestUtils.updateLDAPPassword(ctx.getLdapProvider(), user, "Password1");
        });
    }

    @Test
    @LDAPPasswordPolicy(mustChange=true)
    public void testForcedPasswordChangeAfterReset() throws Exception {
        // Login with user that has to change password.
        loginPage.open();
        loginPage.login("mustchange", "Password1");

        // Forced password change sends user to update password page.
        passwordUpdatePage.assertCurrent();

        // Repeated login without changing password should still send user to update password page.
        loginPage.open();
        loginPage.login("mustchange", "Password1");
        passwordUpdatePage.assertCurrent();
    }

}
