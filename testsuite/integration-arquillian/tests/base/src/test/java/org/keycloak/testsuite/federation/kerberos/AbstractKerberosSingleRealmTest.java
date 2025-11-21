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

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;

import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.common.constants.KerberosConstants;
import org.keycloak.common.util.KerberosSerializationUtils;
import org.keycloak.events.Details;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.protocol.oidc.mappers.UserSessionNoteMapper;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.UserInfo;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.testsuite.Assert;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.pages.AppPage;
import org.keycloak.testsuite.util.AccountHelper;
import org.keycloak.testsuite.util.TestAppHelper;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;

import org.ietf.jgss.GSSCredential;
import org.jboss.arquillian.graphene.page.Page;
import org.junit.Assume;
import org.junit.Test;

import static org.keycloak.testsuite.admin.ApiUtil.findClientByClientId;

/**
 * Contains just test methods
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public abstract class AbstractKerberosSingleRealmTest extends AbstractKerberosTest {

    @Page
    protected AppPage appPage;

    @Test
    public void spnegoNotAvailableTest() throws Exception {
        initHttpClient(false);

        String kcLoginPageLocation = oauth.loginForm().build();

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

            org.junit.Assert.assertTrue(context.contains("Sign in to test"));

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

        TestAppHelper testAppHelper = new TestAppHelper(oauth, loginPage, appPage);

        Assert.assertTrue(testAppHelper.login("jduke", "theduke"));
        Assert.assertTrue(testAppHelper.logout());

        // Change password is not possible as editMode is READ_ONLY
        Assert.assertFalse(AccountHelper.updatePassword(testRealmResource(), "jduke", "newPass"));

        Assert.assertFalse(testAppHelper.login("jduke", "newPass"));

        // Change editMode to UNSYNCED
        updateProviderEditMode(UserStorageProvider.EditMode.UNSYNCED);

        // Successfully change password now
        Assert.assertTrue(AccountHelper.updatePassword(testRealmResource(), "jduke", "newPass"));

        // Login with old password doesn't work, but with new password works
        Assert.assertFalse(testAppHelper.login("jduke", "theduke"));
        Assert.assertTrue(testAppHelper.login("jduke", "newPass"));

        testAppHelper.logout();

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
                true, false, true, true);
        ProtocolMapperRepresentation protocolMapperRep = ModelToRepresentation.toRepresentation(protocolMapper);
        ClientResource clientResource = findClientByClientId(testRealmResource(), "kerberos-app");
        Response response = clientResource.getProtocolMappers().createMapper(protocolMapperRep);
        String protocolMapperId = ApiUtil.getCreatedId(response);
        response.close();

        // SPNEGO login
        AccessTokenResponse tokenResponse = assertSuccessfulSpnegoLogin("hnelson", "hnelson", "secret");
        AccessToken token = oauth.verifyToken(tokenResponse.getAccessToken());

        // Assert kerberos ticket in the accessToken can be re-used to authenticate against other 3rd party kerberos service (ApacheDS Server in this case)
        String serializedGssCredential = (String) token.getOtherClaims().get(KerberosConstants.GSS_DELEGATION_CREDENTIAL);
        Assert.assertNotNull(serializedGssCredential);
        GSSCredential gssCredential = KerberosSerializationUtils.deserializeCredential(serializedGssCredential);
        String ldapResponse = invokeLdap(gssCredential, token.getPreferredUsername());
        Assert.assertEquals("Horatio Nelson", ldapResponse);

        // Assert kerberos ticket also in userinfo endpoint
        UserInfo userInfo = oauth.doUserInfoRequest(tokenResponse.getAccessToken()).getUserInfo();
        Assert.assertEquals(serializedGssCredential, userInfo.getOtherClaims().get(KerberosConstants.GSS_DELEGATION_CREDENTIAL));
        // Clear USER_INFO_REQUEST event
        events.poll();

        // Logout
        oauth.logoutForm().idTokenHint(tokenResponse.getIdToken()).open();
        events.poll();

        // Remove protocolMapper
        clientResource.getProtocolMappers().delete(protocolMapperId);

        // Login and assert delegated credential not anymore
        tokenResponse = assertSuccessfulSpnegoLogin("hnelson", "hnelson", "secret");
        token = oauth.verifyToken(tokenResponse.getAccessToken());
        Assert.assertFalse(token.getOtherClaims().containsKey(KerberosConstants.GSS_DELEGATION_CREDENTIAL));
        userInfo = oauth.doUserInfoRequest(tokenResponse.getAccessToken()).getUserInfo();
        Assert.assertFalse(userInfo.getOtherClaims().containsKey(KerberosConstants.GSS_DELEGATION_CREDENTIAL));

        events.clear();
    }
}
