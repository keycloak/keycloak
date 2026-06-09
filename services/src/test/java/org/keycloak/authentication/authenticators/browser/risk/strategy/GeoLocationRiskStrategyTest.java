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

public class GeoLocationRiskStrategyTest {

    private final GeoLocationRiskStrategy strategy = new GeoLocationRiskStrategy();

    @Test
    public void missingGeoSignalGivesNeutralRisk() {
        assertThat(strategy.evaluate(context(null, Set.of("US"), true), AdaptiveAuthPolicy.defaults()).getRawScore(),
                equalTo(0));
    }

    @Test
    public void sameGeoSignalGivesLowRisk() {
        assertThat(strategy.evaluate(context("us", Set.of("US"), true), AdaptiveAuthPolicy.defaults()).getRawScore(),
                equalTo(0));
    }

    @Test
    public void changedGeoSignalGivesConfiguredRisk() {
        assertThat(strategy.evaluate(context("CA", Set.of("US"), true), AdaptiveAuthPolicy.defaults()).getRawScore(),
                equalTo(50));
    }

    @Test
    public void missingHistoryReturnsNeutralRisk() {
        assertThat(strategy.evaluate(context("US", Set.of(), false), AdaptiveAuthPolicy.defaults()).getRawScore(),
                equalTo(10));
        assertThat(strategy.evaluate(context("US", Set.of(), true), AdaptiveAuthPolicy.defaults()).getRawScore(),
                equalTo(10));
    }

    private static org.keycloak.authentication.authenticators.browser.risk.context.LoginContext context(String geo,
            Set<String> geos, boolean historyAvailable) {
        return LoginContextTestUtils.context(0, "127.0.0.1", "ua", "en", geo, Instant.EPOCH, Set.of(), Set.of(),
                geos, historyAvailable);
    }
}
