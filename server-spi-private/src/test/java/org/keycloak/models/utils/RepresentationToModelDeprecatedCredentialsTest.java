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

package org.keycloak.models.utils;

import java.io.IOException;
import java.util.Collections;

import org.keycloak.models.credential.OTPCredentialModel;
import org.keycloak.models.credential.PasswordCredentialModel;
import org.keycloak.models.credential.dto.OTPCredentialData;
import org.keycloak.models.credential.dto.PasswordCredentialData;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.util.JsonSerialization;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Regression tests for <a href="https://github.com/keycloak/keycloak/issues/41640">issue #41640</a>.
 *
 * <p>Verifies that {@link RepresentationToModel#convertDeprecatedCredentialsFormat} does <em>not</em> throw a
 * {@link NullPointerException} when nullable deprecated fields ({@code hashIterations}, {@code digits},
 * {@code counter}, {@code period}) are absent from the imported credential payload, and that
 * sensible defaults are applied instead.</p>
 *
 * <p>The deprecated fields on {@link CredentialRepresentation} have no public setters, so tests
 * use Jackson deserialization ({@link JsonSerialization#readValue}) to construct instances —
 * exactly the same code path used in Keycloak's real import pipeline.</p>
 */
public class RepresentationToModelDeprecatedCredentialsTest {

    // -------------------------------------------------------------------------
    // Password credential — hashIterations absent (null)
    // -------------------------------------------------------------------------

    /**
     * Regression for issue #41640.
     *
     * <p>Before the fix, passing a {@code CredentialRepresentation} whose {@code hashIterations}
     * field is {@code null} (absent from the JSON payload) caused Java auto-unboxing to throw:</p>
     * <pre>NullPointerException: Cannot invoke "java.lang.Integer.intValue()"
     *     because the return value of "CredentialRepresentation.getHashIterations()" is null</pre>
     *
     * <p>After the fix the call must succeed and default to {@code -1}, which is Keycloak's
     * established sentinel value meaning "use the provider-configured default".</p>
     */
    @Test
    public void convertDeprecatedPassword_nullHashIterations_doesNotThrowNPE() throws IOException {
        // Build credential from JSON the same way Keycloak's real import does — without hashIterations
        String json = "{\"type\":\"password\","
                + "\"hashedSaltedValue\":\"someHash\","
                + "\"salt\":\"someSalt\","
                + "\"algorithm\":\"pbkdf2-sha256\"}";
        CredentialRepresentation cred = JsonSerialization.readValue(json, CredentialRepresentation.class);
        UserRepresentation user = userWithCredential(cred);

        // Must not throw NullPointerException (regression guard)
        RepresentationToModel.convertDeprecatedCredentialsFormat(user);

        CredentialRepresentation converted = user.getCredentials().get(0);
        assertNotNull("credentialData must be populated after conversion", converted.getCredentialData());

        PasswordCredentialData data = JsonSerialization.readValue(
                converted.getCredentialData(), PasswordCredentialData.class);
        // -1 is Keycloak's sentinel meaning "use the provider-configured default"
        assertEquals("hashIterations should default to -1 when absent from the payload", -1, data.getHashIterations());
        assertEquals("algorithm should be preserved", "pbkdf2-sha256", data.getAlgorithm());
    }

    /**
     * When {@code hashIterations} is explicitly present it must be preserved exactly.
     */
    @Test
    public void convertDeprecatedPassword_withHashIterations_preservesValue() throws IOException {
        String json = "{\"type\":\"password\","
                + "\"hashedSaltedValue\":\"someHash\","
                + "\"salt\":\"someSalt\","
                + "\"algorithm\":\"pbkdf2-sha256\","
                + "\"hashIterations\":27500}";
        CredentialRepresentation cred = JsonSerialization.readValue(json, CredentialRepresentation.class);
        UserRepresentation user = userWithCredential(cred);

        RepresentationToModel.convertDeprecatedCredentialsFormat(user);

        PasswordCredentialData data = JsonSerialization.readValue(
                user.getCredentials().get(0).getCredentialData(), PasswordCredentialData.class);
        assertEquals("hashIterations should be preserved when present", 27500, data.getHashIterations());
    }

    // -------------------------------------------------------------------------
    // OTP credential — digits / counter / period all absent (null)
    // -------------------------------------------------------------------------

    /**
     * Regression for issue #41640 (OTP path).
     *
     * <p>A TOTP credential with no {@code digits}/{@code counter}/{@code period} fields caused the
     * same NPE pattern via auto-unboxing of {@code null Integer} → primitive {@code int}.
     * After the fix the call must succeed and standard RFC TOTP defaults must be applied.</p>
     */
    @Test
    public void convertDeprecatedTotp_nullDigitsCounterPeriod_doesNotThrowNPE() throws IOException {
        // Minimal TOTP payload — digits, counter, period intentionally omitted
        String json = "{\"type\":\"totp\",\"hashedSaltedValue\":\"secretValue\"}";
        CredentialRepresentation cred = JsonSerialization.readValue(json, CredentialRepresentation.class);
        UserRepresentation user = userWithCredential(cred);

        // Must not throw NullPointerException (regression guard)
        RepresentationToModel.convertDeprecatedCredentialsFormat(user);

        CredentialRepresentation converted = user.getCredentials().get(0);
        assertNotNull("credentialData must be populated after conversion", converted.getCredentialData());

        OTPCredentialData data = JsonSerialization.readValue(
                converted.getCredentialData(), OTPCredentialData.class);
        assertEquals("digits should default to 6",  6,  data.getDigits());
        assertEquals("counter should default to 0", 0,  data.getCounter());
        assertEquals("period should default to 30", 30, data.getPeriod());
        // type must be normalised to OTP
        assertEquals("type should be normalised to otp", OTPCredentialModel.TYPE,
                converted.getType());
    }

    /**
     * When OTP fields are explicitly provided they must be preserved exactly.
     */
    @Test
    public void convertDeprecatedTotp_withDigitsCounterPeriod_preservesValues() throws IOException {
        String json = "{\"type\":\"totp\","
                + "\"hashedSaltedValue\":\"secretValue\","
                + "\"digits\":8,"
                + "\"counter\":5,"
                + "\"period\":60}";
        CredentialRepresentation cred = JsonSerialization.readValue(json, CredentialRepresentation.class);
        UserRepresentation user = userWithCredential(cred);

        RepresentationToModel.convertDeprecatedCredentialsFormat(user);

        OTPCredentialData data = JsonSerialization.readValue(
                user.getCredentials().get(0).getCredentialData(), OTPCredentialData.class);
        assertEquals("digits should be preserved",  8,  data.getDigits());
        assertEquals("counter should be preserved", 5,  data.getCounter());
        assertEquals("period should be preserved",  60, data.getPeriod());
    }

    // -------------------------------------------------------------------------
    // Modern-format credential must pass through untouched
    // -------------------------------------------------------------------------

    /**
     * A credential that already carries {@code credentialData} + {@code secretData}
     * (the non-deprecated format) must be left completely untouched by the conversion.
     */
    @Test
    public void convertDeprecatedPassword_alreadyModernFormat_isUntouched() throws IOException {
        String json = "{\"type\":\"password\","
                + "\"credentialData\":\"{\\\"hashIterations\\\":27500,\\\"algorithm\\\":\\\"pbkdf2-sha256\\\"}\","
                + "\"secretData\":\"{\\\"value\\\":\\\"hash\\\",\\\"salt\\\":\\\"salt\\\"}\"}";
        CredentialRepresentation cred = JsonSerialization.readValue(json, CredentialRepresentation.class);
        String originalCredentialData = cred.getCredentialData();

        UserRepresentation user = userWithCredential(cred);
        RepresentationToModel.convertDeprecatedCredentialsFormat(user);

        assertEquals("modern-format credentialData must not be modified",
                originalCredentialData, user.getCredentials().get(0).getCredentialData());
    }

    // -------------------------------------------------------------------------
    // Helper
    // -------------------------------------------------------------------------

    private static UserRepresentation userWithCredential(CredentialRepresentation cred) {
        UserRepresentation user = new UserRepresentation();
        user.setUsername("testuser");
        user.setCredentials(Collections.singletonList(cred));
        return user;
    }
}
