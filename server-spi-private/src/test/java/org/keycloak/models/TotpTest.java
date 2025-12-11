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
package org.keycloak.models;

import java.nio.charset.StandardCharsets;
import java.util.Calendar;

import org.keycloak.models.credential.OTPCredentialModel;
import org.keycloak.models.credential.OTPCredentialModel.SecretEncoding;
import org.keycloak.models.utils.Base32;
import org.keycloak.models.utils.TimeBasedOTP;

import org.junit.Assert;
import org.junit.Test;

public class TotpTest {

    @Test
    public void testTotp() {

        TimeBasedOTP totp = new TimeBasedOTP("HmacSHA1", 8, 30, 1);
        String secret = "dSdmuHLQhkm54oIm0A0S";
        String otp = totp.generateTOTP(secret);

        Assert.assertTrue(totp.validateTOTP(otp, secret.getBytes(StandardCharsets.UTF_8)));
    }

    /**
     * KEYCLOAK-18880
     */
    @Test
    public void testTotpLookAround() {

        int lookAroundWindow = 2;
        TimeBasedOTP totp = new TimeBasedOTP("HmacSHA1", 8, 60, lookAroundWindow);
        String secret = "dSdmuHLQhkm54oIm0A0S";
        String otp = totp.generateTOTP(secret);

        for (int i = -lookAroundWindow; i <= lookAroundWindow; i++) {

            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.MINUTE, i);
            totp.setCalendar(calendar);

            Assert.assertTrue("Should accept code with skew offset " + i,totp.validateTOTP(otp, secret.getBytes(StandardCharsets.UTF_8)));
        }
    }

    @Test
    public void testBase32EncodedSecret() {
        TimeBasedOTP totp = new TimeBasedOTP("HmacSHA1", 8, 60, 1);
        String rawSecret = "JNSVMMTEKZCUGSKJIVGHMNSQOZBDA5JT";
        String otp = totp.generateTOTP(Base32.decode(rawSecret));
        OTPCredentialModel credentialModel = OTPCredentialModel.createTOTP(rawSecret, 8, 30, "HmacSHA1");

        Assert.assertFalse(totp.validateTOTP(otp, credentialModel.getDecodedSecret()));

        OTPCredentialModel encodedCredential = OTPCredentialModel.createTOTP(rawSecret, 8, 30, "HmacSHA1", SecretEncoding.BASE32.name());

        Assert.assertTrue(totp.validateTOTP(otp, encodedCredential.getDecodedSecret()));
    }

    @Test
    public void testBase32BinaryEncodedSecret() {
        TimeBasedOTP totp = new TimeBasedOTP("HmacSHA1", 8, 60, 1);
        String rawSecret = "CDLYAYRJ73ORTU4PUWWATWSYQCP4H2QL";
        String otp = totp.generateTOTP(Base32.decode(rawSecret));
        OTPCredentialModel credentialModel = OTPCredentialModel.createTOTP(rawSecret, 8, 30, "HmacSHA1");

        Assert.assertFalse(totp.validateTOTP(otp, credentialModel.getDecodedSecret()));

        OTPCredentialModel encodedCredential = OTPCredentialModel.createTOTP(rawSecret, 8, 30, "HmacSHA1", SecretEncoding.BASE32.name());

        Assert.assertTrue(totp.validateTOTP(otp, encodedCredential.getDecodedSecret()));
    }
}
