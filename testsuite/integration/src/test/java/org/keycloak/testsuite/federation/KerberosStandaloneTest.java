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

package org.keycloak.testsuite.federation;

import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.junit.runners.MethodSorters;
import org.keycloak.common.constants.KerberosConstants;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.federation.kerberos.CommonKerberosConfig;
import org.keycloak.federation.kerberos.KerberosConfig;
import org.keycloak.federation.kerberos.KerberosFederationProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.storage.UserStorageProviderModel;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.federation.storage.ldap.LDAPTestUtils;
import org.keycloak.testsuite.rule.KerberosRule;
import org.keycloak.testsuite.rule.KeycloakRule;
import org.keycloak.testsuite.rule.WebRule;
import org.keycloak.utils.CredentialHelper;

import javax.ws.rs.core.Response;
import java.net.URL;
import java.util.Map;

/**
 * Test of KerberosFederationProvider (Kerberos not backed by LDAP)
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class KerberosStandaloneTest extends AbstractKerberosTest {

    private static final String PROVIDER_CONFIG_LOCATION = "kerberos/kerberos-standalone-connection.properties";

    private static UserStorageProviderModel kerberosModel;

    private static KerberosRule kerberosRule = new KerberosRule(PROVIDER_CONFIG_LOCATION);

    private static KeycloakRule keycloakRule = new KeycloakRule(new KeycloakRule.KeycloakSetup() {

        @Override
        public void config(RealmManager manager, RealmModel adminstrationRealm, RealmModel appRealm) {

            CredentialHelper.setAlternativeCredential(manager.getSession(), CredentialRepresentation.KERBEROS, appRealm);
            URL url = getClass().getResource("/kerberos-test/kerberos-app-keycloak.json");
            keycloakRule.createApplicationDeployment()
                    .name("kerberos-portal").contextPath("/kerberos-portal")
                    .servletClass(KerberosCredDelegServlet.class).adapterConfigPath(url.getPath())
                    .role("user").deployApplication();


            Map<String,String> kerberosConfig = kerberosRule.getConfig();
            MultivaluedHashMap<String, String> config = LDAPTestUtils.toComponentConfig(kerberosConfig);

            UserStorageProviderModel model = new UserStorageProviderModel();
            model.setLastSync(0);
            model.setChangedSyncPeriod(-1);
            model.setFullSyncPeriod(-1);
            model.setName("kerberos-standalone");
            model.setPriority(0);
            model.setProviderId(KerberosFederationProviderFactory.PROVIDER_NAME);
            model.setConfig(config);

            kerberosModel = new UserStorageProviderModel(appRealm.addComponentModel(model));
        }

    }) {

        @Override
        protected void importRealm() {
            server.importRealm(getClass().getResourceAsStream("/kerberos-test/kerberosrealm.json"));
        }

    };



    @ClassRule
    public static TestRule chain = RuleChain
            .outerRule(kerberosRule)
            .around(keycloakRule);

    @Rule
    public WebRule webRule = new WebRule(this);

    @Rule
    public AssertEvents events = new AssertEvents(keycloakRule);


    @Override
    protected CommonKerberosConfig getKerberosConfig() {
        return new KerberosConfig(kerberosModel);
    }

    @Override
    protected KeycloakRule getKeycloakRule() {
        return keycloakRule;
    }

    @Override
    protected AssertEvents getAssertEvents() {
        return events;
    }


    @Test
    public void spnegoLoginTest() throws Exception {
        spnegoLoginTestImpl();

        // Assert user was imported and hasn't any required action on him
        assertUser("hnelson", "hnelson@keycloak.org", null, null, false);
    }

    @Test
    @Override
    public void spnegoCaseInsensitiveTest() throws Exception {
        super.spnegoCaseInsensitiveTest();
    }

    @Test
    @Override
    public void credentialDelegationTest() throws Exception {
        super.credentialDelegationTest();
    }

    @Test
    @Override
    public void usernamePasswordLoginTest() throws Exception {
        super.usernamePasswordLoginTest();
    }

    @Test
    public void updateProfileEnabledTest() throws Exception {
        // Switch updateProfileOnFirstLogin to on
        KeycloakSession session = keycloakRule.startSession();
        try {
            RealmModel realm = session.realms().getRealm("test");
            UserStorageProviderModel kerberosProviderModel = realm.getUserStorageProviders().get(0);
            kerberosProviderModel.getConfig().putSingle(KerberosConstants.UPDATE_PROFILE_FIRST_LOGIN, "true");
            realm.updateComponent(kerberosProviderModel);
        } finally {
            keycloakRule.stopSession(session, true);
        }

        // Assert update profile page is displayed
        Response spnegoResponse = spnegoLogin("hnelson", "secret");
        Assert.assertEquals(200, spnegoResponse.getStatus());
        String responseText = spnegoResponse.readEntity(String.class);
        Assert.assertTrue(responseText.contains("You need to update your user profile to activate your account."));
        Assert.assertTrue(responseText.contains("hnelson@keycloak.org"));
        spnegoResponse.close();

        // Assert user was imported and has required action on him
        assertUser("hnelson", "hnelson@keycloak.org", null, null, true);

        // Switch updateProfileOnFirstLogin to off
        session = keycloakRule.startSession();
        try {
            RealmModel realm = session.realms().getRealm("test");
            UserStorageProviderModel kerberosProviderModel = realm.getUserStorageProviders().get(0);
            kerberosProviderModel.getConfig().putSingle(KerberosConstants.UPDATE_PROFILE_FIRST_LOGIN, "false");
            realm.updateComponent(kerberosProviderModel);
        } finally {
            keycloakRule.stopSession(session, true);
        }
    }


}
