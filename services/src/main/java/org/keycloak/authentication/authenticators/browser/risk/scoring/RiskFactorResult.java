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

package org.keycloak.authentication.authenticators.browser.risk.scoring;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class RiskFactorResult {

    private final String factor;
    private final int rawScore;
    private final int weightedScore;
    private final Map<String, String> details;

    public RiskFactorResult(String factor, int rawScore, int weightedScore, Map<String, String> details) {
        this.factor = factor;
        this.rawScore = clamp(rawScore);
        this.weightedScore = Math.max(0, weightedScore);
        this.details = details == null ? Collections.emptyMap() :
                Collections.unmodifiableMap(new LinkedHashMap<>(details));
    }

    public static RiskFactorResult raw(String factor, int rawScore, Map<String, String> details) {
        return new RiskFactorResult(factor, rawScore, 0, details);
    }

    public RiskFactorResult withWeightedScore(int weightedScore) {
        return new RiskFactorResult(factor, rawScore, weightedScore, details);
    }

    private static int clamp(int value) {
        return Math.max(0, Math.min(100, value));
    }

    public String getFactor() {
        return factor;
    }

    public int getRawScore() {
        return rawScore;
    }

    public int getWeightedScore() {
        return weightedScore;
    }

    public Map<String, String> getDetails() {
        return details;
    }
}
