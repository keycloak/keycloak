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
import java.net.URI;

import jakarta.mail.internet.MimeMessage;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.resource.OrganizationResource;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.common.util.UriUtils;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.mail.MailServer;
import org.keycloak.testframework.mail.annotations.InjectMailServer;
import org.keycloak.tests.utils.MailUtils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@KeycloakIntegrationTest
public class OrganizationInvitationLinkTest extends AbstractOrganizationTest {

    @InjectMailServer
    MailServer mailServer;

    @Test
    public void testRegistrationLinkForNewUserCarriesState() throws IOException {
        OrganizationResource organization = realm.admin().organizations().get(createOrganization().getId());

        try (Response response = organization.members().inviteUser("inviteduser@" + organizationName + ".org", "Homer", "Simpson")) {
            assertEquals(Status.NO_CONTENT.getStatusCode(), response.getStatus());
        }

        MimeMessage message = mailServer.getLastReceivedMessage();
        assertNotNull(message);

        URI link = URI.create(MailUtils.getLink(MailUtils.getBody(message).getHtml()).trim());
        MultivaluedHashMap<String, String> params = UriUtils.decodeQueryString(link.getRawQuery());

        // the authorization request started by the invitation link must carry a state so that the response is
        // callback-shaped. Clients key off the presence of code and state to recognize a callback URL and strip
        // the response parameters from it. Without it, those parameters are left behind in the browser URL and
        // end up in the redirect URI of the next authorization request made from that page, which the server
        // rejects for carrying them
        assertNotNull(params.getFirst(OAuth2Constants.STATE), "Invitation link is missing the state parameter: " + link);
    }
}
