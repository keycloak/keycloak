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

package org.keycloak.ipatuura_user_spi;

import java.util.Collections;
import java.util.Map;

import org.keycloak.models.UserModel;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class IpatuuraUserStorageProviderTest {

    @Test
    public void matchesEnabledFilter() {
        assertTrue(matches(Map.of(UserModel.ENABLED, "true"), true, false, 1000L));
        assertFalse(matches(Map.of(UserModel.ENABLED, "true"), false, false, 1000L));
        assertTrue(matches(Map.of(UserModel.ENABLED, "false"), false, false, 1000L));
    }

    @Test
    public void matchesEmailVerifiedFilter() {
        assertTrue(matches(Map.of(UserModel.EMAIL_VERIFIED, "true"), true, true, 1000L));
        assertFalse(matches(Map.of(UserModel.EMAIL_VERIFIED, "true"), true, false, 1000L));
        assertTrue(matches(Map.of(UserModel.EMAIL_VERIFIED, "false"), true, false, 1000L));
    }

    @Test
    public void matchesInclusiveCreatedTimestampFilters() {
        assertTrue(matches(Map.of(UserModel.CREATED_AFTER, "1000"), true, false, 1000L));
        assertFalse(matches(Map.of(UserModel.CREATED_AFTER, "1001"), true, false, 1000L));
        assertTrue(matches(Map.of(UserModel.CREATED_BEFORE, "1000"), true, false, 1000L));
        assertFalse(matches(Map.of(UserModel.CREATED_BEFORE, "999"), true, false, 1000L));
    }

    @Test
    public void doesNotMatchCreatedTimestampFiltersWithoutTimestamp() {
        assertFalse(matches(Map.of(UserModel.CREATED_AFTER, "1000"), true, false, null));
        assertFalse(matches(Map.of(UserModel.CREATED_BEFORE, "1000"), true, false, null));
    }

    @Test
    public void combinesStandardFiltersWithLogicalAnd() {
        Map<String, String> filters = Map.of(
                UserModel.ENABLED, "true",
                UserModel.EMAIL_VERIFIED, "false",
                UserModel.CREATED_AFTER, "1000",
                UserModel.CREATED_BEFORE, "2000");

        assertTrue(matches(filters, true, false, 1500L));
        assertFalse(matches(filters, false, false, 1500L));
        assertFalse(matches(filters, true, true, 1500L));
        assertFalse(matches(filters, true, false, 999L));
        assertFalse(matches(filters, true, false, 2001L));
    }

    @Test
    public void matchesWhenNoStandardFiltersAreSupplied() {
        assertTrue(matches(Collections.emptyMap(), true, false, null));
    }

    @Test
    public void rejectsUnsupportedCustomAttributes() {
        assertFalse(IpatuuraUserStorageProvider.supportsSearchParameters(Map.of(
                UserModel.SEARCH, "alice",
                "department", "IT")));
        assertTrue(IpatuuraUserStorageProvider.supportsSearchParameters(Map.of(
                UserModel.SEARCH, "alice",
                UserModel.INCLUDE_SERVICE_ACCOUNT, "false",
                UserModel.ENABLED, "true",
                UserModel.EMAIL_VERIFIED, "false",
                UserModel.CREATED_AFTER, "1000",
                UserModel.CREATED_BEFORE, "2000")));
    }

    private static boolean matches(Map<String, String> params, boolean enabled, boolean emailVerified,
            Long createdTimestamp) {
        return IpatuuraUserStorageProvider.matchesStandardFilters(params, enabled, emailVerified, createdTimestamp);
    }
}
