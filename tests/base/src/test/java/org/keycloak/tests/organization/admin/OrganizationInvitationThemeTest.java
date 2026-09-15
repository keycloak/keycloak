/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.tests.organization.admin;

import java.io.IOException;

import jakarta.mail.internet.MimeMessage;
import jakarta.ws.rs.core.Response;

import org.keycloak.admin.client.resource.OrganizationResource;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.mail.MailServer;
import org.keycloak.testframework.mail.annotations.InjectMailServer;
import org.keycloak.testframework.realm.UserBuilder;
import org.keycloak.testframework.ui.annotations.InjectWebDriver;
import org.keycloak.testframework.ui.webdriver.ManagedWebDriver;
import org.keycloak.testframework.util.ApiUtil;
import org.keycloak.tests.common.CustomProvidersServerConfig;
import org.keycloak.tests.utils.MailUtils;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@KeycloakIntegrationTest(config = CustomProvidersServerConfig.class)
public class OrganizationInvitationThemeTest extends AbstractOrganizationTest {

    @InjectMailServer
    MailServer mailServer;

    @InjectWebDriver
    ManagedWebDriver driver;

    @Test
    public void testOrganizationAvailableWhenConfirmingMembership() throws IOException {
        realm.updateWithCleanup(r -> r.loginTheme("organization"));
        OrganizationResource organization = realm.admin().organizations().get(createOrganization().getId());
        String userId = createUser("invited@myemail.com");

        organization.members().inviteExistingUser(userId).close();
        driver.open(getInvitationLink());

        // the invitee is not a member yet, but the page still knows the organization they were invited to
        String page = driver.driver().getPageSource();
        assertThat(page, containsString("Sign-in to " + organizationName + " organization"));
        assertThat(page, containsString("The key is value1, value2"));
        assertThat(page, not(containsString("User is member of")));
    }

    private String createUser(String email) {
        UserRepresentation user = UserBuilder.create()
                .username(email)
                .email(email)
                .name("Invited", "User")
                .emailVerified(true)
                .build();
        String userId;

        try (Response response = realm.admin().users().create(user)) {
            userId = ApiUtil.getCreatedId(response);
        }

        realm.cleanup().add(r -> r.users().get(userId).remove());
        return userId;
    }

    private String getInvitationLink() throws IOException {
        MimeMessage message = mailServer.getLastReceivedMessage();
        assertNotNull(message);
        return MailUtils.getLink(MailUtils.getBody(message).getHtml()).trim();
    }
}
