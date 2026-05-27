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

package org.keycloak.tests.admin.authentication;

import java.util.List;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ForbiddenException;

import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.Constants;
import org.keycloak.representations.idm.WebAuthnPolicyRepresentation;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.events.AdminEventAssertion;
import org.keycloak.tests.utils.admin.AdminEventPaths;

import com.webauthn4j.data.AttestationConveyancePreference;
import com.webauthn4j.data.AuthenticatorAttachment;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@KeycloakIntegrationTest
public class WebAuthnPolicyTest extends AbstractPolicyTest {

    @AfterEach
    public void after() {
        authMgmtResource.updateWebAuthnPolicy(validWebAuthnPolicy());
        authMgmtResource.updateWebAuthnPasswordlessPolicy(validPasswordlessPolicy());
    }

    @Test
    public void testGetWebAuthnPolicyReturnsDefaultPolicy() {
        WebAuthnPolicyRepresentation result = authMgmtResource.getWebAuthnPolicy();

        Assertions.assertNotNull(result);
        Assertions.assertEquals("keycloak", result.getRpEntityName());
        Assertions.assertEquals(List.of("ES256"), result.getSignatureAlgorithms());
    }

    @Test
    public void testGetWebAuthnPasswordlessPolicyReturnsDefaultPolicy() {
        WebAuthnPolicyRepresentation result = authMgmtResource.getWebAuthnPasswordlessPolicy();

        Assertions.assertNotNull(result);
        Assertions.assertEquals("keycloak", result.getRpEntityName());
        Assertions.assertEquals(List.of("ES256"), result.getSignatureAlgorithms());
    }

    @Test
    public void testGetWebAuthnPolicyReturnsAllBaseFields() {
        WebAuthnPolicyRepresentation result = authMgmtResource.getWebAuthnPolicy();

        Assertions.assertNotNull(result.getRpEntityName());
        Assertions.assertNotNull(result.getSignatureAlgorithms());
        Assertions.assertNotNull(result.getRpId());
        Assertions.assertNotNull(result.getAttestationConveyancePreference());
        Assertions.assertNotNull(result.getAuthenticatorAttachment());
        Assertions.assertNotNull(result.getRequireResidentKey());
        Assertions.assertNotNull(result.getUserVerificationRequirement());
        Assertions.assertNotNull(result.getCreateTimeout());
        Assertions.assertNotNull(result.getAvoidSameAuthenticatorRegister());
        Assertions.assertNotNull(result.getAcceptableAaguids());
        Assertions.assertNotNull(result.getExtraOrigins());
    }

    @Test
    public void testGetWebAuthnPolicyDoesNotExposePasswordlessFields() {
        // passkeys/mediation are passwordless-only; must not leak into this endpoint
        WebAuthnPolicyRepresentation result = authMgmtResource.getWebAuthnPolicy();

        Assertions.assertNull(result.getPasskeysEnabled());
        Assertions.assertNull(result.getMediation());
    }

    @Test
    public void testGetWebAuthnPasswordlessPolicyReturnsPasswordlessExclusiveFields() {
        WebAuthnPolicyRepresentation result = authMgmtResource.getWebAuthnPasswordlessPolicy();
        Assertions.assertNotNull(result.getPasskeysEnabled());

        WebAuthnPolicyRepresentation rep = validPasswordlessPolicyWithPasskeys();
        authMgmtResource.updateWebAuthnPasswordlessPolicy(rep);
        Assertions.assertNotNull(authMgmtResource.getWebAuthnPasswordlessPolicy().getMediation());
    }

    @Test
    public void testGetWebAuthnPasswordlessPolicyIsIsolatedFromWebAuthnPolicy() {
        WebAuthnPolicyRepresentation webauthn = validWebAuthnPolicy();
        webauthn.setRpEntityName("webauthn-app");
        authMgmtResource.updateWebAuthnPolicy(webauthn);

        WebAuthnPolicyRepresentation passwordless = validPasswordlessPolicy();
        passwordless.setRpEntityName("passwordless-app");
        authMgmtResource.updateWebAuthnPasswordlessPolicy(passwordless);

        Assertions.assertEquals("webauthn-app",
                authMgmtResource.getWebAuthnPolicy().getRpEntityName());
        Assertions.assertEquals("passwordless-app",
                authMgmtResource.getWebAuthnPasswordlessPolicy().getRpEntityName());
    }

