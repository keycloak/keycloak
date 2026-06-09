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

package org.keycloak.authentication.authenticators.browser.risk.decision;

import java.util.Map;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

public class AdaptiveAuthPolicyTest {

    @Test
    public void defaultConfigLoadsCorrectly() {
        AdaptiveAuthPolicy policy = AdaptiveAuthPolicy.defaults();

        assertThat(policy.getLowRiskThreshold(), equalTo(30));
        assertThat(policy.getHighRiskThreshold(), equalTo(70));
        assertThat(policy.getFailedAttemptsWeight(), equalTo(35));
        assertThat(policy.getHistoryLookbackLimit(), equalTo(10));
    }

    @Test
    public void customConfigLoadsCorrectly() {
        AdaptiveAuthPolicy policy = AdaptiveAuthPolicy.fromConfig(Map.of(
                AdaptiveAuthPolicy.LOW_RISK_THRESHOLD, "20",
                AdaptiveAuthPolicy.HIGH_RISK_THRESHOLD, "80",
                AdaptiveAuthPolicy.NEW_IP_RISK_SCORE, "75",
                AdaptiveAuthPolicy.UNUSUAL_LOGIN_START_HOUR, "22",
                AdaptiveAuthPolicy.UNUSUAL_LOGIN_END_HOUR, "6"));

        assertThat(policy.getLowRiskThreshold(), equalTo(20));
        assertThat(policy.getHighRiskThreshold(), equalTo(80));
        assertThat(policy.getNewIpRiskScore(), equalTo(75));
        assertThat(policy.getUnusualLoginStartHour(), equalTo(22));
        assertThat(policy.getUnusualLoginEndHour(), equalTo(6));
    }

    @Test
    public void invalidConfigFallsBackSafely() {
        AdaptiveAuthPolicy policy = AdaptiveAuthPolicy.fromConfig(Map.of(
                AdaptiveAuthPolicy.LOW_RISK_THRESHOLD, "90",
                AdaptiveAuthPolicy.HIGH_RISK_THRESHOLD, "10",
                AdaptiveAuthPolicy.FAILED_ATTEMPTS_WEIGHT, "-10",
                AdaptiveAuthPolicy.HISTORY_LOOKBACK_LIMIT, "not-a-number"));

        assertThat(policy.getLowRiskThreshold(), equalTo(30));
        assertThat(policy.getHighRiskThreshold(), equalTo(70));
        assertThat(policy.getFailedAttemptsWeight(), equalTo(35));
        assertThat(policy.getHistoryLookbackLimit(), equalTo(10));
    }
}
