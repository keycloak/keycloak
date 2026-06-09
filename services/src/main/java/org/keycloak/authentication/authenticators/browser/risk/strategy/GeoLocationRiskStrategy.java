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

import java.util.Locale;
import java.util.Map;

import org.keycloak.authentication.authenticators.browser.risk.context.LoginContext;
import org.keycloak.authentication.authenticators.browser.risk.decision.AdaptiveAuthPolicy;
import org.keycloak.authentication.authenticators.browser.risk.scoring.RiskFactorResult;
import org.keycloak.utils.StringUtil;

public class GeoLocationRiskStrategy implements RiskStrategy {

    public static final String NAME = "geo";

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public RiskFactorResult evaluate(LoginContext loginContext, AdaptiveAuthPolicy policy) {
        String geoSignal = normalize(loginContext.getGeoSignal());

        if (geoSignal == null) {
            return RiskFactorResult.raw(getName(), 0, Map.of("reason", "missing_geo_signal"));
        }

        if (!loginContext.isHistoryAvailable()) {
            return RiskFactorResult.raw(getName(), 10, Map.of("reason", "history_unavailable"));
        }

        if (loginContext.getRecentGeoSignals().isEmpty()) {
            return RiskFactorResult.raw(getName(), 10, Map.of("reason", "no_geo_history"));
        }

        if (loginContext.getRecentGeoSignals().contains(geoSignal)) {
            return RiskFactorResult.raw(getName(), 0, Map.of("reason", "known_geo"));
        }

        return RiskFactorResult.raw(getName(), policy.getNewGeoRiskScore(), Map.of("reason", "new_geo"));
    }

    static String normalize(String value) {
        if (StringUtil.isBlank(value)) {
            return null;
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }
}
