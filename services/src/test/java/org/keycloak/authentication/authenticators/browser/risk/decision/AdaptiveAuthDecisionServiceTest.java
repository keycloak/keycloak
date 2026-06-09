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

import java.util.List;

import org.junit.Test;
import org.keycloak.authentication.authenticators.browser.risk.scoring.RiskLevel;
import org.keycloak.authentication.authenticators.browser.risk.scoring.RiskScore;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

public class AdaptiveAuthDecisionServiceTest {

    private final AdaptiveAuthDecisionService service = new AdaptiveAuthDecisionService();
    private final AdaptiveAuthPolicy policy = AdaptiveAuthPolicy.defaults();

    @Test
    public void scoreBelowLowThresholdAllows() {
        assertThat(service.decide(score(29), policy), equalTo(AuthAction.ALLOW));
    }

    @Test
    public void scoreBetweenThresholdsStepsUp() {
        assertThat(service.decide(score(30), policy), equalTo(AuthAction.STEP_UP_MFA));
    }

    @Test
    public void scoreAboveHighThresholdBlocks() {
        assertThat(service.decide(score(70), policy), equalTo(AuthAction.BLOCK));
    }

    private static RiskScore score(int value) {
        return new RiskScore(value, RiskLevel.LOW, List.of());
    }
}