    @Test
    public void testUpdateWebAuthnPolicyPersistsAllFields() {
        WebAuthnPolicyRepresentation rep = validWebAuthnPolicy();
        rep.setRpEntityName("myapp");
        rep.setSignatureAlgorithms(List.of("ES256", "RS256"));
        rep.setRpId("myapp.example.com");
        rep.setAttestationConveyancePreference(AttestationConveyancePreference.DIRECT.toString());
        rep.setAuthenticatorAttachment(AuthenticatorAttachment.PLATFORM.toString());
        rep.setRequireResidentKey(Constants.WEBAUTHN_POLICY_OPTION_YES);
        rep.setUserVerificationRequirement(Constants.WEBAUTHN_POLICY_OPTION_REQUIRED);
        rep.setCreateTimeout(60);
        rep.setAvoidSameAuthenticatorRegister(true);
        rep.setAcceptableAaguids(List.of("aaguid-1", "aaguid-2"));
        rep.setExtraOrigins(List.of("https://extra.example.com"));
        authMgmtResource.updateWebAuthnPolicy(rep);

        WebAuthnPolicyRepresentation result = authMgmtResource.getWebAuthnPolicy();
        Assertions.assertEquals("myapp", result.getRpEntityName());
        Assertions.assertEquals(List.of("ES256", "RS256"), result.getSignatureAlgorithms());
        Assertions.assertEquals("myapp.example.com", result.getRpId());
        Assertions.assertEquals(AttestationConveyancePreference.DIRECT.toString(), result.getAttestationConveyancePreference());
        Assertions.assertEquals(AuthenticatorAttachment.PLATFORM.toString(), result.getAuthenticatorAttachment());
        Assertions.assertEquals(Constants.WEBAUTHN_POLICY_OPTION_YES, result.getRequireResidentKey());
        Assertions.assertEquals(Constants.WEBAUTHN_POLICY_OPTION_REQUIRED, result.getUserVerificationRequirement());
        Assertions.assertEquals(60, result.getCreateTimeout());
        Assertions.assertTrue(result.getAvoidSameAuthenticatorRegister());
        Assertions.assertEquals(List.of("aaguid-1", "aaguid-2"), result.getAcceptableAaguids());
        Assertions.assertEquals(List.of("https://extra.example.com"), result.getExtraOrigins());
    }

    @Test
    public void testUpdateWebAuthnPasswordlessPolicyPersistsAllFields() {
        WebAuthnPolicyRepresentation rep = validPasswordlessPolicy();
        rep.setRpEntityName("passwordless-app");
        rep.setSignatureAlgorithms(List.of("ES256", "RS256"));
        rep.setRpId("passwordless.example.com");
        rep.setAttestationConveyancePreference(AttestationConveyancePreference.INDIRECT.toString());
        rep.setAuthenticatorAttachment(AuthenticatorAttachment.CROSS_PLATFORM.toString());
        rep.setRequireResidentKey(Constants.WEBAUTHN_POLICY_OPTION_NO);
        rep.setUserVerificationRequirement(Constants.WEBAUTHN_POLICY_OPTION_PREFERED);
        rep.setCreateTimeout(120);
        rep.setAvoidSameAuthenticatorRegister(true);
        rep.setAcceptableAaguids(List.of("aaguid-A", "aaguid-B"));
        rep.setExtraOrigins(List.of("https://extra.example.com"));
        rep.setPasskeysEnabled(true);
        rep.setMediation("required");
        authMgmtResource.updateWebAuthnPasswordlessPolicy(rep);

        WebAuthnPolicyRepresentation result = authMgmtResource.getWebAuthnPasswordlessPolicy();
        Assertions.assertEquals("passwordless-app", result.getRpEntityName());
        Assertions.assertEquals(List.of("ES256", "RS256"), result.getSignatureAlgorithms());
        Assertions.assertEquals("passwordless.example.com", result.getRpId());
        Assertions.assertEquals(AttestationConveyancePreference.INDIRECT.toString(), result.getAttestationConveyancePreference());
        Assertions.assertEquals(AuthenticatorAttachment.CROSS_PLATFORM.toString(), result.getAuthenticatorAttachment());
        Assertions.assertEquals(Constants.WEBAUTHN_POLICY_OPTION_NO, result.getRequireResidentKey());
        Assertions.assertEquals(Constants.WEBAUTHN_POLICY_OPTION_PREFERED, result.getUserVerificationRequirement());
        Assertions.assertEquals(120, result.getCreateTimeout());
        Assertions.assertTrue(result.getAvoidSameAuthenticatorRegister());
        Assertions.assertEquals(List.of("aaguid-A", "aaguid-B"), result.getAcceptableAaguids());
        Assertions.assertEquals(List.of("https://extra.example.com"), result.getExtraOrigins());
        Assertions.assertEquals(true, result.getPasskeysEnabled());
        Assertions.assertEquals("required", result.getMediation());
    }

