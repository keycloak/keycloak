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
 */

package org.keycloak.testsuite.broker;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import jakarta.ws.rs.core.Response;
import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.federation.kerberos.CommonKerberosConfig;
import org.keycloak.models.LDAPConstants;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.storage.UserStorageProvider.EditMode;
import org.keycloak.storage.UserStorageProviderModel;
import org.keycloak.storage.ldap.LDAPStorageProviderFactory;
import org.keycloak.storage.ldap.kerberos.LDAPProviderKerberosConfig;
import org.keycloak.testsuite.KerberosEmbeddedServer;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.federation.kerberos.KeycloakSPNegoSchemeFactory;
import org.keycloak.testsuite.util.KerberosRule;

public final class KcOidcBrokerLdapTest extends AbstractInitializedBaseBrokerTest {

    private static final String PROVIDER_CONFIG_LOCATION = "classpath:kerberos/kerberos-ldap-connection.properties";

    private KeycloakSPNegoSchemeFactory spnegoSchemeFactory;

    @Override
    protected BrokerConfiguration getBrokerConfiguration() {
        return KcOidcBrokerConfiguration.INSTANCE;
    }

    @ClassRule
    public static KerberosRule kerberosRule = new KerberosRule(PROVIDER_CONFIG_LOCATION, KerberosEmbeddedServer.DEFAULT_KERBEROS_REALM);

    @Before
    public void onBefore() {
        getKerberosRule().setKrb5ConfPath(testingClient.testing());
        spnegoSchemeFactory = new KeycloakSPNegoSchemeFactory(getKerberosConfig());
        oauth.clientId("kerberos-app");
        ComponentRepresentation rep = getUserStorageConfiguration();
        Response resp = adminClient.realm(bc.consumerRealmName()).components().add(rep);
        getCleanup().addComponentId(ApiUtil.getCreatedId(resp));
        resp.close();
    }

    @Test
    public void testUpdateProfileOnFirstLogin() {
        driver.manage().timeouts().pageLoadTimeout(1, TimeUnit.DAYS);
        updateExecutions(AbstractBrokerTest::enableUpdateProfileOnFirstLogin);
        oauth.clientId("broker-app");
        loginPage.open(bc.consumerRealmName());
        logInWithBroker(bc);
        updateAccountInformationPage.updateAccountInformation(bc.getUserLogin(), bc.getUserEmail(), "f", "l");
        Assert.assertFalse(errorPage.isCurrent());
    }

    private ComponentRepresentation getUserStorageConfiguration(String providerName, String providerId) {
        Map<String,String> kerberosConfig = getKerberosRule().getConfig();
        kerberosConfig.put(LDAPConstants.SYNC_REGISTRATIONS, "false");
        kerberosConfig.put(LDAPConstants.EDIT_MODE, EditMode.UNSYNCED.name());
        kerberosConfig.put(UserStorageProviderModel.IMPORT_ENABLED, "true");
        MultivaluedHashMap<String, String> config = toComponentConfig(kerberosConfig);

        UserStorageProviderModel model = new UserStorageProviderModel();
        model.setLastSync(0);
        model.setChangedSyncPeriod(-1);
        model.setFullSyncPeriod(-1);
        model.setName(providerName);
        model.setPriority(0);
        model.setProviderId(providerId);
        model.setConfig(config);

        return ModelToRepresentation.toRepresentationWithoutConfig(model);
    }

    private static MultivaluedHashMap<String, String> toComponentConfig(Map<String, String> ldapConfig) {
        MultivaluedHashMap<String, String> config = new MultivaluedHashMap<>();
        for (Map.Entry<String, String> entry : ldapConfig.entrySet()) {
            config.add(entry.getKey(), entry.getValue());

        }
        return config;
    }

    private KerberosRule getKerberosRule() {
        return kerberosRule;
    }


    private CommonKerberosConfig getKerberosConfig() {
        return new LDAPProviderKerberosConfig(getUserStorageConfiguration());
    }

    private ComponentRepresentation getUserStorageConfiguration() {
        return getUserStorageConfiguration("kerberos-ldap", LDAPStorageProviderFactory.PROVIDER_NAME);
    }
}
