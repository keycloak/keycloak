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

package org.keycloak.testsuite.federation.kerberos;

import java.net.URI;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.ietf.jgss.GSSCredential;
import org.junit.Assume;
import org.junit.Test;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.common.constants.KerberosConstants;
import org.keycloak.common.util.KerberosSerializationUtils;
import org.keycloak.events.Details;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.protocol.oidc.mappers.UserSessionNoteMapper;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.testsuite.ActionURIUtils;
import org.keycloak.testsuite.Assert;
import org.keycloak.testsuite.admin.ApiUtil;

import static org.keycloak.testsuite.admin.ApiUtil.findClientByClientId;

/**
 * Contains just test methods
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public abstract class AbstractKerberosSingleRealmTest extends AbstractKerberosTest {

    @Test
    public void spnegoNotAvailableTest() throws Exception {
        initHttpClient(false);

        String kcLoginPageLocation = oauth.getLoginFormUrl();

        Response response = client.target(kcLoginPageLocation).request().get();
        Assert.assertEquals(401, response.getStatus());
        Assert.assertEquals(KerberosConstants.NEGOTIATE, response.getHeaderString(HttpHeaders.WWW_AUTHENTICATE));
        String responseText = response.readEntity(String.class);
        response.close();
    }


    // KEYCLOAK-12424
    @Test
    public void spnegoWithInvalidTokenTest() throws Exception {
        initHttpClient(true);

        // Update kerberos configuration with some invalid location of keytab file
        AtomicReference<String> origKeytab = new AtomicReference<>();
        updateUserStorageProvider(kerberosProviderRep -> {
            String keytab = kerberosProviderRep.getConfig().getFirst(KerberosConstants.KEYTAB);
            origKeytab.set(keytab);

            kerberosProviderRep.getConfig().putSingle(KerberosConstants.KEYTAB, keytab + "-invalid");
        });

        try {
            /*
            To do this we do a valid kerberos login on client side.  The authenticator will obtain a valid token, but user
            storage provider is incorrectly configured, so SPNEGO login will fail on server side. However the server should continue to
            the login page (username/password) and return status 200. It should not return 401 with "Kerberos unsupported" page as that
            would display some strange dialogs in the web browser on windows - see KEYCLOAK-12424
            */
            Response spnegoResponse = spnegoLogin("hnelson", "secret");

            Assert.assertEquals(200, spnegoResponse.getStatus());
            String context = spnegoResponse.readEntity(String.class);
            spnegoResponse.close();

            org.junit.Assert.assertTrue(context.contains("Log in to test"));

            events.clear();
        } finally {
            // Revert keytab configuration
            updateUserStorageProvider(kerberosProviderRep -> kerberosProviderRep.getConfig().putSingle(KerberosConstants.KEYTAB, origKeytab.get()));
        }
    }

    // KEYCLOAK-7823
    @Test
    public void spnegoLoginWithRequiredKerberosAuthExecutionTest() {
        AuthenticationExecutionModel.Requirement oldRequirement = updateKerberosAuthExecutionRequirement(
                AuthenticationExecutionModel.Requirement.REQUIRED);
        Response response = spnegoLogin("hnelson", "secret");
        updateKerberosAuthExecutionRequirement(oldRequirement);

        Assert.assertEquals(302, response.getStatus());
    }


    // KEYCLOAK-2102
    @Test
    public void spnegoCaseInsensitiveTest() throws Exception {
        assertSuccessfulSpnegoLogin(getKerberosRule().isCaseSensitiveLogin() ? "MyDuke" : "myduke", "myduke", "theduke");
    }


    @Test
    public void usernamePasswordLoginTest() throws Exception {
        // Change editMode to READ_ONLY
        updateProviderEditMode(UserStorageProvider.EditMode.READ_ONLY);

        // Login with username/password from kerberos
        changePasswordPage.open();
        loginPage.assertCurrent();
        loginPage.login("jduke", "theduke");
        changePasswordPage.assertCurrent();

        // Bad existing password
        changePasswordPage.changePassword("theduke-invalid", "newPass", "newPass");
        Assert.assertTrue(driver.getPageSource().contains("Invalid existing password."));

        // Change password is not possible as editMode is READ_ONLY
        changePasswordPage.changePassword("theduke", "newPass", "newPass");
        Assert.assertTrue(
                driver.getPageSource().contains("You can't update your password as your account is read-only"));

        // Change editMode to UNSYNCED
        updateProviderEditMode(UserStorageProvider.EditMode.UNSYNCED);

        // Successfully change password now
        changePasswordPage.changePassword("theduke", "newPass", "newPass");
        Assert.assertTrue(driver.getPageSource().contains("Your password has been updated."));
        changePasswordPage.logout();

        // Login with old password doesn't work, but with new password works
        loginPage.login("jduke", "theduke");
        loginPage.assertCurrent();
        loginPage.login("jduke", "newPass");
        changePasswordPage.assertCurrent();
        changePasswordPage.logout();

        // Assert SPNEGO login still with the old password as mode is unsynced
        events.clear();
        Response spnegoResponse = spnegoLogin("jduke", "theduke");
        Assert.assertEquals(302, spnegoResponse.getStatus());
        List<UserRepresentation> users = testRealmResource().users().search("jduke", 0, 1);
        String userId = users.get(0).getId();
        events.expectLogin()
                .client("kerberos-app")
                .user(userId)
                .detail(Details.USERNAME, "jduke")
                .assertEvent();

        String codeUrl = spnegoResponse.getLocation().toString();

        assertAuthenticationSuccess(codeUrl);
    }


    @Test
    public void credentialDelegationTest() throws Exception {
        Assume.assumeTrue("Ignoring test as the embedded server is not started", getKerberosRule().isStartEmbeddedLdapServer());
        // Add kerberos delegation credential mapper
        ProtocolMapperModel protocolMapper = UserSessionNoteMapper.createClaimMapper(KerberosConstants.GSS_DELEGATION_CREDENTIAL_DISPLAY_NAME,
                KerberosConstants.GSS_DELEGATION_CREDENTIAL,
                KerberosConstants.GSS_DELEGATION_CREDENTIAL, "String",
                true, false);
        ProtocolMapperRepresentation protocolMapperRep = ModelToRepresentation.toRepresentation(protocolMapper);
        ClientResource clientResource = findClientByClientId(testRealmResource(), "kerberos-app");
        Response response = clientResource.getProtocolMappers().createMapper(protocolMapperRep);
        String protocolMapperId = ApiUtil.getCreatedId(response);
        response.close();

        // SPNEGO login
        AccessToken token = assertSuccessfulSpnegoLogin("hnelson", "hnelson", "secret");

        // Assert kerberos ticket in the accessToken can be re-used to authenticate against other 3rd party kerberos service (ApacheDS Server in this case)
        String serializedGssCredential = (String) token.getOtherClaims().get(KerberosConstants.GSS_DELEGATION_CREDENTIAL);
        Assert.assertNotNull(serializedGssCredential);
        GSSCredential gssCredential = KerberosSerializationUtils.deserializeCredential(serializedGssCredential);
        String ldapResponse = invokeLdap(gssCredential, token.getPreferredUsername());
        Assert.assertEquals("Horatio Nelson", ldapResponse);

        // Logout
        oauth.openLogout();

        // Remove protocolMapper
        clientResource.getProtocolMappers().delete(protocolMapperId);

        // Login and assert delegated credential not anymore
        token = assertSuccessfulSpnegoLogin("hnelson", "hnelson", "secret");
        Assert.assertFalse(token.getOtherClaims().containsKey(KerberosConstants.GSS_DELEGATION_CREDENTIAL));

        events.clear();
    }
}
