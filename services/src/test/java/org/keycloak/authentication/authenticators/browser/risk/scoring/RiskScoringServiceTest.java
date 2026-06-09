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

package org.keycloak.authentication.authenticators.browser.risk.scoring;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;
import org.keycloak.authentication.authenticators.browser.risk.context.LoginContext;
import org.keycloak.authentication.authenticators.browser.risk.decision.AdaptiveAuthPolicy;
import org.keycloak.authentication.authenticators.browser.risk.strategy.RiskStrategy;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

public class RiskScoringServiceTest {

    private final LoginContext context = new LoginContext("realm", "user", "alice", "127.0.0.1", "ua", "en", null,
            Instant.EPOCH, 0, Set.of(), Set.of(), Set.of(), false);

    @Test
    public void weightedScoresAggregateCorrectly() {
        RiskScoringService service = new RiskScoringService(List.of(
                fixed("failed_attempts", 100),
                fixed("ip", 50),
                fixed("device", 25),
                fixed("behavior", 0),
                fixed("geo", 10)));

        RiskScore score = service.score(context, AdaptiveAuthPolicy.defaults());

        assertThat(score.getValue(), equalTo(51));
        assertThat(score.getLevel(), equalTo(RiskLevel.MEDIUM));
        assertThat(score.getFactors().get(0).getWeightedScore(), equalTo(35));
        assertThat(score.getFactors().get(4).getRawScore(), equalTo(10));
    }

    @Test
    public void finalScoreClampsTo100() {
        RiskScoringService service = new RiskScoringService(List.of(
                fixed("failed_attempts", 100),
                fixed("ip", 100),
                fixed("device", 100),
                fixed("behavior", 100),
                fixed("geo", 100)));

        RiskScore score = service.score(context, AdaptiveAuthPolicy.fromConfig(Map.of(
                AdaptiveAuthPolicy.FAILED_ATTEMPTS_WEIGHT, "100",
                AdaptiveAuthPolicy.IP_RISK_WEIGHT, "100",
                AdaptiveAuthPolicy.DEVICE_RISK_WEIGHT, "100",
                AdaptiveAuthPolicy.BEHAVIOR_RISK_WEIGHT, "100",
                AdaptiveAuthPolicy.GEO_RISK_WEIGHT, "100")));

        assertThat(score.getValue(), equalTo(100));
        assertThat(score.getLevel(), equalTo(RiskLevel.HIGH));
    }

    @Test
    public void scoreNeverGoesBelowZeroAndKeepsFactorBreakdown() {
        RiskScoringService service = new RiskScoringService(List.of(fixed("unknown", -10)));

        RiskScore score = service.score(context, AdaptiveAuthPolicy.defaults());

        assertThat(score.getValue(), equalTo(0));
        assertThat(score.getLevel(), equalTo(RiskLevel.LOW));
        assertThat(score.getFactors().size(), equalTo(1));
        assertThat(score.getFactors().get(0).getRawScore(), equalTo(0));
    }

    private static RiskStrategy fixed(String name, int rawScore) {
        return new RiskStrategy() {
            @Override
            public String getName() {
                return name;
            }

            @Override
            public RiskFactorResult evaluate(LoginContext loginContext, AdaptiveAuthPolicy policy) {
                return RiskFactorResult.raw(name, rawScore, Map.of("fixed", "true"));
            }
        };
    }
}