    @Test
    public void testUpdateWebAuthnPasswordlessPolicyPersistsPasskeysDisabled() {
        WebAuthnPolicyRepresentation rep = validPasswordlessPolicy();
        rep.setPasskeysEnabled(false);
        authMgmtResource.updateWebAuthnPasswordlessPolicy(rep);
        Assertions.assertFalse(
                authMgmtResource.getWebAuthnPasswordlessPolicy().getPasskeysEnabled());
    }

    @Test
    public void testUpdateWebAuthnPolicyFailsOnBlankRpEntityName() {
        WebAuthnPolicyRepresentation rep = validWebAuthnPolicy();
        rep.setRpEntityName("");
        Assertions.assertThrows(BadRequestException.class,
                () -> authMgmtResource.updateWebAuthnPolicy(rep));
    }

    @Test
    public void testUpdateWebAuthnPasswordlessPolicyFailsOnBlankRpEntityName() {
        WebAuthnPolicyRepresentation rep = validPasswordlessPolicy();
        rep.setRpEntityName("");
        Assertions.assertThrows(BadRequestException.class,
                () -> authMgmtResource.updateWebAuthnPasswordlessPolicy(rep));
    }

    @Test
    public void testUpdateWebAuthnPolicyFailsOnWhitespaceOnlyRpEntityName() {
        WebAuthnPolicyRepresentation rep = validWebAuthnPolicy();
        rep.setRpEntityName("   ");
        Assertions.assertThrows(BadRequestException.class,
                () -> authMgmtResource.updateWebAuthnPolicy(rep));
    }

    @Test
    public void testUpdateWebAuthnPasswordlessPolicyFailsOnWhitespaceOnlyRpEntityName() {
        WebAuthnPolicyRepresentation rep = validPasswordlessPolicy();
        rep.setRpEntityName("   ");
        Assertions.assertThrows(BadRequestException.class,
                () -> authMgmtResource.updateWebAuthnPasswordlessPolicy(rep));
    }

    @Test
    public void testUpdateWebAuthnPolicyFailsOnEmptySignatureAlgorithms() {
        WebAuthnPolicyRepresentation rep = validWebAuthnPolicy();
        rep.setSignatureAlgorithms(List.of());
        Assertions.assertThrows(BadRequestException.class,
                () -> authMgmtResource.updateWebAuthnPolicy(rep));
    }

    @Test
    public void testUpdateWebAuthnPasswordlessPolicyFailsOnEmptySignatureAlgorithms() {
        WebAuthnPolicyRepresentation rep = validPasswordlessPolicy();
        rep.setSignatureAlgorithms(List.of());
        Assertions.assertThrows(BadRequestException.class,
                () -> authMgmtResource.updateWebAuthnPasswordlessPolicy(rep));
    }

    @Test
    public void testUpdateWebAuthnPolicyFailsOnUnsupportedSignatureAlgorithms() {
        WebAuthnPolicyRepresentation rep = validWebAuthnPolicy();
        rep.setSignatureAlgorithms(List.of("unsupported"));
        Assertions.assertThrows(BadRequestException.class,
                () -> authMgmtResource.updateWebAuthnPolicy(rep));
    }

    @Test
    public void testUpdateWebAuthnPasswordlessPolicyFailsOnUnsupportedSignatureAlgorithms() {
        WebAuthnPolicyRepresentation rep = validPasswordlessPolicy();
        rep.setSignatureAlgorithms(List.of("unsupported"));
        Assertions.assertThrows(BadRequestException.class,
                () -> authMgmtResource.updateWebAuthnPasswordlessPolicy(rep));
    }

    @Test
    public void testUpdateWebAuthnPolicyAllAttestationConveyancePreferences() {
        List<String> preferences = List.of(
                Constants.DEFAULT_WEBAUTHN_POLICY_NOT_SPECIFIED,
                AttestationConveyancePreference.NONE.toString(),
                AttestationConveyancePreference.INDIRECT.toString(),
                AttestationConveyancePreference.DIRECT.toString());
        for (String pref : preferences) {
            WebAuthnPolicyRepresentation rep = validWebAuthnPolicy();
            rep.setAttestationConveyancePreference(pref);
            authMgmtResource.updateWebAuthnPolicy(rep);
            Assertions.assertEquals(pref,
                    authMgmtResource.getWebAuthnPolicy().getAttestationConveyancePreference(),
                    "Failed for attestation preference: " + pref);
        }
    }

