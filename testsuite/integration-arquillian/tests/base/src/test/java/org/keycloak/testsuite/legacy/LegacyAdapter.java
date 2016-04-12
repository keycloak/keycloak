/*
 * Copyright 2016 Red Hat Inc. and/or its affiliates and other contributors
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

package org.keycloak.testsuite.legacy;

import java.util.List;
import org.junit.Before;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.AbstractKeycloakTest;

import static org.keycloak.testsuite.legacy.admin.LegacyAbstractClientTest.loadJson;

/**
 * Allows migration of legacy tests with a minimum of changes to the tests themselves.
 *
 * @author Stan Silvert ssilvert@redhat.com (C) 2016 Red Hat Inc.
 */
public class LegacyAdapter extends AbstractKeycloakTest {

    protected Keycloak keycloak;
    protected LegacyOAuthClient oauth;
    private String testRealmPublicKey;

    private ClientRepresentation findTestApp(List<ClientRepresentation> clients) {
        for (ClientRepresentation client : clients) {
            if (client.getClientId().equals("test-app")) return client;
        }

        return null;
    }

    public static RealmRepresentation findRealm(String realmName, List<RealmRepresentation> testRealms) {
        for (RealmRepresentation realm : testRealms) {
            if (realm.getRealm().equals(realmName)) return realm;
        }

        return null;
    }

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        RealmRepresentation rep = loadJson(getClass().getResourceAsStream("/testrealm.json"), RealmRepresentation.class);
        this.testRealmPublicKey = rep.getPublicKey();

        /* Implement this old behavior by changing the representation before the realm gets loaded
        keycloakRule.configure(new KeycloakRule.KeycloakSetup() {
            @Override
            public void config(RealmManager manager, RealmModel adminstrationRealm, RealmModel appRealm) {
                appRealm.getClientByClientId("test-app").setDirectAccessGrantsEnabled(true);
            }
        });*/
        findTestApp(rep.getClients()).setDirectAccessGrantsEnabled(true);

        testRealms.add(rep);
    }

    @Before
    public void setUpKeycloakAlias() {
        keycloak = adminClient;
    }

    @Before
    public void setUpOAuthClient() {
        oauth = new LegacyOAuthClient(driver, testRealmPublicKey);
    }

}
