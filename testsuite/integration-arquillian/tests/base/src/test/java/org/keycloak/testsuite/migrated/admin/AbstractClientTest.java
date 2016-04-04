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

package org.keycloak.testsuite.migrated.admin;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.After;
import org.junit.Before;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.ClientsResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.AbstractKeycloakTest;
import org.keycloak.util.JsonSerialization;

import static org.junit.Assert.assertArrayEquals;

/**
 * This class adapts the functionality from the old testsuite to make tests
 * easier to port.
 *
 * @author Stan Silvert ssilvert@redhat.com (C) 2016 Red Hat Inc.
 */
public abstract class AbstractClientTest extends AbstractKeycloakTest  {
    protected static final String REALM_NAME = "admin-client-test";

    protected Keycloak keycloak;
    protected RealmResource realm;
    protected OAuthClient oauth;

    @Before
    public void setUpKeycloakAlias() {
        keycloak = adminClient;
    }

    @Before
    public void setUpOAuthClient() {
        oauth = new OAuthClient(driver);
    }

    // old testsuite expects this realm to be removed at the end of the test
    // not sure if it really matters
    @After
    public void after() {
        for (RealmRepresentation r : keycloak.realms().findAll()) {
            if (r.getRealm().equals(REALM_NAME)) {
                removeRealm(r);
            }
        }
    }

    private ClientRepresentation findTestApp(List<ClientRepresentation> clients) {
        for (ClientRepresentation client : clients) {
            if (client.getClientId().equals("test-app")) return client;
        }

        return null;
    }

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        RealmRepresentation rep = loadJson(getClass().getResourceAsStream("/testrealm.json"), RealmRepresentation.class);


        /* Implement this old behavior by changing the representation before the realm gets loaded
        keycloakRule.configure(new KeycloakRule.KeycloakSetup() {
            @Override
            public void config(RealmManager manager, RealmModel adminstrationRealm, RealmModel appRealm) {
                appRealm.getClientByClientId("test-app").setDirectAccessGrantsEnabled(true);
            }
        });*/
        findTestApp(rep.getClients()).setDirectAccessGrantsEnabled(true);

        testRealms.add(rep);

        RealmRepresentation adminRealmRep = new RealmRepresentation();
        adminRealmRep.setRealm(REALM_NAME);
        adminRealmRep.setEnabled(true);
        Map<String, String> config = new HashMap<>();
        config.put("from", "auto@keycloak.org");
        config.put("host", "localhost");
        config.put("port", "3025");
        adminRealmRep.setSmtpServer(config);
        testRealms.add(adminRealmRep);
    }

    @Before
    public void setRealm() {
        realm = adminClient.realm(REALM_NAME);
    }

    // Taken from Keycloak class in old testsuite.
    // So, code in old testsuite calling this looks like Keycloak.loadJson(.....)
    public static <T> T loadJson(InputStream is, Class<T> type) {
        try {
            return JsonSerialization.readValue(is, type);
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse json", e);
        }
    }

    public static <T> void assertNames(List<T> actual, String... expected) {
        Arrays.sort(expected);
        String[] actualNames = names(actual);
        assertArrayEquals("Expected: " + Arrays.toString(expected) + ", was: " + Arrays.toString(actualNames), expected, actualNames);
    }

    public static <T> List<T> sort(List<T> list) {
        Collections.sort(list, new Comparator<Object>() {
            @Override
            public int compare(Object o1, Object o2) {
                return name(o1).compareTo(name(o2));
            }
        });
        return list;
    }

    public static <T> String[] names(List<T> list) {
        String[] names = new String[list.size()];
        for (int i = 0; i < list.size(); i++) {
            names[i] = name(list.get(i));
        }
        Arrays.sort(names);
        return names;
    }

    public static String name(Object o1) {
        if (o1 instanceof RealmRepresentation) {
            return ((RealmRepresentation) o1).getRealm();
        } else if (o1 instanceof ClientRepresentation) {
            return ((ClientRepresentation) o1).getClientId();
        } else if (o1 instanceof IdentityProviderRepresentation) {
            return ((IdentityProviderRepresentation) o1).getAlias();
        }
        throw new IllegalArgumentException();
    }

}
