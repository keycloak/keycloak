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

package org.keycloak.tests.webauthn;

import java.util.List;

import jakarta.ws.rs.BadRequestException;

import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.common.util.SecretGenerator;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testframework.annotations.InjectKeycloakUrls;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.server.KeycloakUrls;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.JavascriptExecutor;

/**
 * Checks that the hints set in the WebAuthn policy reach navigator.credentials.create() and navigator.credentials.get() in the order they were configured, and that nothing is sent when none are set.
 * The passwordless subclass runs the same checks against the passwordless policy.
 */
@KeycloakIntegrationTest
public class WebAuthnHintsTest extends AbstractWebAuthnVirtualTest {

    @InjectKeycloakUrls
    KeycloakUrls keycloakUrls;

    @Test
    public void registrationWithHints() {
        managedRealm.cleanup().add(RealmResource::logoutAll);

        setHints(List.of("hybrid", "security-key", "client-device"));
        registerWithCapture("hints-registration", captureOptions("create"));

        Assertions.assertEquals("hybrid,security-key,client-device", getCapturedHints());
    }

    @Test
    public void registrationWithoutHints() {
        managedRealm.cleanup().add(RealmResource::logoutAll);

        // Hints set on the other policy must not be used by this flow, so nothing should be sent here.
        setOtherPolicyHints(List.of("hybrid"));
        registerWithCapture("no-hints-registration", captureOptions("create"));

        Assertions.assertEquals("absent", getCapturedHints());
    }

    @Test
    public void authenticationWithHints() {
        managedRealm.cleanup().add(RealmResource::logoutAll);

        registerUserWithCredential("hints-authentication");
        setHints(List.of("security-key", "hybrid"));

        authenticateWithCapture("hints-authentication", captureOptions("get"));

        Assertions.assertEquals("security-key,hybrid", getCapturedHints());
    }

    @Test
    public void authenticationWithoutHints() {
        managedRealm.cleanup().add(RealmResource::logoutAll);

        registerUserWithCredential("no-hints-authentication");
        // Hints set on the other policy must not be used by this flow, so nothing should be sent here.
        setOtherPolicyHints(List.of("hybrid"));

        authenticateWithCapture("no-hints-authentication", captureOptions("get"));

        Assertions.assertEquals("absent", getCapturedHints());
    }

    @Test
    public void registerAndAuthenticateWithHints() {
        managedRealm.cleanup().add(RealmResource::logoutAll);

        setHints(List.of("security-key", "client-device"));

        registerUserWithCredential("hints-login");
        authenticateUser("hints-login", PASSWORD, true);
    }

    @Test
    public void hintsRoundTrip() {
        Assertions.assertEquals(List.of(), getHints(managedRealm.admin().toRepresentation()));

        setHints(List.of("hybrid", "security-key"));

        RealmRepresentation realmRep = managedRealm.admin().toRepresentation();
        Assertions.assertEquals(List.of("hybrid", "security-key"), getHints(realmRep));
        Assertions.assertEquals(List.of(), getOtherPolicyHints(realmRep));
        // Hints are saved as realm attributes, but they are only exposed through the dedicated fields.
        Assertions.assertFalse(realmRep.getAttributes().containsKey("webAuthnPolicyHints"));
        Assertions.assertFalse(realmRep.getAttributes().containsKey("webAuthnPolicyHintsPasswordless"));

        setHints(List.of());

        Assertions.assertEquals(List.of(), getHints(managedRealm.admin().toRepresentation()));
    }

    @Test
    public void invalidHintRejected() {
        // Hints are saved as one comma separated attribute, so a value containing a comma would come back as two hints, and an empty value would be dropped.
        Assertions.assertThrows(BadRequestException.class, () -> setHints(List.of("security-key,hybrid")));
        Assertions.assertThrows(BadRequestException.class, () -> setHints(List.of("security-key", "")));

        Assertions.assertEquals(List.of(), getHints(managedRealm.admin().toRepresentation()));
    }

    private void registerUserWithCredential(String username) {
        registerUser(username, PASSWORD, username + "@email", SecretGenerator.getInstance().randomString(24), true);

        Assertions.assertTrue(oAuthClient.parseLoginResponse().isSuccess());
        logout();
    }

    //Registers a user and its WebAuthn credential, with the capture script in place for the credential call.
    private void registerWithCapture(String username, String captureScript) {
        oAuthClient.openRegistrationForm();
        registerPage.assertCurrent();
        registerPage.register("firstName", "lastName", username + "@email", username, PASSWORD);

        webAuthnRegisterPage.assertCurrent();
        ((JavascriptExecutor) driver.driver()).executeScript(captureScript);

        webAuthnRegisterPage.clickRegister();
        tryRegisterAuthenticator(SecretGenerator.getInstance().randomString(24));

        Assertions.assertTrue(oAuthClient.parseLoginResponse().isSuccess());
    }

    //Logs the user in with its WebAuthn credential, with the capture script in place for the credential call.
    private void authenticateWithCapture(String username, String captureScript) {
        oAuthClient.openLoginForm();
        loginPage.assertCurrent();
        loginPage.fillLogin(username, PASSWORD);
        loginPage.submit();

        webAuthnLoginPage.assertCurrent();
        ((JavascriptExecutor) driver.driver()).executeScript(captureScript);

        webAuthnLoginPage.clickAuthenticate();

        Assertions.assertTrue(oAuthClient.parseLoginResponse().isSuccess());
    }

    // The hints the page passed to the browser, in order, or "absent" when it passed none. A finished flow leaves the browser on the application, so this goes back to a Keycloak page first: that is where the value was stored.
    protected String getCapturedHints() {
        driver.driver().get(keycloakUrls.getBase());
        return (String) ((JavascriptExecutor) driver.driver()).executeScript("return localStorage.getItem('captured_hints');");
    }

    private void setHints(List<String> hints) {
        managedRealm.updateWithCleanup(r -> r.webAuthn(isPasswordless(), builder -> builder.hints(hints)));
    }

    private void setOtherPolicyHints(List<String> hints) {
        managedRealm.updateWithCleanup(r -> r.webAuthn(!isPasswordless(), builder -> builder.hints(hints)));
    }

    private List<String> getHints(RealmRepresentation realmRep) {
        return isPasswordless() ? realmRep.getWebAuthnPolicyPasswordlessHints() : realmRep.getWebAuthnPolicyHints();
    }

    private List<String> getOtherPolicyHints(RealmRepresentation realmRep) {
        return isPasswordless() ? realmRep.getWebAuthnPolicyHints() : realmRep.getWebAuthnPolicyPasswordlessHints();
    }

    // JS capture

    /**
     * The hints are part of the options the login page passes to navigator.credentials.create() or get(), and those
     * options never reach the server. This wraps the call so it saves the hints first, then runs as usual.
     */
    protected static String captureOptions(String method) {
        return "localStorage.removeItem('captured_hints');" +
                "const orig = navigator.credentials." + method + ".bind(navigator.credentials);" +
                "navigator.credentials." + method + " = function(opts) {" +
                "  const hints = opts.publicKey.hints;" +
                "  localStorage.setItem('captured_hints', hints === undefined ? 'absent' : hints.join(','));" +
                "  return orig(opts);" +
                "};";
    }
}
