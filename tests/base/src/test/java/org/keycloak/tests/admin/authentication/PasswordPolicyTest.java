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

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ForbiddenException;

import org.keycloak.crypto.hash.Argon2PasswordHashProviderFactory;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.PasswordPolicy;
import org.keycloak.policy.AgePasswordPolicyProviderFactory;
import org.keycloak.policy.DenylistPasswordPolicyProviderFactory;
import org.keycloak.policy.DigitsPasswordPolicyProviderFactory;
import org.keycloak.policy.HistoryPasswordPolicyProviderFactory;
import org.keycloak.policy.LengthPasswordPolicyProviderFactory;
import org.keycloak.policy.LowerCasePasswordPolicyProviderFactory;
import org.keycloak.policy.MaxAuthAgePasswordPolicyProviderFactory;
import org.keycloak.policy.MaximumLengthPasswordPolicyProviderFactory;
import org.keycloak.policy.NotContainsUsernamePasswordPolicyProviderFactory;
import org.keycloak.policy.NotEmailPasswordPolicyProviderFactory;
import org.keycloak.policy.NotUsernamePasswordPolicyProviderFactory;
import org.keycloak.policy.RegexPatternsPasswordPolicyProviderFactory;
import org.keycloak.policy.SpecialCharsPasswordPolicyProviderFactory;
import org.keycloak.policy.UpperCasePasswordPolicyProviderFactory;
import org.keycloak.representations.idm.PasswordPolicyValueRepresentation;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.events.AdminEventAssertion;
import org.keycloak.tests.policy.PasswordPolicyTest.PasswordPolicyServerConfig;
import org.keycloak.tests.utils.admin.AdminEventPaths;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@KeycloakIntegrationTest(config = PasswordPolicyServerConfig.class)
public class PasswordPolicyTest extends AbstractPolicyTest {

    @AfterEach
    public void after() {
        authMgmtResource.updatePasswordPolicy(List.of());
        adminEvents.clear();
    }

    @Test
    public void testGetPasswordPolicyEmptyByDefault() {
        List<PasswordPolicyValueRepresentation> result = authMgmtResource.getPasswordPolicy();
        Assertions.assertEquals(0, result.size(), result.toString());
    }

    @Test
    public void testUpdatePasswordPolicy() {
        authMgmtResource.updatePasswordPolicy(List.of(
                new PasswordPolicyValueRepresentation(PasswordPolicy.PASSWORD_AGE,
                        AgePasswordPolicyProviderFactory.DEFAULT_AGE_DAYS)));

        List<PasswordPolicyValueRepresentation> result = authMgmtResource.getPasswordPolicy();
        Assertions.assertEquals(1, result.size());
        Assertions.assertEquals(PasswordPolicy.PASSWORD_AGE, result.get(0).getId());
    }

    @Test
    public void testUpdatePasswordPolicyOverwritesExistingPolicy() {
        authMgmtResource.updatePasswordPolicy(List.of(
                new PasswordPolicyValueRepresentation(PasswordPolicy.PASSWORD_AGE,
                        AgePasswordPolicyProviderFactory.DEFAULT_AGE_DAYS)
        ));

        // Overwrite with a completely different policy
        authMgmtResource.updatePasswordPolicy(List.of(
                new PasswordPolicyValueRepresentation(PasswordPolicy.FORCE_EXPIRED_ID, 365)
        ));

        List<PasswordPolicyValueRepresentation> result = authMgmtResource.getPasswordPolicy();
        Assertions.assertEquals(1, result.size());
        Assertions.assertEquals(PasswordPolicy.FORCE_EXPIRED_ID, result.get(0).getId());
    }

    @Test
    public void testUpdatePasswordPolicyWithEmptyListClearsPolicy() {
        authMgmtResource.updatePasswordPolicy(List.of(
                new PasswordPolicyValueRepresentation(PasswordPolicy.PASSWORD_AGE,
                        AgePasswordPolicyProviderFactory.DEFAULT_AGE_DAYS)));

        authMgmtResource.updatePasswordPolicy(List.of());

        List<PasswordPolicyValueRepresentation> result = authMgmtResource.getPasswordPolicy();
        Assertions.assertEquals(0, result.size());
    }

