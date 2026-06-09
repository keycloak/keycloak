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
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

public class DeviceRiskStrategyTest {

    private final DeviceRiskStrategy strategy = new DeviceRiskStrategy();

    @Test
    public void knownDeviceGivesLowRisk() {
        String fingerprint = DeviceRiskStrategy.fingerprint("UA", "en");

        assertThat(strategy.evaluate(context("UA", "en", Set.of(fingerprint), true), AdaptiveAuthPolicy.defaults()).getRawScore(),
                equalTo(0));
    }

    @Test
    public void newDeviceGivesConfiguredRisk() {
        assertThat(strategy.evaluate(context("UA2", "en", Set.of(DeviceRiskStrategy.fingerprint("UA1", "en")), true),
                AdaptiveAuthPolicy.defaults()).getRawScore(), equalTo(60));
    }

    @Test
    public void missingUserAgentGivesLowNeutralRisk() {
        assertThat(DeviceRiskStrategy.fingerprint(null, "en"), nullValue());
        assertThat(strategy.evaluate(context(null, "en", Set.of(), true), AdaptiveAuthPolicy.defaults()).getRawScore(),
                equalTo(10));
    }

    @Test
    public void fingerprintIsHashed() {
        assertThat(DeviceRiskStrategy.fingerprint("UA", "en"), not(equalTo("ua|en")));
    }

    private static org.keycloak.authentication.authenticators.browser.risk.context.LoginContext context(String userAgent,
            String acceptLanguage, Set<String> devices, boolean historyAvailable) {
        return LoginContextTestUtils.context(0, "127.0.0.1", userAgent, acceptLanguage, null, Instant.EPOCH, Set.of(),
                devices, Set.of(), historyAvailable);
    }
}
