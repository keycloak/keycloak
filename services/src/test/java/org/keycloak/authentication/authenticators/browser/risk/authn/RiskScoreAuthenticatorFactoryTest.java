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

package org.keycloak.authentication.authenticators.browser.risk.authn;

import java.util.stream.Collectors;

import org.junit.Test;
import org.keycloak.authentication.authenticators.browser.risk.decision.AdaptiveAuthPolicy;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;

public class RiskScoreAuthenticatorFactoryTest {

    @Test
    public void exposesExpectedConfigurationProperties() {
        RiskScoreAuthenticatorFactory factory = new RiskScoreAuthenticatorFactory();

        assertThat(factory.getId(), equalTo("risk-score-authenticator"));
        assertThat(factory.isConfigurable(), equalTo(true));
        assertThat(factory.getConfigProperties().stream().map(property -> property.getName()).collect(Collectors.toList()),
                contains(
                        AdaptiveAuthPolicy.LOW_RISK_THRESHOLD,
                        AdaptiveAuthPolicy.HIGH_RISK_THRESHOLD,
                        AdaptiveAuthPolicy.FAILED_ATTEMPTS_WEIGHT,
                        AdaptiveAuthPolicy.IP_RISK_WEIGHT,
                        AdaptiveAuthPolicy.DEVICE_RISK_WEIGHT,
                        AdaptiveAuthPolicy.BEHAVIOR_RISK_WEIGHT,
                        AdaptiveAuthPolicy.GEO_RISK_WEIGHT,
                        AdaptiveAuthPolicy.FAILED_ATTEMPTS_THRESHOLD,
                        AdaptiveAuthPolicy.NEW_IP_RISK_SCORE,
                        AdaptiveAuthPolicy.NEW_DEVICE_RISK_SCORE,
                        AdaptiveAuthPolicy.UNUSUAL_LOGIN_START_HOUR,
                        AdaptiveAuthPolicy.UNUSUAL_LOGIN_END_HOUR,
                        AdaptiveAuthPolicy.UNUSUAL_LOGIN_RISK_SCORE,
                        AdaptiveAuthPolicy.NEW_GEO_RISK_SCORE,
                        AdaptiveAuthPolicy.HISTORY_LOOKBACK_LIMIT));
    }
}