    @Test
    public void testUpdatePasswordPolicyWithAllSupportedPolicies() {
        List<PasswordPolicyValueRepresentation> allOptions = Arrays.asList(
                new PasswordPolicyValueRepresentation(DigitsPasswordPolicyProviderFactory.ID, 1),
                new PasswordPolicyValueRepresentation(PasswordPolicy.FORCE_EXPIRED_ID, 365),
                new PasswordPolicyValueRepresentation(PasswordPolicy.HASH_ALGORITHM_ID, Argon2PasswordHashProviderFactory.ID),
                new PasswordPolicyValueRepresentation(PasswordPolicy.HASH_ITERATIONS_ID, 10),
                new PasswordPolicyValueRepresentation(PasswordPolicy.PASSWORD_HISTORY_ID, HistoryPasswordPolicyProviderFactory.DEFAULT_VALUE),
                new PasswordPolicyValueRepresentation(LengthPasswordPolicyProviderFactory.ID, 8),
                new PasswordPolicyValueRepresentation(LowerCasePasswordPolicyProviderFactory.ID, 1),
                new PasswordPolicyValueRepresentation(MaximumLengthPasswordPolicyProviderFactory.ID, MaximumLengthPasswordPolicyProviderFactory.DEFAULT_MAX_LENGTH),
                new PasswordPolicyValueRepresentation(NotUsernamePasswordPolicyProviderFactory.ID, null),
                new PasswordPolicyValueRepresentation(NotContainsUsernamePasswordPolicyProviderFactory.ID, null),
                new PasswordPolicyValueRepresentation(RegexPatternsPasswordPolicyProviderFactory.ID, "password"),
                new PasswordPolicyValueRepresentation(SpecialCharsPasswordPolicyProviderFactory.ID, 1),
                new PasswordPolicyValueRepresentation(UpperCasePasswordPolicyProviderFactory.ID, 1),
                new PasswordPolicyValueRepresentation(DenylistPasswordPolicyProviderFactory.ID, "test-password-blacklist.txt"),
                new PasswordPolicyValueRepresentation(NotEmailPasswordPolicyProviderFactory.ID, null),
                new PasswordPolicyValueRepresentation(PasswordPolicy.RECOVERY_CODES_WARNING_THRESHOLD_ID, 4),
                new PasswordPolicyValueRepresentation(PasswordPolicy.MAX_AUTH_AGE_ID, MaxAuthAgePasswordPolicyProviderFactory.DEFAULT_MAX_AUTH_AGE),
                new PasswordPolicyValueRepresentation(PasswordPolicy.PASSWORD_AGE, AgePasswordPolicyProviderFactory.DEFAULT_AGE_DAYS)
        );

        authMgmtResource.updatePasswordPolicy(allOptions);

        List<PasswordPolicyValueRepresentation> result = authMgmtResource.getPasswordPolicy();
        Assertions.assertEquals(allOptions.size(), result.size());

        Set<String> resultIds = result.stream()
                .map(PasswordPolicyValueRepresentation::getId)
                .collect(Collectors.toSet());
        allOptions.forEach(opt -> Assertions.assertTrue(
                resultIds.contains(opt.getId()),
                "Missing policy: " + opt.getId()
        ));
    }

    @Test
    public void testUpdatePasswordPolicyFailsOnUnknownPolicyType() {
        Assertions.assertThrows(
                BadRequestException.class,
                () -> authMgmtResource.updatePasswordPolicy(List.of(
                        new PasswordPolicyValueRepresentation("nonExistentPolicy", 365)
                ))
        );
    }

    @Test
    public void testUpdatePasswordPolicyFailsOnMultipleUnknownPolicyTypes() {
        Assertions.assertThrows(
                BadRequestException.class,
                () -> authMgmtResource.updatePasswordPolicy(Arrays.asList(
                        new PasswordPolicyValueRepresentation("unknown1", 1),
                        new PasswordPolicyValueRepresentation("unknown2", 2))));
    }

    @Test
    public void testUpdatePasswordPolicyFailsWhenMixedValidAndInvalidEntries() {
        Assertions.assertThrows(
                BadRequestException.class,
                () -> authMgmtResource.updatePasswordPolicy(Arrays.asList(
                        new PasswordPolicyValueRepresentation(PasswordPolicy.PASSWORD_AGE,
                                AgePasswordPolicyProviderFactory.DEFAULT_AGE_DAYS),
                        new PasswordPolicyValueRepresentation("invalidPolicy", 999)
                ))
        );
    }

    @Test
    public void testUpdatePasswordPolicyFailsWhenIntPolicyReceivesString() {
        // FORCE_EXPIRED_ID expects an Integer
        Assertions.assertThrows(
                BadRequestException.class,
                () -> authMgmtResource.updatePasswordPolicy(List.of(
                        new PasswordPolicyValueRepresentation(PasswordPolicy.FORCE_EXPIRED_ID, "year")
                ))
        );
    }

    @Test
    public void testUpdatePasswordPolicyFailsWhenIntPolicyReceivesNull() {
        Assertions.assertThrows(
                BadRequestException.class,
                () -> authMgmtResource.updatePasswordPolicy(List.of(
                        new PasswordPolicyValueRepresentation(PasswordPolicy.FORCE_EXPIRED_ID, null)
                ))
        );
    }

    @Test
    public void testUpdatePasswordPolicyFailsWhenIntPolicyReceivesBoolean() {
        Assertions.assertThrows(
                BadRequestException.class,
                () -> authMgmtResource.updatePasswordPolicy(List.of(
                        new PasswordPolicyValueRepresentation(PasswordPolicy.HASH_ITERATIONS_ID, true))));
    }

