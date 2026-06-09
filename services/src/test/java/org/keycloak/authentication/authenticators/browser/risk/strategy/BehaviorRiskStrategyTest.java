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

public class BehaviorRiskStrategyTest {

    private final BehaviorRiskStrategy strategy = new BehaviorRiskStrategy();

    @Test
    public void unusualLoginHourGivesBehaviorRisk() {
        assertThat(strategy.evaluate(context("2026-06-09T02:00:00Z"), AdaptiveAuthPolicy.defaults()).getRawScore(),
                equalTo(50));
    }

    @Test
    public void normalLoginHourGivesLowRisk() {
        assertThat(strategy.evaluate(context("2026-06-09T12:00:00Z"), AdaptiveAuthPolicy.defaults()).getRawScore(),
                equalTo(0));
    }

    @Test
    public void wraparoundWindowIsHandled() {
        assertThat(BehaviorRiskStrategy.isInsideWindow(23, 22, 5), equalTo(true));
        assertThat(BehaviorRiskStrategy.isInsideWindow(4, 22, 5), equalTo(true));
        assertThat(BehaviorRiskStrategy.isInsideWindow(12, 22, 5), equalTo(false));
    }

    private static org.keycloak.authentication.authenticators.browser.risk.context.LoginContext context(String instant) {
        return LoginContextTestUtils.context(0, "127.0.0.1", "ua", "en", null, Instant.parse(instant), Set.of(),
                Set.of(), Set.of(), false);
    }
}