    @Test
    public void testUpdateWebAuthnPasswordlessPolicyAllAttestationConveyancePreferences() {
        List<String> preferences = List.of(
                Constants.DEFAULT_WEBAUTHN_POLICY_NOT_SPECIFIED,
                AttestationConveyancePreference.NONE.toString(),
                AttestationConveyancePreference.INDIRECT.toString(),
                AttestationConveyancePreference.DIRECT.toString());
        for (String pref : preferences) {
            WebAuthnPolicyRepresentation rep = validPasswordlessPolicy();
            rep.setAttestationConveyancePreference(pref);
            authMgmtResource.updateWebAuthnPasswordlessPolicy(rep);
            Assertions.assertEquals(pref,
                    authMgmtResource.getWebAuthnPasswordlessPolicy().getAttestationConveyancePreference(),
                    "Failed for attestation preference: " + pref);
        }
    }

    @Test
    public void testUpdateWebAuthnPolicyFailsOnNullAttestationConveyancePreference() {
        WebAuthnPolicyRepresentation rep = validWebAuthnPolicy();
        rep.setAttestationConveyancePreference(null);
        Assertions.assertThrows(BadRequestException.class,
                () -> authMgmtResource.updateWebAuthnPolicy(rep));
    }

    @Test
    public void testUpdateWebAuthnPasswordlessPolicyFailsOnNullAttestationConveyancePreference() {
        WebAuthnPolicyRepresentation rep = validPasswordlessPolicy();
        rep.setAttestationConveyancePreference(null);
        Assertions.assertThrows(BadRequestException.class,
                () -> authMgmtResource.updateWebAuthnPasswordlessPolicy(rep));
    }

    @Test
    public void testUpdateWebAuthnPolicyFailsOnUnsupportedAttestationConveyancePreference() {
        WebAuthnPolicyRepresentation rep = validWebAuthnPolicy();
        rep.setAttestationConveyancePreference("unsupported");
        Assertions.assertThrows(BadRequestException.class,
                () -> authMgmtResource.updateWebAuthnPolicy(rep));
    }

    @Test
    public void testUpdateWebAuthnPasswordlessPolicyFailsOnUnsupportedAttestationConveyancePreference() {
        WebAuthnPolicyRepresentation rep = validPasswordlessPolicy();
        rep.setAttestationConveyancePreference("unsupported");
        Assertions.assertThrows(BadRequestException.class,
                () -> authMgmtResource.updateWebAuthnPasswordlessPolicy(rep));
    }

    @Test
    public void testUpdateWebAuthnPolicyAllRequireResidentKeyOptions() {
        List<String> preferences = List.of(
                Constants.DEFAULT_WEBAUTHN_POLICY_NOT_SPECIFIED,
                Constants.WEBAUTHN_POLICY_OPTION_YES,
                Constants.WEBAUTHN_POLICY_OPTION_NO);
        for (String option : preferences) {
            WebAuthnPolicyRepresentation rep = validWebAuthnPolicy();
            rep.setRequireResidentKey(option);
            authMgmtResource.updateWebAuthnPolicy(rep);
            Assertions.assertEquals(option,
                    authMgmtResource.getWebAuthnPolicy().getRequireResidentKey(),
                    "Failed for requireResidentKey: " + option);
        }
    }

    @Test
    public void testUpdateWebAuthnPasswordlessPolicyAllRequireResidentKeyOptions() {
        List<String> preferences = List.of(
                Constants.DEFAULT_WEBAUTHN_POLICY_NOT_SPECIFIED,
                Constants.WEBAUTHN_POLICY_OPTION_YES,
                Constants.WEBAUTHN_POLICY_OPTION_NO);
        for (String option : preferences) {
            WebAuthnPolicyRepresentation rep = validPasswordlessPolicy();
            rep.setRequireResidentKey(option);
            authMgmtResource.updateWebAuthnPasswordlessPolicy(rep);
            Assertions.assertEquals(option,
                    authMgmtResource.getWebAuthnPasswordlessPolicy().getRequireResidentKey(),
                    "Failed for requireResidentKey: " + option);
        }
    }

    @Test
    public void testUpdateWebAuthnPolicyFailsOnNullRequireResidentKey() {
        WebAuthnPolicyRepresentation rep = validWebAuthnPolicy();
        rep.setRequireResidentKey(null);
        Assertions.assertThrows(BadRequestException.class,
                () -> authMgmtResource.updateWebAuthnPolicy(rep));
    }

