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

package org.keycloak.protocol.oidc.utils;

import java.util.Map;

import org.keycloak.models.Constants;

import org.junit.Assert;
import org.junit.Test;

/**
 * Whether an execution completed by an earlier authentication in the SSO session is reported by a later one.
 * The current time is passed explicitly, so the boundaries do not depend on the wall clock.
 */
public class AmrUtilsTest {

    private static final int COMPLETED_AT = 1_700_000_000;

    @Test
    public void zeroMaxAgeNeverCarriesOver() {
        Assert.assertFalse("same second", AmrUtils.carriesOver(config("0"), COMPLETED_AT, COMPLETED_AT));
        Assert.assertFalse("next second", AmrUtils.carriesOver(config("0"), COMPLETED_AT, COMPLETED_AT + 1));
    }

    @Test
    public void missingMaxAgeDefaultsToZero() {
        Assert.assertFalse(AmrUtils.carriesOver(Map.of(), COMPLETED_AT, COMPLETED_AT));
    }

    @Test
    public void positiveMaxAgeCarriesOverInclusive() {
        Assert.assertTrue("same second", AmrUtils.carriesOver(config("10"), COMPLETED_AT, COMPLETED_AT));
        Assert.assertTrue("at the max age", AmrUtils.carriesOver(config("10"), COMPLETED_AT, COMPLETED_AT + 10));
        Assert.assertFalse("past the max age", AmrUtils.carriesOver(config("10"), COMPLETED_AT, COMPLETED_AT + 11));
    }

    @Test
    public void invalidMaxAgeDoesNotCarryOver() {
        Assert.assertFalse(AmrUtils.carriesOver(config("soon"), COMPLETED_AT, COMPLETED_AT));
    }

    private static Map<String, String> config(String maxAge) {
        return Map.of(Constants.AUTHENTICATION_EXECUTION_REFERENCE_MAX_AGE, maxAge);
    }
}
