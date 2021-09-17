/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates
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
 *
 */

package org.keycloak.testsuite.account;

import java.io.IOException;

import javax.ws.rs.BadRequestException;

import org.jboss.logging.Logger;
import org.junit.Assert;
import org.junit.Test;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.broker.provider.util.SimpleHttp;
import org.keycloak.representations.account.UserRepresentation;
import org.keycloak.representations.idm.ErrorRepresentation;
import org.keycloak.services.messages.Messages;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude.AuthServer.REMOTE;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@AuthServerContainerExclude({REMOTE}) // TODO: Enable this for quarkus and hopefully for remote as well...
public class AccountRestServiceReadOnlyAttributesTest extends AbstractRestServiceTest {

    private static final Logger logger = Logger.getLogger(AccountRestServiceReadOnlyAttributesTest.class);

    @Test
    public void testUpdateProfileCannotUpdateReadOnlyAttributes() throws IOException {
        // Denied by default
        testAccountUpdateAttributeExpectFailure("usercertificate");
        testAccountUpdateAttributeExpectFailure("uSErCertificate");
        testAccountUpdateAttributeExpectFailure("KERBEROS_PRINCIPAL", true);

        // Should be allowed
        testAccountUpdateAttributeExpectSuccess("noKerberos_Principal");
        testAccountUpdateAttributeExpectSuccess("KERBEROS_PRINCIPALno");

        // Denied by default
        testAccountUpdateAttributeExpectFailure("enabled");
        testAccountUpdateAttributeExpectFailure("CREATED_TIMESTAMP", true);

        // Should be allowed
        testAccountUpdateAttributeExpectSuccess("saml.something");

        // Denied by configuration. "deniedFoot" is allowed as there is no wildcard
        testAccountUpdateAttributeExpectFailure("deniedfoo");
        testAccountUpdateAttributeExpectFailure("deniedFOo");
        testAccountUpdateAttributeExpectSuccess("deniedFoot");

        // Denied by configuration. There is wildcard at the end
        testAccountUpdateAttributeExpectFailure("deniedbar");
        testAccountUpdateAttributeExpectFailure("deniedBAr");
        testAccountUpdateAttributeExpectFailure("deniedBArr");
        testAccountUpdateAttributeExpectFailure("deniedbarrier");

        // Wildcard just at the end
        testAccountUpdateAttributeExpectSuccess("nodeniedbar");
        testAccountUpdateAttributeExpectSuccess("nodeniedBARrier");

        // Wildcard at the end
        testAccountUpdateAttributeExpectFailure("saml.persistent.name.id.for.foo");
        testAccountUpdateAttributeExpectFailure("saml.persistent.name.id.for._foo_");
        testAccountUpdateAttributeExpectSuccess("saml.persistent.name.idafor.foo");

        // Special characters inside should be quoted
        testAccountUpdateAttributeExpectFailure("deniedsome/thing");
        testAccountUpdateAttributeExpectFailure("deniedsome*thing");
        testAccountUpdateAttributeExpectSuccess("deniedsomeithing");

        // Denied only for admin, but allowed for normal user
        testAccountUpdateAttributeExpectSuccess("deniedSomeAdmin");
    }

    private void testAccountUpdateAttributeExpectFailure(String attrName) throws IOException {
        testAccountUpdateAttributeExpectFailure(attrName, false);
    }

