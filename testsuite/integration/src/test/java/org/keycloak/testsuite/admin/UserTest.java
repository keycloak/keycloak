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

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.models.UserModel;
import org.keycloak.representations.idm.*;
import org.keycloak.testsuite.actions.RequiredActionEmailVerificationTest;
import org.keycloak.testsuite.forms.ResetPasswordTest;
import org.keycloak.testsuite.pages.*;
import org.keycloak.testsuite.rule.GreenMailRule;
import org.keycloak.testsuite.rule.WebResource;
import org.keycloak.testsuite.rule.WebRule;
import org.openqa.selenium.WebDriver;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import javax.ws.rs.ClientErrorException;
import javax.ws.rs.core.Response;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class UserTest extends AbstractClientTest {

    @Rule
    public WebRule webRule = new WebRule(this);

    @Rule
    public GreenMailRule greenMail = new GreenMailRule();

    @WebResource
    protected LoginPasswordUpdatePage passwordUpdatePage;

    @WebResource
    protected WebDriver driver;

    @WebResource
    protected InfoPage infoPage;

    @WebResource
    protected LoginPage loginPage;

    public String createUser() {
        return createUser("user1", "user1@localhost");
    }

    public String createUser(String username, String email) {
        UserRepresentation user = new UserRepresentation();
        user.setUsername(username);
        user.setEmail(email);
        user.setRequiredActions(Collections.<String>emptyList());
        user.setEnabled(true);

        Response response = realm.users().create(user);
        String createdId = ApiUtil.getCreatedId(response);
        response.close();
        return createdId;
    }

    /*--------------------------------------------------------------------------------
    THESE TWO TESTS BELOW STILL NEED TO BE MIGRATED TO SAME CLASS IN THE NEW TESTSUITE
     ---------------------------------------------------------------------------------*/
    @Test
    public void sendResetPasswordEmailSuccess() throws IOException, MessagingException {
        UserRepresentation userRep = new UserRepresentation();
        userRep.setEnabled(true);
        userRep.setUsername("user1");
        userRep.setEmail("user1@test.com");
        Response response = realm.users().create(userRep);
        String id = ApiUtil.getCreatedId(response);
        response.close();
        UserResource user = realm.users().get(id);
        List<String> actions = new LinkedList<>();
        actions.add(UserModel.RequiredAction.UPDATE_PASSWORD.name());
        user.executeActionsEmail("account", actions);

        Assert.assertEquals(1, greenMail.getReceivedMessages().length);

        MimeMessage message = greenMail.getReceivedMessages()[0];

        String link = ResetPasswordTest.getPasswordResetEmailLink(message);

        driver.navigate().to(link);

        assertTrue(passwordUpdatePage.isCurrent());

        passwordUpdatePage.changePassword("new-pass", "new-pass");

        assertEquals("Your account has been updated.", driver.getTitle());

        driver.navigate().to(link);

        assertEquals("We're sorry...", driver.getTitle());
    }


    @Test
    public void sendVerifyEmail() throws IOException, MessagingException {
        UserRepresentation userRep = new UserRepresentation();
        userRep.setUsername("user1");
        Response response = realm.users().create(userRep);
        String id = ApiUtil.getCreatedId(response);
        response.close();

        UserResource user = realm.users().get(id);

        try {
            user.sendVerifyEmail();
            fail("Expected failure");
        } catch (ClientErrorException e) {
            assertEquals(400, e.getResponse().getStatus());

            ErrorRepresentation error = e.getResponse().readEntity(ErrorRepresentation.class);
            Assert.assertEquals("User email missing", error.getErrorMessage());
        }
        try {
            userRep = user.toRepresentation();
            userRep.setEmail("user1@localhost");
            userRep.setEnabled(false);
            user.update(userRep);
            user.sendVerifyEmail();
            fail("Expected failure");
        } catch (ClientErrorException e) {
            assertEquals(400, e.getResponse().getStatus());

            ErrorRepresentation error = e.getResponse().readEntity(ErrorRepresentation.class);
            Assert.assertEquals("User is disabled", error.getErrorMessage());
        }
        try {
            userRep.setEnabled(true);
            user.update(userRep);
            user.sendVerifyEmail("invalidClientId");
            fail("Expected failure");
        } catch (ClientErrorException e) {
            assertEquals(400, e.getResponse().getStatus());

            ErrorRepresentation error = e.getResponse().readEntity(ErrorRepresentation.class);
            Assert.assertEquals("invalidClientId not enabled", error.getErrorMessage());
        }

        user.sendVerifyEmail();
        assertEquals(1, greenMail.getReceivedMessages().length);

        String link = RequiredActionEmailVerificationTest.getPasswordResetEmailLink(greenMail.getReceivedMessages()[0]);

        driver.navigate().to(link);

        Assert.assertEquals("Your account has been updated.", infoPage.getInfo());
    }
}