    @Test
    public void testUpdatePasswordPolicyFailsWhenStringPolicyReceivesInteger() {
        // HASH_ALGORITHM_ID expects a String
        Assertions.assertThrows(
                BadRequestException.class,
                () -> authMgmtResource.updatePasswordPolicy(List.of(
                        new PasswordPolicyValueRepresentation(PasswordPolicy.HASH_ALGORITHM_ID, 123)
                ))
        );
    }

    @Test
    public void testUpdatePasswordPolicyFailsWhenStringPolicyReceivesNull() {
        Assertions.assertThrows(
                BadRequestException.class,
                () -> authMgmtResource.updatePasswordPolicy(List.of(
                        new PasswordPolicyValueRepresentation(PasswordPolicy.HASH_ALGORITHM_ID, null))));
    }

    @Test
    public void testUpdatePasswordPolicyAcceptsNullValueForOnOffPolicies() {
        authMgmtResource.updatePasswordPolicy(List.of(
                new PasswordPolicyValueRepresentation(NotUsernamePasswordPolicyProviderFactory.ID, null)));

        List<PasswordPolicyValueRepresentation> result = authMgmtResource.getPasswordPolicy();
        Assertions.assertEquals(1, result.size());
        Assertions.assertEquals(NotUsernamePasswordPolicyProviderFactory.ID, result.get(0).getId());
    }

    @Test
    public void testUpdatePasswordPolicyAcceptsIntegerParsableStringsInIntPolicies() {
        authMgmtResource.updatePasswordPolicy(List.of(
                new PasswordPolicyValueRepresentation(PasswordPolicy.FORCE_EXPIRED_ID, "365")));

        List<PasswordPolicyValueRepresentation> result = authMgmtResource.getPasswordPolicy();
        Assertions.assertEquals(1, result.size());
        Assertions.assertEquals(PasswordPolicy.FORCE_EXPIRED_ID, result.get(0).getId());
        Assertions.assertEquals(365, result.get(0).getValue());
    }

    @Test
    public void testUpdatePasswordPolicyApplyingSamePolicyTwiceIsIdempotent() {
        List<PasswordPolicyValueRepresentation> policy = List.of(
                new PasswordPolicyValueRepresentation(PasswordPolicy.PASSWORD_AGE,
                        AgePasswordPolicyProviderFactory.DEFAULT_AGE_DAYS)
        );

        authMgmtResource.updatePasswordPolicy(policy);
        authMgmtResource.updatePasswordPolicy(policy);

        List<PasswordPolicyValueRepresentation> result = authMgmtResource.getPasswordPolicy();
        Assertions.assertEquals(1, result.size());
        Assertions.assertEquals(PasswordPolicy.PASSWORD_AGE, result.get(0).getId());
    }

    @Test
    public void testGetPasswordPolicyRequiresViewRealmPermission() {
        Assertions.assertThrows(
                ForbiddenException.class,
                () -> clients.get("view-users").realm(managedRealm.getId()).flows().getPasswordPolicy()
        );
        Assertions.assertDoesNotThrow(
                () -> clients.get("view-realm").realm(managedRealm.getId()).flows().getPasswordPolicy()
        );
    }

    @Test
    public void testUpdatePasswordPolicyRequiresManageRealmPermission() {
        Assertions.assertThrows(
                ForbiddenException.class,
                () -> clients.get("view-realm").realm(managedRealm.getId()).flows().updatePasswordPolicy(List.of()));
        Assertions.assertDoesNotThrow(
                () -> clients.get("manage-realm").realm(managedRealm.getId()).flows().updatePasswordPolicy(List.of()));
    }

    @Test
    public void testUpdatePasswordPolicySuccessTriggersAdminEvent() {
        List<PasswordPolicyValueRepresentation> policies = List.of(
                new PasswordPolicyValueRepresentation(PasswordPolicy.PASSWORD_AGE,
                        AgePasswordPolicyProviderFactory.DEFAULT_AGE_DAYS));

        authMgmtResource.updatePasswordPolicy(policies);

        AdminEventAssertion.assertSuccess(adminEvents.poll())
                .operationType(OperationType.UPDATE)
                .resourceType(ResourceType.AUTHENTICATION_POLICY)
                .resourcePath(AdminEventPaths.passwordPolicyPath())
                .representation(policies);
    }

    @Test
    public void testUpdatePasswordPolicyFailureDoesNotTriggerAdminEvent() {
        Assertions.assertThrows(
                BadRequestException.class,
                () -> authMgmtResource.updatePasswordPolicy(List.of(
                        new PasswordPolicyValueRepresentation("nonExistentPolicy", 365)
                ))
        );

        Assertions.assertNull(adminEvents.poll());
    }
}