    @Test
    public void testUpdateWebAuthnPasswordlessPolicyFailsOnNullRequireResidentKey() {
        WebAuthnPolicyRepresentation rep = validPasswordlessPolicy();
        rep.setRequireResidentKey(null);
        Assertions.assertThrows(BadRequestException.class,
                () -> authMgmtResource.updateWebAuthnPasswordlessPolicy(rep));
    }

    @Test
    public void testUpdateWebAuthnPolicyFailsOnUnsupportedRequireResidentKey() {
        WebAuthnPolicyRepresentation rep = validWebAuthnPolicy();
        rep.setRequireResidentKey("unsupported");
        Assertions.assertThrows(BadRequestException.class,
                () -> authMgmtResource.updateWebAuthnPolicy(rep));
    }

    @Test
    public void testUpdateWebAuthnPasswordlessPolicyFailsOnUnsupportedRequireResidentKey() {
        WebAuthnPolicyRepresentation rep = validPasswordlessPolicy();
        rep.setRequireResidentKey("unsupported");
        Assertions.assertThrows(BadRequestException.class,
                () -> authMgmtResource.updateWebAuthnPasswordlessPolicy(rep));
    }

    @Test
    public void testUpdateWebAuthnPolicyAllAuthenticatorAttachmentOptions() {
        List<String> options = List.of(
                Constants.DEFAULT_WEBAUTHN_POLICY_NOT_SPECIFIED,
                AuthenticatorAttachment.PLATFORM.toString(),
                AuthenticatorAttachment.CROSS_PLATFORM.toString());
        for (String attachment : options) {
            WebAuthnPolicyRepresentation rep = validWebAuthnPolicy();
            rep.setAuthenticatorAttachment(attachment);
            authMgmtResource.updateWebAuthnPolicy(rep);
            Assertions.assertEquals(attachment,
                    authMgmtResource.getWebAuthnPolicy().getAuthenticatorAttachment(),
                    "Failed for authenticatorAttachment: " + attachment);
        }
    }

    @Test
    public void testUpdateWebAuthnPasswordlessPolicyAllAuthenticatorAttachmentOptions() {
        List<String> options = List.of(
                Constants.DEFAULT_WEBAUTHN_POLICY_NOT_SPECIFIED,
                AuthenticatorAttachment.PLATFORM.toString(),
                AuthenticatorAttachment.CROSS_PLATFORM.toString());
        for (String attachment : options) {
            WebAuthnPolicyRepresentation rep = validPasswordlessPolicy();
            rep.setAuthenticatorAttachment(attachment);
            authMgmtResource.updateWebAuthnPasswordlessPolicy(rep);
            Assertions.assertEquals(attachment,
                    authMgmtResource.getWebAuthnPasswordlessPolicy().getAuthenticatorAttachment(),
                    "Failed for authenticatorAttachment: " + attachment);
        }
    }

    @Test
    public void testUpdateWebAuthnPolicyFailsOnNullAuthenticatorAttachment() {
        WebAuthnPolicyRepresentation rep = validWebAuthnPolicy();
        rep.setAuthenticatorAttachment(null);
        Assertions.assertThrows(BadRequestException.class,
                () -> authMgmtResource.updateWebAuthnPolicy(rep));
    }

    @Test
    public void testUpdateWebAuthnPasswordlessPolicyFailsOnNullAuthenticatorAttachment() {
        WebAuthnPolicyRepresentation rep = validPasswordlessPolicy();
        rep.setAuthenticatorAttachment(null);
        Assertions.assertThrows(BadRequestException.class,
                () -> authMgmtResource.updateWebAuthnPasswordlessPolicy(rep));
    }

    @Test
    public void testUpdateWebAuthnPolicyFailsOnUnsupportedAuthenticatorAttachment() {
        WebAuthnPolicyRepresentation rep = validWebAuthnPolicy();
        rep.setAuthenticatorAttachment("unsupported");
        Assertions.assertThrows(BadRequestException.class,
                () -> authMgmtResource.updateWebAuthnPolicy(rep));
    }

    @Test
    public void testUpdateWebAuthnPasswordlessPolicyFailsOnUnsupportedAuthenticatorAttachment() {
        WebAuthnPolicyRepresentation rep = validPasswordlessPolicy();
        rep.setAuthenticatorAttachment("unsupported");
        Assertions.assertThrows(BadRequestException.class,
                () -> authMgmtResource.updateWebAuthnPasswordlessPolicy(rep));
    }

