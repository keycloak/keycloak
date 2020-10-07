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

package org.keycloak.testsuite.admin;

import org.jboss.arquillian.graphene.page.Page;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.events.Details;
import org.keycloak.events.EventType;
import org.keycloak.events.admin.OperationType;
import org.keycloak.models.credential.OTPCredentialModel;
import org.keycloak.models.utils.TimeBasedOTP;
import org.keycloak.representations.idm.AdminEventRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.AbstractTestRealmKeycloakTest;
import org.keycloak.testsuite.pages.AccountTotpPage;
import org.keycloak.testsuite.pages.AccountUpdateProfilePage;
import org.keycloak.testsuite.pages.LoginPage;

import java.util.List;


/**
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 */
public class UserTotpTest extends AbstractTestRealmKeycloakTest {

    @Rule
    public AssertEvents events = new AssertEvents(this);

    @Page
    protected AccountTotpPage totpPage;

    @Page
    protected AccountUpdateProfilePage profilePage;

    @Page
    protected LoginPage loginPage;

    private TimeBasedOTP totp = new TimeBasedOTP();


    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {
    }

    @Test
    public void setupTotp() {
        totpPage.open();
        loginPage.login("test-user@localhost", "password");

        events.expectLogin().client("account").detail(Details.REDIRECT_URI, getAccountRedirectUrl() + "?path=totp").assertEvent();

        Assert.assertTrue(totpPage.isCurrent());

        Assert.assertFalse(driver.getPageSource().contains("Remove Google"));

        totpPage.configure(totp.generateTOTP(totpPage.getTotpSecret()));

        Assert.assertEquals("Mobile authenticator configured.", profilePage.getSuccess());

        events.expectAccount(EventType.UPDATE_TOTP).assertEvent();

        Assert.assertTrue(driver.getPageSource().contains("pficon-delete"));

        List<UserRepresentation> users = adminClient.realms().realm("test").users().search("test-user@localhost", null, null, null, 0, 1);
        String userId = users.get(0).getId();
        testingClient.testing().clearAdminEventQueue();
        CredentialRepresentation totpCredential = adminClient.realms().realm("test").users().get(userId).credentials()
                .stream().filter(c -> OTPCredentialModel.TYPE.equals(c.getType())).findFirst().get();
        adminClient.realms().realm("test").users().get(userId).removeCredential(totpCredential.getId());

        totpPage.open();
        Assert.assertFalse(driver.getPageSource().contains("pficon-delete"));

        AdminEventRepresentation event = testingClient.testing().pollAdminEvent();
        Assert.assertNotNull(event);
        Assert.assertEquals(OperationType.ACTION.name(), event.getOperationType());
        Assert.assertEquals("users/" + userId + "/credentials/" + totpCredential.getId(), event.getResourcePath());
    }
}
