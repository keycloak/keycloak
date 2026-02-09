/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates
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

import java.util.List;

import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.jpa.RealmAdapter;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.UserStorageProviderModel;
import org.keycloak.testsuite.AbstractTestRealmKeycloakTest;
import org.keycloak.testsuite.pages.AppPage;
import org.keycloak.testsuite.pages.LoginPage;

import org.jboss.arquillian.container.test.api.ContainerController;
import org.jboss.arquillian.graphene.page.Page;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

/**
 * KEYCLOAK-3903 and KEYCLOAK-3620
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class BrokenUserStorageTest extends AbstractTestRealmKeycloakTest {

    @ArquillianResource
    protected ContainerController controller;

    @Page
    protected LoginPage loginPage;

    @Page
    protected AppPage appPage;

    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {
    }


    private void loginSuccessAndLogout(String username, String password) {
        loginPage.open();
        loginPage.login(username, password);
        Assert.assertEquals(AppPage.RequestType.AUTH_RESPONSE, appPage.getRequestType());
        Assert.assertNotNull(oauth.parseLoginResponse().getCode());
        oauth.openLogoutForm();
    }

    @Test
    public void testBootWithBadProviderId() throws Exception {
        testingClient.server().run(session -> {
            // set this system property
            System.setProperty(RealmAdapter.COMPONENT_PROVIDER_EXISTS_DISABLED, "true");

            RealmModel realm = session.realms().getRealmByName("master");

            UserStorageProviderModel model = new UserStorageProviderModel();
            model.setName("bad-provider-id");
            model.setPriority(2);
            model.setParentId(realm.getId());
            model.setProviderId("error");
            ComponentModel component = realm.importComponentModel(model);
        });

        controller.stop(suiteContext.getAuthServerInfo().getQualifier());
        controller.start(suiteContext.getAuthServerInfo().getQualifier());
        reconnectAdminClient();

        loginSuccessAndLogout("test-user@localhost", "password");

        // make sure we can list components and delete provider as this is an admin console operation

        RealmResource master = adminClient.realms().realm("master");
        String masterId = master.toRepresentation().getId();
        List<ComponentRepresentation> components = master.components().query(masterId, UserStorageProvider.class.getName());

        ComponentRepresentation found = null;
        for (ComponentRepresentation rep : components) {
            if (rep.getName().equals("bad-provider-id")) {
                found = rep;
            }
        }
        Assert.assertNotNull(found);

        master.components().component(found.getId()).remove();

        List<ComponentRepresentation> components2 = master.components().query(masterId, UserStorageProvider.class.getName());
        Assert.assertEquals(components.size() - 1, components2.size());

    }

    @After
    public void resetSystemProperty() {
        testingClient.server().run(session -> {
            System.getProperties().remove(RealmAdapter.COMPONENT_PROVIDER_EXISTS_DISABLED);
        });
    }

 }
