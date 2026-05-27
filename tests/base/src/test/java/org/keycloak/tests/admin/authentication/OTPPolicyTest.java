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

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ForbiddenException;

import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.OTPPolicy;
import org.keycloak.representations.idm.OTPPolicyRepresentation;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.events.AdminEventAssertion;
import org.keycloak.tests.utils.admin.AdminEventPaths;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@KeycloakIntegrationTest
public class OTPPolicyTest extends AbstractPolicyTest {

    @Test
    public void testGetOTPPolicyReturnsAllFields() {
        OTPPolicyRepresentation result = authMgmtResource.getOTPPolicy();

        Assertions.assertNotNull(result);
        Assertions.assertNotNull(result.getType());
        Assertions.assertNotNull(result.getAlgorithm());
        Assertions.assertNotNull(result.getDigits());
        Assertions.assertNotNull(result.getPeriod());
        Assertions.assertNotNull(result.getInitialCounter());
        Assertions.assertNotNull(result.getLookAheadWindow());
        Assertions.assertNotNull(result.isCodeReusable());
    }

    @Test
    public void testGetOTPPolicyReturnsDefaultPolicy() {
        OTPPolicyRepresentation result = authMgmtResource.getOTPPolicy();

        Assertions.assertNotNull(result);
        Assertions.assertEquals(OTPPolicy.DEFAULT_POLICY.getType(), result.getType());
        Assertions.assertEquals(OTPPolicy.DEFAULT_POLICY.getAlgorithm(), result.getAlgorithm());
        Assertions.assertEquals(OTPPolicy.DEFAULT_POLICY.getDigits(), result.getDigits());
        Assertions.assertEquals(OTPPolicy.DEFAULT_POLICY.getPeriod(), result.getPeriod());
        Assertions.assertEquals(OTPPolicy.DEFAULT_POLICY.getInitialCounter(), result.getInitialCounter());
        Assertions.assertEquals(OTPPolicy.DEFAULT_POLICY.getLookAheadWindow(), result.getLookAheadWindow());
        Assertions.assertEquals(OTPPolicy.DEFAULT_POLICY.isCodeReusable(), result.isCodeReusable());
    }

    @Test
    public void testUpdateOTPPolicyTotpHmacSHA1With6digits() {
        OTPPolicyRepresentation rep = validTotpPolicy();
        rep.setAlgorithm("HmacSHA1");
        rep.setDigits(6);
        authMgmtResource.updateOTPPolicy(rep);

        OTPPolicyRepresentation result = authMgmtResource.getOTPPolicy();
        Assertions.assertEquals("totp", result.getType());
        Assertions.assertEquals("HmacSHA1", result.getAlgorithm());
        Assertions.assertEquals(6, result.getDigits());
    }

    @Test
    public void testUpdateOTPPolicyTotpHmacSHA256With8digits() {
        OTPPolicyRepresentation rep = validTotpPolicy();
        rep.setAlgorithm("HmacSHA256");
        rep.setDigits(8);
        authMgmtResource.updateOTPPolicy(rep);

        OTPPolicyRepresentation result = authMgmtResource.getOTPPolicy();
        Assertions.assertEquals("totp", result.getType());
        Assertions.assertEquals("HmacSHA256", result.getAlgorithm());
        Assertions.assertEquals(8, result.getDigits());
    }

    @Test
    public void testUpdateOTPPolicyTotpHmacSHA512() {
        OTPPolicyRepresentation rep = validTotpPolicy();
        rep.setAlgorithm("HmacSHA512");
        authMgmtResource.updateOTPPolicy(rep);

        OTPPolicyRepresentation result = authMgmtResource.getOTPPolicy();
        Assertions.assertEquals("HmacSHA512", result.getAlgorithm());
    }

    @Test
    public void testUpdateOTPPolicyHotp() {
        OTPPolicyRepresentation rep = validHotpPolicy();
        authMgmtResource.updateOTPPolicy(rep);

        OTPPolicyRepresentation result = authMgmtResource.getOTPPolicy();
        Assertions.assertEquals("hotp", result.getType());
        Assertions.assertEquals("HmacSHA256", result.getAlgorithm());
        Assertions.assertEquals(8, result.getDigits());
    }

