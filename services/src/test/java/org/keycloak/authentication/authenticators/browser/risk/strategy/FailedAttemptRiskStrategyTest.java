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

package org.keycloak.authentication.authenticators.browser.risk.strategy;

import java.time.Instant;
import java.util.Set;

import org.junit.Test;
import org.keycloak.authentication.authenticators.browser.risk.decision.AdaptiveAuthPolicy;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

public class FailedAttemptRiskStrategyTest {

    private final FailedAttemptRiskStrategy strategy = new FailedAttemptRiskStrategy();

    @Test
    public void failedAttemptsProduceProportionalRisk() {
        assertThat(strategy.evaluate(context(0), AdaptiveAuthPolicy.defaults()).getRawScore(), equalTo(0));
        assertThat(strategy.evaluate(context(1), AdaptiveAuthPolicy.defaults()).getRawScore(), equalTo(33));
        assertThat(strategy.evaluate(context(3), AdaptiveAuthPolicy.defaults()).getRawScore(), equalTo(100));
        assertThat(strategy.evaluate(context(6), AdaptiveAuthPolicy.defaults()).getRawScore(), equalTo(100));
    }

    private static org.keycloak.authentication.authenticators.browser.risk.context.LoginContext context(int failures) {
        return LoginContextTestUtils.context(failures, "127.0.0.1", "ua", "en", null, Instant.EPOCH, Set.of(), Set.of(),
                Set.of(), false);
    }
}
