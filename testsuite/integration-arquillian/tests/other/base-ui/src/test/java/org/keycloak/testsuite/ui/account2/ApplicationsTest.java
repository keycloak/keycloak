/*
 * Copyright 2019 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.testsuite.ui.account2;

import org.jboss.arquillian.graphene.page.Page;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.ui.account2.page.AbstractLoggedInPage;
import org.keycloak.testsuite.ui.account2.page.ApplicationsPage;
import org.keycloak.testsuite.util.ClientBuilder;
import org.keycloak.testsuite.util.OAuthClient;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.keycloak.testsuite.util.OAuthClient.APP_ROOT;
import static org.hamcrest.Matchers.containsInAnyOrder;

/**
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public class ApplicationsTest extends BaseAccountPageTest {
    @Page
    private ApplicationsPage applicationsPage;

    @Override
    protected AbstractLoggedInPage getAccountPage() {
        return applicationsPage;
    }

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        super.addTestRealms(testRealms);
        RealmRepresentation realm = testRealms.get(0);

        realm.setClients(Arrays.asList(
                ClientBuilder
                        .create()
                        .clientId("always-display-client")
                        .id(KeycloakModelUtils.generateId())
                        .name("Always Display Client")
                        .baseUrl(APP_ROOT + "/always-display-client")
                        .directAccessGrants()
                        .secret("secret1")
                        .alwaysDisplayInConsole(true)
                        .build(),
                ClientBuilder
                        .create()
                        .clientId("third-party-client")
                        .id(KeycloakModelUtils.generateId())
                        .name("Third Party Client")
                        .baseUrl(APP_ROOT + "/third-party-client")
                        .directAccessGrants()
                        .secret("secret1")
                        .consentRequired(true)
                        .build()
        ));
    }

    @Test
    public void applicationListTest() throws Exception {
        List<ApplicationsPage.ClientRepresentation> applications = applicationsPage.getApplications();
        assertFalse(applications.isEmpty());
        Map<String, ApplicationsPage.ClientRepresentation> apps = applications.stream().collect(Collectors.toMap(x -> x.getClientId(), x -> x));
        assertThat(apps.keySet(), containsInAnyOrder("always-display-client", "account-console"));
        assertClientRep(apps.get("account-console"), "Account Console", false, true, getAuthServerRoot() + "realms/test/account/", false);
        assertClientRep(apps.get("always-display-client"), "Always Display Client", false, false, getAuthServerRoot() + "realms/master/app/always-display-client", false);
    }

    @Test
    public void toggleApplicationDetailsTest() throws Exception {
        applicationsPage.toggleApplicationDetails("account-console");
        List<ApplicationsPage.ClientRepresentation> applications = applicationsPage.getApplications();
        assertFalse(applications.isEmpty());
        Map<String, ApplicationsPage.ClientRepresentation> apps = applications.stream().collect(Collectors.toMap(x -> x.getClientId(), x -> x));
        assertThat(apps.keySet(), containsInAnyOrder("always-display-client", "account-console"));
        assertClientRep(apps.get("account-console"), "Account Console", false, true, getAuthServerRoot() + "realms/test/account/", true);
        assertClientRep(apps.get("always-display-client"), "Always Display Client", false, false, getAuthServerRoot() + "realms/master/app/always-display-client", false);

        applicationsPage.toggleApplicationDetails("account-console");
        applications = applicationsPage.getApplications();
        assertFalse(applications.isEmpty());
        apps = applications.stream().collect(Collectors.toMap(x -> x.getClientId(), x -> x));
        assertThat(apps.keySet(), containsInAnyOrder("always-display-client", "account-console"));
        assertClientRep(apps.get("account-console"), "Account Console", false, true, getAuthServerRoot() + "realms/test/account/", false);
        assertClientRep(apps.get("always-display-client"), "Always Display Client", false, false, getAuthServerRoot() + "realms/master/app/always-display-client", false);
    }

    private void assertClientRep(ApplicationsPage.ClientRepresentation clientRep, String name, boolean userConsentRequired, boolean inUse, String effectiveUrl, boolean applicationDetailsVisible) {
        assertNotNull(clientRep);
        assertEquals(name, clientRep.getClientName());
        assertEquals(userConsentRequired, clientRep.isUserConsentRequired());
        assertEquals(inUse, clientRep.isInUse());
        assertEquals(applicationDetailsVisible, clientRep.isApplicationDetailsVisible());
        if (applicationDetailsVisible) assertEquals(effectiveUrl, clientRep.getEffectiveUrl());
    }

}
