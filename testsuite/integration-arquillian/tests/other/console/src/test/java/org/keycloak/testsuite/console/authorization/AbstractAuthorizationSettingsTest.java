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
package org.keycloak.testsuite.console.authorization;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.keycloak.common.Profile.Feature.AUTHORIZATION;
import static org.keycloak.testsuite.auth.page.login.Login.OIDC;

import org.jboss.arquillian.graphene.page.Page;
import org.junit.Before;
import org.junit.BeforeClass;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.testsuite.ProfileAssume;
import org.keycloak.testsuite.console.clients.AbstractClientTest;
import org.keycloak.testsuite.console.page.clients.authorization.Authorization;
import org.keycloak.testsuite.console.page.clients.settings.ClientSettings;
import org.keycloak.testsuite.console.page.clients.settings.ClientSettingsForm;
import org.openqa.selenium.By;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public abstract class AbstractAuthorizationSettingsTest extends AbstractClientTest {

    @Page
    protected ClientSettings clientSettingsPage;

    @Page
    protected Authorization authorizationPage;

    protected ClientRepresentation newClient;

    @BeforeClass
    public static void enabled() {
        ProfileAssume.assumeFeatureEnabled(AUTHORIZATION);
    }

    @Before
    public void configureTest() {
        this.newClient = createResourceServer();
    }

    private ClientRepresentation createResourceServer() {
        ClientRepresentation newClient = createClientRep("oidc-confidetial", OIDC);

        createClient(newClient);

        newClient.setRedirectUris(TEST_REDIRECT_URIs);
        newClient.setAuthorizationServicesEnabled(true);

        clientSettingsPage.form().setAccessType(ClientSettingsForm.OidcAccessType.CONFIDENTIAL);
        clientSettingsPage.form().setRedirectUris(TEST_REDIRECT_URIs);
        clientSettingsPage.form().setAuthorizationSettingsEnabled(true);
        clientSettingsPage.form().save();
        assertAlertSuccess();

        ClientRepresentation found = findClientByClientId(newClient.getClientId());
        assertNotNull("Client " + newClient.getClientId() + " was not found.", found);

        newClient.setPublicClient(false);
        newClient.setServiceAccountsEnabled(true);

        assertClientSettingsEqual(newClient, found);
        assertTrue(clientSettingsPage.tabs().getTabs().findElement(By.linkText("Authorization")).isDisplayed());

        clientSettingsPage.setId(found.getId());
        clientSettingsPage.navigateTo();
        authorizationPage.setId(found.getId());

        clientSettingsPage.tabs().authorization();
        assertTrue(authorizationPage.isCurrent());

        newClient.setId(found.getId());

        return newClient;
    }
}