    @Test
    public void testUpdateOTPPolicyPersistsAllFields() {
        OTPPolicyRepresentation rep = validTotpPolicy();
        rep.setPeriod(60);
        rep.setInitialCounter(10);
        rep.setLookAheadWindow(5);
        rep.setCodeReusable(true);
        authMgmtResource.updateOTPPolicy(rep);

        OTPPolicyRepresentation result = authMgmtResource.getOTPPolicy();
        Assertions.assertEquals(5, result.getLookAheadWindow());
        Assertions.assertEquals(60, result.getPeriod());
        Assertions.assertEquals(10, result.getInitialCounter());
        Assertions.assertTrue(result.isCodeReusable());
    }

    @Test
    public void testUpdateOTPPolicyOverwritesPreviousPolicy() {
        authMgmtResource.updateOTPPolicy(validTotpPolicy());
        authMgmtResource.updateOTPPolicy(validHotpPolicy());

        OTPPolicyRepresentation result = authMgmtResource.getOTPPolicy();
        Assertions.assertEquals("hotp", result.getType());
    }

    @Test
    public void testUpdateOTPPolicyIsIdempotent() {
        OTPPolicyRepresentation rep = validTotpPolicy();
        authMgmtResource.updateOTPPolicy(rep);
        authMgmtResource.updateOTPPolicy(rep);

        OTPPolicyRepresentation result = authMgmtResource.getOTPPolicy();
        Assertions.assertEquals("totp", result.getType());
        Assertions.assertEquals("HmacSHA1", result.getAlgorithm());
        Assertions.assertEquals(6, result.getDigits());
    }

    @Test
    public void testUpdateOTPPolicyFailsOnUnsupportedType() {
        OTPPolicyRepresentation rep = validTotpPolicy();
        rep.setType("unsupportedType");

        Assertions.assertThrows(
                BadRequestException.class,
                () -> authMgmtResource.updateOTPPolicy(rep)
        );
    }

    @Test
    public void testUpdateOTPPolicyFailsOnNullType() {
        OTPPolicyRepresentation rep = validTotpPolicy();
        rep.setType(null);

        Assertions.assertThrows(
                BadRequestException.class,
                () -> authMgmtResource.updateOTPPolicy(rep)
        );
    }

    @Test
    public void testUpdateOTPPolicyFailsOnEmptyType() {
        OTPPolicyRepresentation rep = validTotpPolicy();
        rep.setType("");

        Assertions.assertThrows(
                BadRequestException.class,
                () -> authMgmtResource.updateOTPPolicy(rep)
        );
    }

    @Test
    public void testUpdateOTPPolicyFailsOnCaseSensitiveType() {
        OTPPolicyRepresentation rep = validTotpPolicy();
        rep.setType("TOTP");

        Assertions.assertThrows(
                BadRequestException.class,
                () -> authMgmtResource.updateOTPPolicy(rep)
        );
    }

    @Test
    public void testUpdateOTPPolicyFailsOnUnsupportedAlgorithm() {
        OTPPolicyRepresentation rep = validTotpPolicy();
        rep.setAlgorithm("HmacMD5");

        Assertions.assertThrows(
                BadRequestException.class,
                () -> authMgmtResource.updateOTPPolicy(rep)
        );
    }

    @Test
    public void testUpdateOTPPolicyFailsOnNullAlgorithm() {
        OTPPolicyRepresentation rep = validTotpPolicy();
        rep.setAlgorithm(null);

        Assertions.assertThrows(
                BadRequestException.class,
                () -> authMgmtResource.updateOTPPolicy(rep)
        );
    }

    @Test
    public void testUpdateOTPPolicyFailsOnEmptyAlgorithm() {
        OTPPolicyRepresentation rep = validTotpPolicy();
        rep.setAlgorithm("");

        Assertions.assertThrows(
                BadRequestException.class,
                () -> authMgmtResource.updateOTPPolicy(rep)
        );
    }

    @Test
    public void testUpdateOTPPolicyFailsOnCaseSensitiveAlgorithm() {
        OTPPolicyRepresentation rep = validTotpPolicy();
        rep.setAlgorithm("hmacsha1");

        Assertions.assertThrows(
                BadRequestException.class,
                () -> authMgmtResource.updateOTPPolicy(rep));
    }

    @Test
    public void testUpdateOTPPolicyFailsOnUnsupportedDigits() {
        OTPPolicyRepresentation rep = validTotpPolicy();
        rep.setDigits(7);

        Assertions.assertThrows(
                BadRequestException.class,
                () -> authMgmtResource.updateOTPPolicy(rep)
        );
    }