    @Test
    public void testUpdateWebAuthnPolicyAllUserVerificationRequirements() {
        List<String> options = List.of(
                Constants.DEFAULT_WEBAUTHN_POLICY_NOT_SPECIFIED,
                Constants.WEBAUTHN_POLICY_OPTION_REQUIRED,
                Constants.WEBAUTHN_POLICY_OPTION_PREFERED,
                Constants.WEBAUTHN_POLICY_OPTION_DISCOURAGED);
        for (String requirement : options) {
            WebAuthnPolicyRepresentation rep = validWebAuthnPolicy();
            rep.setUserVerificationRequirement(requirement);
            authMgmtResource.updateWebAuthnPolicy(rep);
            Assertions.assertEquals(requirement,
                    authMgmtResource.getWebAuthnPolicy().getUserVerificationRequirement(),
                    "Failed for userVerificationRequirement: " + requirement);
        }
    }

    @Test
    public void testUpdateWebAuthnPasswordlessPolicyAllUserVerificationRequirements() {
        List<String> options = List.of(
                Constants.DEFAULT_WEBAUTHN_POLICY_NOT_SPECIFIED,
                Constants.WEBAUTHN_POLICY_OPTION_REQUIRED,
                Constants.WEBAUTHN_POLICY_OPTION_PREFERED,
                Constants.WEBAUTHN_POLICY_OPTION_DISCOURAGED);
        for (String requirement : options) {
            WebAuthnPolicyRepresentation rep = validPasswordlessPolicy();
            rep.setUserVerificationRequirement(requirement);
            authMgmtResource.updateWebAuthnPasswordlessPolicy(rep);
            Assertions.assertEquals(requirement,
                    authMgmtResource.getWebAuthnPasswordlessPolicy().getUserVerificationRequirement(),
                    "Failed for userVerificationRequirement: " + requirement);
        }
    }

    @Test
    public void testUpdateWebAuthnPolicyFailsOnNullUserVerificationRequirement() {
        WebAuthnPolicyRepresentation rep = validWebAuthnPolicy();
        rep.setUserVerificationRequirement(null);
        Assertions.assertThrows(BadRequestException.class,
                () -> authMgmtResource.updateWebAuthnPolicy(rep));
    }

    @Test
    public void testUpdateWebAuthnPasswordlessPolicyFailsOnNullUserVerificationRequirement() {
        WebAuthnPolicyRepresentation rep = validPasswordlessPolicy();
        rep.setUserVerificationRequirement(null);
        Assertions.assertThrows(BadRequestException.class,
                () -> authMgmtResource.updateWebAuthnPasswordlessPolicy(rep));
    }

    @Test
    public void testUpdateWebAuthnPolicyFailsOnUnsupportedUserVerificationRequirement() {
        WebAuthnPolicyRepresentation rep = validWebAuthnPolicy();
        rep.setUserVerificationRequirement("unsupported");
        Assertions.assertThrows(BadRequestException.class,
                () -> authMgmtResource.updateWebAuthnPolicy(rep));
    }

    @Test
    public void testUpdateWebAuthnPasswordlessPolicyFailsOnUnsupportedUserVerificationRequirement() {
        WebAuthnPolicyRepresentation rep = validPasswordlessPolicy();
        rep.setUserVerificationRequirement("unsupported");
        Assertions.assertThrows(BadRequestException.class,
                () -> authMgmtResource.updateWebAuthnPasswordlessPolicy(rep));
    }

    @Test
    public void testUpdateWebAuthnPasswordlessPolicyAllValidMediationValuesWithPasskeysEnabled() {
        for (String mediation : List.of("conditional", "none", "optional", "required", "silent")) {
            WebAuthnPolicyRepresentation rep = validPasswordlessPolicyWithPasskeys();
            rep.setMediation(mediation);
            authMgmtResource.updateWebAuthnPasswordlessPolicy(rep);

            WebAuthnPolicyRepresentation result = authMgmtResource.getWebAuthnPasswordlessPolicy();
            Assertions.assertTrue(result.getPasskeysEnabled(),
                    "passkeysEnabled should be true for mediation: " + mediation);
            Assertions.assertEquals(mediation, result.getMediation(),
                    "Mediation mismatch for: " + mediation);
        }
    }

    @Test
    public void testUpdateWebAuthnPasswordlessPolicyFailsOnNullMediationWhenPasskeysEnabled() {
        WebAuthnPolicyRepresentation rep = validPasswordlessPolicyWithPasskeys();
        rep.setMediation(null);
        Assertions.assertThrows(BadRequestException.class,
                () -> authMgmtResource.updateWebAuthnPasswordlessPolicy(rep));
    }

