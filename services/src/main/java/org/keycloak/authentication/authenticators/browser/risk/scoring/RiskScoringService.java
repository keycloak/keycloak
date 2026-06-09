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

import java.util.ArrayList;
import java.util.List;

import org.keycloak.authentication.authenticators.browser.risk.context.LoginContext;
import org.keycloak.authentication.authenticators.browser.risk.decision.AdaptiveAuthPolicy;
import org.keycloak.authentication.authenticators.browser.risk.strategy.BehaviorRiskStrategy;
import org.keycloak.authentication.authenticators.browser.risk.strategy.DeviceRiskStrategy;
import org.keycloak.authentication.authenticators.browser.risk.strategy.FailedAttemptRiskStrategy;
import org.keycloak.authentication.authenticators.browser.risk.strategy.GeoLocationRiskStrategy;
import org.keycloak.authentication.authenticators.browser.risk.strategy.IPRiskStrategy;
import org.keycloak.authentication.authenticators.browser.risk.strategy.RiskStrategy;

public class RiskScoringService {

    private final List<RiskStrategy> strategies;

    public RiskScoringService() {
        this(List.of(
                new FailedAttemptRiskStrategy(),
                new IPRiskStrategy(),
                new DeviceRiskStrategy(),
                new BehaviorRiskStrategy(),
                new GeoLocationRiskStrategy()));
    }

    public RiskScoringService(List<RiskStrategy> strategies) {
        this.strategies = List.copyOf(strategies);
    }

    public RiskScore score(LoginContext loginContext, AdaptiveAuthPolicy policy) {
        List<RiskFactorResult> factors = new ArrayList<>();
        int total = 0;

        for (RiskStrategy strategy : strategies) {
            RiskFactorResult result = strategy.evaluate(loginContext, policy);
            int weighted = (result.getRawScore() * weightFor(strategy.getName(), policy)) / 100;
            RiskFactorResult weightedResult = result.withWeightedScore(weighted);
            factors.add(weightedResult);
            total += weighted;
        }

        int value = Math.max(0, Math.min(100, total));
        return new RiskScore(value, levelFor(value, policy), factors);
    }

    private static int weightFor(String factor, AdaptiveAuthPolicy policy) {
        return switch (factor) {
            case FailedAttemptRiskStrategy.NAME -> policy.getFailedAttemptsWeight();
            case IPRiskStrategy.NAME -> policy.getIpRiskWeight();
            case DeviceRiskStrategy.NAME -> policy.getDeviceRiskWeight();
            case BehaviorRiskStrategy.NAME -> policy.getBehaviorRiskWeight();
            case GeoLocationRiskStrategy.NAME -> policy.getGeoRiskWeight();
            default -> 0;
        };
    }

    private static RiskLevel levelFor(int value, AdaptiveAuthPolicy policy) {
        if (value >= policy.getHighRiskThreshold()) {
            return RiskLevel.HIGH;
        }
        if (value >= policy.getLowRiskThreshold()) {
            return RiskLevel.MEDIUM;
        }
        return RiskLevel.LOW;
    }
}