    @Test
    public void testUpdateOTPPolicyFailsOnZeroDigits() {
        OTPPolicyRepresentation rep = validTotpPolicy();
        rep.setDigits(0);

        Assertions.assertThrows(
                BadRequestException.class,
                () -> authMgmtResource.updateOTPPolicy(rep)
        );
    }

    @Test
    public void testUpdateOTPPolicyFailsOnNegativeDigits() {
        OTPPolicyRepresentation rep = validTotpPolicy();
        rep.setDigits(-1);

        Assertions.assertThrows(
                BadRequestException.class,
                () -> authMgmtResource.updateOTPPolicy(rep)
        );
    }

    @Test
    public void testUpdateOTPPolicyFailsOnMissingTotpPeriod() {
        OTPPolicyRepresentation rep = validTotpPolicy();
        rep.setPeriod(null);

        Assertions.assertThrows(
                BadRequestException.class,
                () -> authMgmtResource.updateOTPPolicy(rep)
        );
    }

    @Test
    public void testUpdateOTPPolicyFailsOnMissingHotpInitialCounter() {
        OTPPolicyRepresentation rep = validHotpPolicy();
        rep.setInitialCounter(null);

        Assertions.assertThrows(
                BadRequestException.class,
                () -> authMgmtResource.updateOTPPolicy(rep)
        );
    }

    @Test
    public void testUpdateOTPPolicyFailsWhenMultipleFieldsAreInvalid() {
        OTPPolicyRepresentation rep = validTotpPolicy();
        rep.setType("invalidType");
        rep.setAlgorithm("invalidAlgorithm");
        rep.setDigits(5);

        Assertions.assertThrows(
                BadRequestException.class,
                () -> authMgmtResource.updateOTPPolicy(rep)
        );
    }

    @Test
    public void testGetOTPPolicyRequiresViewRealmPermission() {
        Assertions.assertThrows(
                ForbiddenException.class,
                () -> clients.get("view-users").realm(managedRealm.getId()).flows().getOTPPolicy()
        );
        Assertions.assertDoesNotThrow(
                () -> clients.get("view-realm").realm(managedRealm.getId()).flows().getOTPPolicy()
        );
    }

    @Test
    public void testUpdateOTPPolicyRequiresManageRealmPermission() {
        Assertions.assertThrows(
                ForbiddenException.class,
                () -> clients.get("view-realm").realm(managedRealm.getId()).flows().updateOTPPolicy(validTotpPolicy())
        );
        Assertions.assertDoesNotThrow(
                () -> clients.get("manage-realm").realm(managedRealm.getId()).flows().updateOTPPolicy(validTotpPolicy())
        );
    }

    @Test
    public void testUpdateOTPPolicySuccessTriggersAdminEvent() {
        authMgmtResource.updateOTPPolicy(validHotpPolicy());

        AdminEventAssertion.assertSuccess(adminEvents.poll())
                .operationType(OperationType.UPDATE)
                .resourceType(ResourceType.AUTHENTICATION_POLICY)
                .resourcePath(AdminEventPaths.otpPolicyPath())
                .representation(validHotpPolicy());
    }

    @Test
    public void testUpdateOTPPolicyFailureDoesNotTriggerAdminEvent() {
        OTPPolicyRepresentation rep = new OTPPolicyRepresentation();

        Assertions.assertThrows(
                BadRequestException.class,
                () -> authMgmtResource.updateOTPPolicy(rep)
        );

        Assertions.assertNull(adminEvents.poll());
    }

    private OTPPolicyRepresentation validTotpPolicy() {
        OTPPolicyRepresentation rep = new OTPPolicyRepresentation();
        rep.setType("totp");
        rep.setAlgorithm("HmacSHA1");
        rep.setDigits(6);
        rep.setLookAheadWindow(1);
        rep.setPeriod(30);
        rep.setInitialCounter(0);
        rep.setCodeReusable(false);
        return rep;
    }

    private OTPPolicyRepresentation validHotpPolicy() {
        OTPPolicyRepresentation rep = new OTPPolicyRepresentation();
        rep.setType("hotp");
        rep.setAlgorithm("HmacSHA256");
        rep.setDigits(8);
        rep.setLookAheadWindow(2);
        rep.setPeriod(60);
        rep.setInitialCounter(1);
        rep.setCodeReusable(true);
        return rep;
    }
}
