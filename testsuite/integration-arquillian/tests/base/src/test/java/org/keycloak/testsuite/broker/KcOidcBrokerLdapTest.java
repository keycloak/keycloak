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

import jakarta.ws.rs.core.Response;

import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.models.LDAPConstants;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.storage.UserStorageProvider.EditMode;
import org.keycloak.storage.UserStorageProviderModel;
import org.keycloak.storage.ldap.LDAPStorageProviderFactory;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.util.LDAPRule;

import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

public final class KcOidcBrokerLdapTest extends AbstractInitializedBaseBrokerTest {

    @Override
    protected BrokerConfiguration getBrokerConfiguration() {
        return KcOidcBrokerConfiguration.INSTANCE;
    }

    @ClassRule
    public static LDAPRule ldapRule = new LDAPRule();

    @Before
    public void onBefore() {
        ComponentRepresentation rep = getUserStorageConfiguration();
        Response resp = adminClient.realm(bc.consumerRealmName()).components().add(rep);
        getCleanup().addComponentId(ApiUtil.getCreatedId(resp));
        resp.close();
    }

    @Test
    public void testUpdateProfileOnFirstLogin() {
        updateExecutions(AbstractBrokerTest::enableUpdateProfileOnFirstLogin);
        oauth.clientId("broker-app");
        loginPage.open(bc.consumerRealmName());
        logInWithBroker(bc);
        updateAccountInformationPage.updateAccountInformation(bc.getUserLogin(), bc.getUserEmail(), "f", "l");
        Assert.assertFalse(errorPage.isCurrent());
    }

    private ComponentRepresentation getUserStorageConfiguration(String providerName, String providerId) {
        Map<String,String> ldapConfig = ldapRule.getConfig();
        ldapConfig.put(LDAPConstants.SYNC_REGISTRATIONS, "false");
        ldapConfig.put(LDAPConstants.EDIT_MODE, EditMode.UNSYNCED.name());
        ldapConfig.put(UserStorageProviderModel.IMPORT_ENABLED, "true");
        MultivaluedHashMap<String, String> config = toComponentConfig(ldapConfig);

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

    private ComponentRepresentation getUserStorageConfiguration() {
        return getUserStorageConfiguration("ldap", LDAPStorageProviderFactory.PROVIDER_NAME);
    }
}
