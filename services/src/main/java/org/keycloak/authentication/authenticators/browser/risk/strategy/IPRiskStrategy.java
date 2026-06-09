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
import org.keycloak.utils.StringUtil;

public class IPRiskStrategy implements RiskStrategy {

    public static final String NAME = "ip";

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public RiskFactorResult evaluate(LoginContext loginContext, AdaptiveAuthPolicy policy) {
        String remoteAddress = normalize(loginContext.getRemoteAddress());

        if (remoteAddress == null) {
            return RiskFactorResult.raw(getName(), 0, Map.of("reason", "missing_ip"));
        }

        if (!loginContext.isHistoryAvailable()) {
            return RiskFactorResult.raw(getName(), 10, Map.of("reason", "history_unavailable"));
        }

        if (loginContext.getRecentSuccessfulLoginIps().isEmpty()) {
            return RiskFactorResult.raw(getName(), 10, Map.of("reason", "no_ip_history"));
        }

        if (loginContext.getRecentSuccessfulLoginIps().contains(remoteAddress)) {
            return RiskFactorResult.raw(getName(), 0, Map.of("reason", "known_ip"));
        }

        return RiskFactorResult.raw(getName(), policy.getNewIpRiskScore(), Map.of("reason", "new_ip"));
    }

    private static String normalize(String value) {
        if (StringUtil.isBlank(value)) {
            return null;
        }
        return value.trim();
    }
}
