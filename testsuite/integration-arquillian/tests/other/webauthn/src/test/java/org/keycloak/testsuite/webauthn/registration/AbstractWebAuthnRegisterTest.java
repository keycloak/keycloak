/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.testsuite.webauthn.registration;

import org.jboss.arquillian.graphene.page.Page;
import org.junit.Rule;
import org.keycloak.authentication.requiredactions.WebAuthnPasswordlessRegisterFactory;
import org.keycloak.authentication.requiredactions.WebAuthnRegisterFactory;
import org.keycloak.common.util.SecretGenerator;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RequiredActionProviderRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.admin.AbstractAdminTest;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude;
import org.keycloak.testsuite.pages.AppPage;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.pages.RegisterPage;
import org.keycloak.testsuite.webauthn.AbstractWebAuthnVirtualTest;
import org.keycloak.testsuite.webauthn.pages.WebAuthnErrorPage;
import org.keycloak.testsuite.webauthn.pages.WebAuthnRegisterPage;
import org.openqa.selenium.virtualauthenticator.Credential;

import javax.ws.rs.core.Response;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude.AuthServer.REMOTE;
import static org.keycloak.testsuite.util.WaitUtils.waitForPageToLoad;

/**
 * @author <a href="mailto:mabartos@redhat.com">Martin Bartos</a>
 */
@AuthServerContainerExclude(REMOTE)
public abstract class AbstractWebAuthnRegisterTest extends AbstractWebAuthnVirtualTest {

    @Rule
    public AssertEvents events = new AssertEvents(this);

    @Page
    protected LoginPage loginPage;

    @Page
    protected RegisterPage registerPage;

    @Page
    protected WebAuthnRegisterPage webAuthnRegisterPage;

    @Page
    protected WebAuthnErrorPage webAuthnErrorPage;

    @Page
    protected AppPage appPage;

    protected static final String USERNAME = "registerUserWebAuthnSuccess";
    protected static final String PASSWORD = "password";
    protected static final String EMAIL = "registerUserWebAuthnSuccess@email";

    protected final static String base64EncodedPK =
            "MIGHAgEAMBMGByqGSM49AgEGCCqGSM49AwEHBG0wawIBAQQg8_zMDQDYAxlU-Q"
                    + "hk1Dwkf0v18GZca1DMF3SaJ9HPdmShRANCAASNYX5lyVCOZLzFZzrIKmeZ2jwU"
                    + "RmgsJYxGP__fWN_S-j5sN4tT15XEpN_7QZnt14YvI6uvAgO0uJEboFaZlOEB";

    protected final static PKCS8EncodedKeySpec privateKey = new PKCS8EncodedKeySpec(Base64.getUrlDecoder().decode(base64EncodedPK));


    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        RealmRepresentation realmRepresentation = AbstractAdminTest.loadJson(getClass().getResourceAsStream("/webauthn/testrealm-webauthn.json"), RealmRepresentation.class);

        if (isPasswordless()) {
            makePasswordlessRequiredActionDefault(realmRepresentation);
        }

        testRealms.add(realmRepresentation);
    }

    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {
    }

    @Override
    protected void postAfterAbstractKeycloak() {
        List<UserRepresentation> defaultUser = testRealm().users().search(USERNAME, true);
        if (defaultUser != null && !defaultUser.isEmpty()) {
            Response response = testRealm().users().delete(defaultUser.get(0).getId());
            assertThat(response, notNullValue());
            assertThat(response.getStatus(), is(204));
        }
    }

    protected void registerDefaultWebAuthnUser(boolean promptLabel) {
        if (promptLabel) {
            registerDefaultWebAuthnUser();
        } else {
            registerDefaultWebAuthnUser(null);
        }
    }

    protected void registerDefaultWebAuthnUser(String authenticatorLabel) {
        registerWebAuthnUser(USERNAME, PASSWORD, EMAIL, authenticatorLabel);
    }

    protected void registerDefaultWebAuthnUser() {
        registerDefaultWebAuthnUser(SecretGenerator.getInstance().randomString(24));
    }

    protected void registerWebAuthnUser(String username, String password, String email, String authenticatorLabel) {
        loginPage.open();
        loginPage.clickRegister();

        waitForPageToLoad();
        registerPage.assertCurrent();
        registerPage.register("firstName", "lastName", email, username, password, password);

        // User was registered. Now he needs to register WebAuthn credential
        waitForPageToLoad();
        webAuthnRegisterPage.assertCurrent();
        webAuthnRegisterPage.clickRegister();

        if (authenticatorLabel != null) {
            webAuthnRegisterPage.registerWebAuthnCredential(authenticatorLabel);
        }
    }

    protected String displayErrorMessageIfPresent() {
        if (webAuthnErrorPage.isCurrent()) {
            final String msg = webAuthnErrorPage.getError();
            log.info("Error message from Error Page: " + msg);
            return msg;
        }
        return null;
    }

    protected Credential getDefaultResidentKeyCredential() {
        byte[] credentialId = {1, 2, 3, 4};
        byte[] userHandle = {1};
        return Credential.createResidentCredential(credentialId, "localhost", privateKey, userHandle, 0);
    }

    protected Credential getDefaultNonResidentKeyCredential() {
        byte[] credentialId = {1, 2, 3, 4};
        return Credential.createNonResidentCredential(credentialId, "localhost", privateKey, 0);
    }

    protected static void makePasswordlessRequiredActionDefault(RealmRepresentation realm) {
        RequiredActionProviderRepresentation webAuthnProvider = realm.getRequiredActions()
                .stream()
                .filter(f -> f.getProviderId().equals(WebAuthnRegisterFactory.PROVIDER_ID))
                .findFirst()
                .orElse(null);
        assertThat(webAuthnProvider, notNullValue());

        webAuthnProvider.setEnabled(false);

        RequiredActionProviderRepresentation webAuthnPasswordlessProvider = realm.getRequiredActions()
                .stream()
                .filter(f -> f.getProviderId().equals(WebAuthnPasswordlessRegisterFactory.PROVIDER_ID))
                .findFirst()
                .orElse(null);
        assertThat(webAuthnPasswordlessProvider, notNullValue());

        webAuthnPasswordlessProvider.setEnabled(true);
        webAuthnPasswordlessProvider.setDefaultAction(true);
    }
}
