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

import org.keycloak.common.util.Time;
import org.keycloak.models.Constants;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

/**
 * Whether an execution completed by an earlier authentication in the SSO session is reported by a later one.
 */
public class AmrUtilsTest {

    @After
    public void resetTime() {
        Time.setOffset(0);
    }

    @Test
    public void zeroMaxAgeNeverCarriesOver() {
        int completedAt = Time.currentTime();
        Assert.assertFalse("same second", AmrUtils.carriesOver(config("0"), completedAt));
        Time.setOffset(1);
        Assert.assertFalse("next second", AmrUtils.carriesOver(config("0"), completedAt));
    }

    @Test
    public void missingMaxAgeDefaultsToZero() {
        Assert.assertFalse(AmrUtils.carriesOver(Map.of(), Time.currentTime()));
    }

    @Test
    public void positiveMaxAgeCarriesOverInclusive() {
        int completedAt = Time.currentTime();
        Assert.assertTrue("same second", AmrUtils.carriesOver(config("10"), completedAt));
        Time.setOffset(10);
        Assert.assertTrue("at the max age", AmrUtils.carriesOver(config("10"), completedAt));
        Time.setOffset(11);
        Assert.assertFalse("past the max age", AmrUtils.carriesOver(config("10"), completedAt));
    }

    @Test
    public void invalidMaxAgeDoesNotCarryOver() {
        Assert.assertFalse(AmrUtils.carriesOver(config("soon"), Time.currentTime()));
    }

    private static Map<String, String> config(String maxAge) {
        return Map.of(Constants.AUTHENTICATION_EXECUTION_REFERENCE_MAX_AGE, maxAge);
    }
}
