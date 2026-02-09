/*
 * Copyright 2023 Red Hat, Inc. and/or its affiliates
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

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.stream.Collectors;

import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.RealmModel;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.testsuite.federation.UserPropertyFileStorageFactory;
import org.keycloak.testsuite.util.LDAPRule;
import org.keycloak.testsuite.util.LDAPTestUtils;

import org.apache.commons.io.FileUtils;
import org.junit.ClassRule;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;

/**
 * It tests correct behavior when using {@code firstResult} during querying users 
 * with a provider not-implementing {@code UserCountMethodsProvider} - LDAPStorageProvider.
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class LDAPCountQueryTest extends AbstractLDAPTest {

    private static final File CONFIG_DIR = new File(System.getProperty("auth.server.config.dir", ""));

    @ClassRule
    public static LDAPRule ldapRule = new LDAPRule();

    @Override
    protected LDAPRule getLDAPRule() {
        return ldapRule;
    }

    @Override
    protected void afterImportTestRealm() {
        copyPropertiesFiles();
        final String ldapComponentId = ldapModelId;
        final String propertyFile = CONFIG_DIR.getAbsolutePath() + File.separator + "user-password.properties";
        testingClient.server().run(session -> {

            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel appRealm = ctx.getRealm();

            // set high priority to ensure the provider which doesn't implement UserCountMethodsProvider is queried first
            ComponentModel ldapComponent = appRealm.getComponent(ldapComponentId);
            ldapComponent.getConfig().putSingle("priority", Integer.toString(0));
            appRealm.updateComponent(ldapComponent);

            // Delete all local users and add some new for testing
            session.users().searchForUserStream(appRealm, Map.of()).collect(Collectors.toList()).forEach(u -> session.users().removeUser(appRealm, u));

            LDAPTestUtils.addLocalUser(session, appRealm, "user1", "user1@email", "password");
            LDAPTestUtils.addLocalUser(session, appRealm, "user2", "user2@email", "password");

            // Delete all LDAP users and add some new for testing
            LDAPTestUtils.removeAllLDAPUsers(ctx.getLdapProvider(), appRealm);

            LDAPTestUtils.addLDAPUser(ctx.getLdapProvider(), appRealm, "john00", "john", "Doe", "john0@email.org", null, "1234");
            LDAPTestUtils.addLDAPUser(ctx.getLdapProvider(), appRealm, "john01", "john", "Doe", "john1@email.org", null, "1234");
            LDAPTestUtils.addLDAPUser(ctx.getLdapProvider(), appRealm, "john02", "john", "Doe", "john2@email.org", null, "1234");

            // add user-prop provider with lower priority
            ComponentModel userPropProvider = new ComponentModel();
            userPropProvider.setName("user-props");
            userPropProvider.setProviderId(UserPropertyFileStorageFactory.PROVIDER_ID);
            userPropProvider.setProviderType(UserStorageProvider.class.getName());
            userPropProvider.setConfig(new MultivaluedHashMap<>());
            userPropProvider.getConfig().putSingle("priority", Integer.toString(1));
            userPropProvider.getConfig().putSingle("propertyFile", propertyFile);
            userPropProvider.getConfig().putSingle("federatedStorage", "false");
            appRealm.addComponentModel(userPropProvider);
        });
    }

    private void copyPropertiesFiles() throws RuntimeException {
        try {
            // copy files used by the following user-props user provider
            File stResDir = new File(getClass().getResource("/storage-test").toURI());
            if (stResDir.exists() && stResDir.isDirectory() && CONFIG_DIR.exists() && CONFIG_DIR.isDirectory()) {
                for (File f : stResDir.listFiles()) {
                    log.infof("Copying %s to %s", f.getName(), CONFIG_DIR.getAbsolutePath());
                    FileUtils.copyFileToDirectory(f, CONFIG_DIR);
                }
            } else {
                throw new RuntimeException("Property `auth.server.config.dir` must be set to run the test.");
            }
        } catch (IOException | RuntimeException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testFirstResultWithMultipleProviders() {
        assertThat(adminClient.realm(TEST_REALM_NAME).users().list(6, null), hasSize(4));
    }

    @Test
    public void testFirstResultWithMultipleProvidersMaxResultSet() {
        assertThat(adminClient.realm(TEST_REALM_NAME).users().list(6, 20), hasSize(4));
    }
}