    private void testAccountUpdateAttributeExpectFailure(String attrName, boolean deniedForAdminAsWell) throws IOException {
        // Attribute not yet supposed to be on the user
        UserRepresentation user = SimpleHttp.doGet(getAccountUrl(null), httpClient).auth(tokenUtil.getToken()).asJson(UserRepresentation.class);
        Assert.assertThat(user.getAttributes().keySet(), not(contains(attrName)));

        // Assert not possible to add the attribute to the user
        user.singleAttribute(attrName, "foo");
        updateError(user, 400, Messages.UPDATE_READ_ONLY_ATTRIBUTES_REJECTED);

        // Add the attribute to the user with admin REST (Case when we are adding new attribute)
        UserResource adminUserResource = null;
        org.keycloak.representations.idm.UserRepresentation adminUserRep = null;
        try {
            adminUserResource = ApiUtil.findUserByUsernameId(testRealm(), user.getUsername());
            adminUserRep = adminUserResource.toRepresentation();
            adminUserRep.singleAttribute(attrName, "foo");
            adminUserResource.update(adminUserRep);
            if (deniedForAdminAsWell) {
                Assert.fail("Not expected to update attribute " + attrName + " by admin REST API");
            }
        } catch (BadRequestException bre) {
            if (!deniedForAdminAsWell) {
                Assert.fail("Was expected to update attribute " + attrName + " by admin REST API");
            }
            return;
        }

        // Update attribute of the user with account REST to the same value (Case when we are updating existing attribute) - should be fine as our attribute is not changed
        user = SimpleHttp.doGet(getAccountUrl(null), httpClient).auth(tokenUtil.getToken()).asJson(UserRepresentation.class);
        Assert.assertEquals("foo", user.getAttributes().get(attrName).get(0));
        user.singleAttribute("someOtherAttr", "foo");
        user = updateAndGet(user);

        // Update attribute of the user with account REST (Case when we are updating existing attribute
        user.singleAttribute(attrName, "foo-updated");
        updateError(user, 400, Messages.UPDATE_READ_ONLY_ATTRIBUTES_REJECTED);

        // Ignore removal of read-only attributes
        user.getAttributes().remove(attrName);
        user = updateAndGet(user);
        assertTrue(user.getAttributes().containsKey(attrName));

        // Revert with admin REST
        adminUserRep.getAttributes().remove(attrName);
        adminUserRep.getAttributes().remove("someOtherAttr");
        adminUserResource.update(adminUserRep);
    }

    private void testAccountUpdateAttributeExpectSuccess(String attrName) throws IOException {
        // Attribute not yet supposed to be on the user
        UserRepresentation user = SimpleHttp.doGet(getAccountUrl(null), httpClient).auth(tokenUtil.getToken()).asJson(UserRepresentation.class);
        Assert.assertThat(user.getAttributes().keySet(), not(contains(attrName)));

        // Assert not possible to add the attribute to the user
        user.singleAttribute(attrName, "foo");
        user = updateAndGet(user);

        // Update attribute of the user with account REST to the same value (Case when we are updating existing attribute) - should be fine as our attribute is not changed
        user = SimpleHttp.doGet(getAccountUrl(null), httpClient).auth(tokenUtil.getToken()).asJson(UserRepresentation.class);
        Assert.assertEquals("foo", user.getAttributes().get(attrName).get(0));
        user.singleAttribute("someOtherAttr", "foo");
        user = updateAndGet(user);

        // Update attribute of the user with account REST (Case when we are updating existing attribute
        user.singleAttribute(attrName, "foo-updated");
        user = updateAndGet(user);

        // Remove attribute from the user with account REST (Case when we are removing existing attribute)
        user.getAttributes().remove(attrName);
        user = updateAndGet(user);

        // Revert
        user.getAttributes().remove("foo");
        user.getAttributes().remove("someOtherAttr");
        user = updateAndGet(user);
    }

    private UserRepresentation updateAndGet(UserRepresentation user) throws IOException {
        int status = SimpleHttp.doPost(getAccountUrl(null), httpClient).auth(tokenUtil.getToken()).json(user).asStatus();
        assertEquals(204, status);
        return SimpleHttp.doGet(getAccountUrl(null), httpClient).auth(tokenUtil.getToken()).asJson(UserRepresentation.class);
    }


    private void updateError(UserRepresentation user, int expectedStatus, String expectedMessage) throws IOException {
        SimpleHttp.Response response = SimpleHttp.doPost(getAccountUrl(null), httpClient).auth(tokenUtil.getToken()).json(user).asResponse();
        assertEquals(expectedStatus, response.getStatus());
        assertEquals(expectedMessage, response.asJson(ErrorRepresentation.class).getErrorMessage());
    }

}
