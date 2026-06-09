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
import java.util.List;

public final class RiskScore {

    private final int value;
    private final RiskLevel level;
    private final List<RiskFactorResult> factors;

    public RiskScore(int value, RiskLevel level, List<RiskFactorResult> factors) {
        this.value = Math.max(0, Math.min(100, value));
        this.level = level;
        this.factors = factors == null ? Collections.emptyList() : List.copyOf(factors);
    }

    public int getValue() {
        return value;
    }

    public RiskLevel getLevel() {
        return level;
    }

    public List<RiskFactorResult> getFactors() {
        return factors;
    }
}
