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

import java.util.Map;

import org.keycloak.authentication.authenticators.browser.risk.context.LoginContext;
import org.keycloak.authentication.authenticators.browser.risk.decision.AdaptiveAuthPolicy;
import org.keycloak.authentication.authenticators.browser.risk.scoring.RiskFactorResult;

public class FailedAttemptRiskStrategy implements RiskStrategy {

    public static final String NAME = "failed_attempts";

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public RiskFactorResult evaluate(LoginContext loginContext, AdaptiveAuthPolicy policy) {
        int threshold = Math.max(1, policy.getFailedAttemptsThreshold());
        int failures = Math.max(0, loginContext.getRecentFailedAttempts());
        int raw = Math.min(100, (failures * 100) / threshold);

        return RiskFactorResult.raw(getName(), raw, Map.of(
                "failures", Integer.toString(failures),
                "threshold", Integer.toString(threshold)));
    }
}
