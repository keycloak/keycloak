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

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Locale;
import java.util.Map;

import org.keycloak.authentication.authenticators.browser.risk.context.LoginContext;
import org.keycloak.authentication.authenticators.browser.risk.decision.AdaptiveAuthPolicy;
import org.keycloak.authentication.authenticators.browser.risk.scoring.RiskFactorResult;
import org.keycloak.utils.StringUtil;

public class DeviceRiskStrategy implements RiskStrategy {

    public static final String NAME = "device";

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public RiskFactorResult evaluate(LoginContext loginContext, AdaptiveAuthPolicy policy) {
        String fingerprint = fingerprint(loginContext.getUserAgent(), loginContext.getAcceptLanguage());

        if (fingerprint == null) {
            return RiskFactorResult.raw(getName(), 10, Map.of("reason", "missing_user_agent"));
        }

        if (!loginContext.isHistoryAvailable()) {
            return RiskFactorResult.raw(getName(), 10, Map.of("reason", "history_unavailable"));
        }

        if (loginContext.getRecentDeviceFingerprints().isEmpty()) {
            return RiskFactorResult.raw(getName(), 10, Map.of("reason", "no_device_history"));
        }

        if (loginContext.getRecentDeviceFingerprints().contains(fingerprint)) {
            return RiskFactorResult.raw(getName(), 0, Map.of("reason", "known_device"));
        }

        return RiskFactorResult.raw(getName(), policy.getNewDeviceRiskScore(), Map.of("reason", "new_device"));
    }

    public static String fingerprint(String userAgent, String acceptLanguage) {
        if (StringUtil.isBlank(userAgent)) {
            return null;
        }

        String normalized = userAgent.trim().toLowerCase(Locale.ROOT) + "|"
                + (acceptLanguage == null ? "" : acceptLanguage.trim().toLowerCase(Locale.ROOT));

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(normalized.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 must be available", e);
        }
    }
}