    @Test
    public void testUpdateWebAuthnPasswordlessPolicyFailsOnUnsupportedMediationWhenPasskeysEnabled() {
        WebAuthnPolicyRepresentation rep = validPasswordlessPolicyWithPasskeys();
        rep.setMediation("unsupported");
        Assertions.assertThrows(BadRequestException.class,
                () -> authMgmtResource.updateWebAuthnPasswordlessPolicy(rep));
    }

    @Test
    public void testUpdateWebAuthnPasswordlessPolicyMediationNotValidatedWhenPasskeysDisabled() {
        // Validation gate is skipped entirely when passkeysEnabled == false
        WebAuthnPolicyRepresentation rep = validPasswordlessPolicy();
        rep.setPasskeysEnabled(false);
        rep.setMediation("unsupported");
        Assertions.assertDoesNotThrow(
                () -> authMgmtResource.updateWebAuthnPasswordlessPolicy(rep));
    }

    @Test
    public void testUpdateWebAuthnPasswordlessPolicyMediationNotValidatedWhenPasskeysNull() {
        // Validation gate is also skipped when passkeysEnabled == null
        WebAuthnPolicyRepresentation rep = validPasswordlessPolicy();
        rep.setPasskeysEnabled(null);
        rep.setMediation("unsupported");
        Assertions.assertDoesNotThrow(
                () -> authMgmtResource.updateWebAuthnPasswordlessPolicy(rep));
    }

    @Test
    public void testUpdateWebAuthnPolicyOverwritesPreviousPolicy() {
        WebAuthnPolicyRepresentation first = validWebAuthnPolicy();
        first.setRpEntityName("first-app");
        authMgmtResource.updateWebAuthnPolicy(first);

        WebAuthnPolicyRepresentation second = validWebAuthnPolicy();
        second.setRpEntityName("second-app");
        authMgmtResource.updateWebAuthnPolicy(second);

        Assertions.assertEquals("second-app",
                authMgmtResource.getWebAuthnPolicy().getRpEntityName());
    }

    @Test
    public void testUpdateWebAuthnPasswordlessPolicyOverwritesPreviousPolicy() {
        WebAuthnPolicyRepresentation first = validPasswordlessPolicy();
        first.setRpEntityName("first-passwordless");
        authMgmtResource.updateWebAuthnPasswordlessPolicy(first);

        WebAuthnPolicyRepresentation second = validPasswordlessPolicy();
        second.setRpEntityName("second-passwordless");
        authMgmtResource.updateWebAuthnPasswordlessPolicy(second);

        Assertions.assertEquals("second-passwordless",
                authMgmtResource.getWebAuthnPasswordlessPolicy().getRpEntityName());
    }

    @Test
    public void testUpdateWebAuthnPolicyIsIdempotent() {
        WebAuthnPolicyRepresentation rep = validWebAuthnPolicy();
        authMgmtResource.updateWebAuthnPolicy(rep);
        authMgmtResource.updateWebAuthnPolicy(rep);

        Assertions.assertEquals(rep.getRpEntityName(),
                authMgmtResource.getWebAuthnPolicy().getRpEntityName());
    }

    @Test
    public void testUpdateWebAuthnPasswordlessPolicyIsIdempotent() {
        WebAuthnPolicyRepresentation rep = validPasswordlessPolicy();
        authMgmtResource.updateWebAuthnPasswordlessPolicy(rep);
        authMgmtResource.updateWebAuthnPasswordlessPolicy(rep);

        Assertions.assertEquals(rep.getRpEntityName(),
                authMgmtResource.getWebAuthnPasswordlessPolicy().getRpEntityName());
    }

    @Test
    public void testUpdateWebAuthnPolicyRequiresViewRealmPermission() {
        Assertions.assertThrows(ForbiddenException.class,
                () -> clients.get("view-users").realm(managedRealm.getId()).flows().getWebAuthnPolicy());
        Assertions.assertDoesNotThrow(
                () -> clients.get("view-realm").realm(managedRealm.getId()).flows().getWebAuthnPolicy()
        );
    }

    @Test
    public void testUpdateWebAuthnPasswordlessPolicyRequiresViewRealmPermission() {
        Assertions.assertThrows(ForbiddenException.class,
                () -> clients.get("view-users").realm(managedRealm.getId()).flows().getWebAuthnPasswordlessPolicy());
        Assertions.assertDoesNotThrow(
                () -> clients.get("view-realm").realm(managedRealm.getId()).flows().getWebAuthnPasswordlessPolicy()
        );
    }

