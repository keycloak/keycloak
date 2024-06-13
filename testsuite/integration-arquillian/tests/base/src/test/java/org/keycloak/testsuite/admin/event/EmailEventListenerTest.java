/*
 * Copyright 2023 Red Hat Inc. and/or its affiliates and other contributors
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

package org.keycloak.testsuite.admin.event;

import static org.junit.Assert.assertNotNull;

import java.util.List;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.events.email.EmailEventListenerProviderFactory;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.util.GreenMailRule;
import org.keycloak.testsuite.util.UserBuilder;

public class EmailEventListenerTest extends AbstractEventTest {

    @Rule
    public GreenMailRule greenMail = new GreenMailRule();

    @Before
    public void init() {
        RealmRepresentation realm = testRealmResource().toRepresentation();

        realm.setSmtpServer(suiteContext.getSmtpServer());

        testRealmResource().update(realm);

        configRep.setEventsEnabled(true);
        configRep.setEventsListeners(List.of(EmailEventListenerProviderFactory.ID));
        saveConfig();
        RealmResource realmResource = testRealmResource();

        realmResource.users().create(UserBuilder.create()
                .username("alice")
                .email("alice@keycloak.org")
                .emailVerified(true)
                .password("alice").build());

        createAppClientInRealm("test");
        realmResource.clearEvents();
    }

    @Test
    public void eventAttributesTest() {
        driver.navigate().to(oauth.getLoginFormUrl());
        loginPage.form().login("alice", "invalid");
        loginPage.assertCurrent();
        assertNotNull(greenMail.getLastReceivedMessage());
    }
}
