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
package org.keycloak.testsuite.federation.storage;

import org.junit.After;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.common.util.Time;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.Constants;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.cache.CachedUserModel;
import org.keycloak.models.cache.infinispan.UserAdapter;
import org.keycloak.models.jpa.RealmAdapter;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.storage.StorageId;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.UserStorageProviderModel;
import org.keycloak.testsuite.ApplicationServlet;
import org.keycloak.testsuite.OAuthClient;
import org.keycloak.testsuite.pages.AppPage;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.rule.KeycloakRule;
import org.keycloak.testsuite.rule.WebResource;
import org.keycloak.testsuite.rule.WebRule;
import org.openqa.selenium.WebDriver;

import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * KEYCLOAK-3903 and KEYCLOAK-3620
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class BrokenUserStorageTest {
    @ClassRule
    public static KeycloakRule keycloakRule = new KeycloakRule();

    @Rule
    public WebRule webRule = new WebRule(this);

    @WebResource
    protected OAuthClient oauth;

    @WebResource
    protected WebDriver driver;

    @WebResource
    protected AppPage appPage;

    @WebResource
    protected LoginPage loginPage;

    private void loginSuccessAndLogout(String username, String password) {
        loginPage.open();
        loginPage.login(username, password);
        Assert.assertEquals(AppPage.RequestType.AUTH_RESPONSE, appPage.getRequestType());
        Assert.assertNotNull(oauth.getCurrentQuery().get(OAuth2Constants.CODE));
        oauth.openLogout();
    }
    protected String AUTH_SERVER_URL = "http://localhost:8081/auth";

    @Test
    public void testBootWithBadProviderId() throws Exception {
        KeycloakSession session = keycloakRule.startSession();
        // set this system property
        System.setProperty(RealmAdapter.COMPONENT_PROVIDER_EXISTS_DISABLED, "true");
        RealmModel realm = session.realms().getRealmByName("master");
        String masterId = realm.getId();
        UserStorageProviderModel model;
        model = new UserStorageProviderModel();
        model.setName("bad-provider-id");
        model.setPriority(2);
        model.setParentId(realm.getId());
        model.setProviderId("error");
        ComponentModel component = realm.importComponentModel(model);

        keycloakRule.stopSession(session, true);

        keycloakRule.restartServer();
        keycloakRule.deployServlet("app", "/app", ApplicationServlet.class);

        loginSuccessAndLogout("test-user@localhost", "password");

        // make sure we can list components and delete provider as this is an admin console operation

        Keycloak keycloakAdmin = Keycloak.getInstance(AUTH_SERVER_URL, "master", "admin", "admin", Constants.ADMIN_CLI_CLIENT_ID);
        RealmResource master = keycloakAdmin.realms().realm("master");
        List<ComponentRepresentation> components = master.components().query(masterId, UserStorageProvider.class.getName());
        boolean found = false;
        for (ComponentRepresentation rep : components) {
            if (rep.getName().equals("bad-provider-id")) {
                found = true;
            }
        }
        Assert.assertTrue(found);

        master.components().component(component.getId()).remove();

        List<ComponentRepresentation> components2 = master.components().query(masterId, UserStorageProvider.class.getName());
        Assert.assertEquals(components.size() - 1, components2.size());
    }

    @After
    public void resetTimeoffset() {
        Time.setOffset(0);

    }

 }