    @Test
    public void testUpdateWebAuthnPolicyRequiresManageRealmPermission() {
        Assertions.assertThrows(ForbiddenException.class,
                () -> clients.get("view-realm").realm(managedRealm.getId()).flows().updateWebAuthnPolicy(validWebAuthnPolicy()));
        Assertions.assertDoesNotThrow(
                () -> clients.get("manage-realm").realm(managedRealm.getId()).flows()
                        .updateWebAuthnPolicy(validWebAuthnPolicy())
        );
    }

    @Test
    public void testUpdateWebAuthnPasswordlessPolicyRequiresManageRealmPermission() {
        Assertions.assertThrows(ForbiddenException.class,
                () -> clients.get("view-realm").realm(managedRealm.getId()).flows().updateWebAuthnPasswordlessPolicy(validPasswordlessPolicy()));
        Assertions.assertDoesNotThrow(
                () -> clients.get("manage-realm").realm(managedRealm.getId()).flows()
                        .updateWebAuthnPasswordlessPolicy(validPasswordlessPolicy())
        );
    }

    @Test
    public void testUpdateWebAuthnPolicySuccessTriggersAdminEvent() {
        authMgmtResource.updateWebAuthnPolicy(validWebAuthnPolicy());

        AdminEventAssertion.assertSuccess(adminEvents.poll())
                .operationType(OperationType.UPDATE)
                .resourceType(ResourceType.AUTHENTICATION_POLICY)
                .resourcePath(AdminEventPaths.webAuthnPolicyPath())
                .representation(validWebAuthnPolicy());
    }

    @Test
    public void testUpdateWebAuthnPasswordlessPolicySuccessTriggersAdminEvent() {
        authMgmtResource.updateWebAuthnPasswordlessPolicy(validPasswordlessPolicy());

        AdminEventAssertion.assertSuccess(adminEvents.poll())
                .operationType(OperationType.UPDATE)
                .resourceType(ResourceType.AUTHENTICATION_POLICY)
                .resourcePath(AdminEventPaths.webAuthnPasswordlessPolicyPath())
                .representation(validPasswordlessPolicy());
    }

    @Test
    public void testUpdateWebAuthnPolicyFailureDoesNotTriggerAdminEvent() {
        WebAuthnPolicyRepresentation rep = new WebAuthnPolicyRepresentation();

        Assertions.assertThrows(
                BadRequestException.class,
                () -> authMgmtResource.updateWebAuthnPolicy(rep)
        );

        Assertions.assertNull(adminEvents.poll());
    }

    @Test
    public void testUpdateWebAuthnPasswordlessPolicyFailureDoesNotTriggerAdminEvent() {
        WebAuthnPolicyRepresentation rep = new WebAuthnPolicyRepresentation();

        Assertions.assertThrows(
                BadRequestException.class,
                () -> authMgmtResource.updateWebAuthnPasswordlessPolicy(rep)
        );

        Assertions.assertNull(adminEvents.poll());
    }

    private WebAuthnPolicyRepresentation validWebAuthnPolicy() {
        WebAuthnPolicyRepresentation rep = new WebAuthnPolicyRepresentation();
        rep.setRpEntityName("keycloak");
        rep.setSignatureAlgorithms(List.of("ES256"));
        rep.setRpId("");
        rep.setAttestationConveyancePreference(Constants.DEFAULT_WEBAUTHN_POLICY_NOT_SPECIFIED);
        rep.setAuthenticatorAttachment(Constants.DEFAULT_WEBAUTHN_POLICY_NOT_SPECIFIED);
        rep.setRequireResidentKey(Constants.DEFAULT_WEBAUTHN_POLICY_NOT_SPECIFIED);
        rep.setUserVerificationRequirement(Constants.DEFAULT_WEBAUTHN_POLICY_NOT_SPECIFIED);
        rep.setCreateTimeout(0);
        rep.setAvoidSameAuthenticatorRegister(false);
        rep.setAcceptableAaguids(List.of());
        rep.setExtraOrigins(List.of());
        return rep;
    }

    private WebAuthnPolicyRepresentation validPasswordlessPolicy() {
        WebAuthnPolicyRepresentation rep = validWebAuthnPolicy();
        rep.setPasskeysEnabled(false);
        rep.setMediation(null);
        return rep;
    }

    private WebAuthnPolicyRepresentation validPasswordlessPolicyWithPasskeys() {
        WebAuthnPolicyRepresentation rep = validWebAuthnPolicy();
        rep.setPasskeysEnabled(true);
        rep.setMediation("conditional");
        return rep;
    }
}
