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

package org.keycloak.tests.conformance.runner;

import java.util.Map;

/**
 * A conformance suite test module to run in one variant combination. The plan variant selects the plan wide
 * variant when the plan is created, while the module variant selects which combination of the module to run, as
 * a plan can contain the same module in several variant combinations.
 */
public record ConformanceModuleVariant(
        String plan,
        Map<String, String> planVariant,
        String name,
        Map<String, String> moduleVariant,
        ConformanceResult expectedResult,
        BrowserInteraction browserInteraction) {

    public ConformanceModuleVariant(String plan, Map<String, String> planVariant,
            String name, Map<String, String> moduleVariant) {
        this(plan, planVariant, name, moduleVariant, ConformanceResult.PASSED, BrowserInteraction.NONE);
    }

    // Display name for the parameterized test, including the module variant to keep combinations distinguishable
    @Override
    public String toString() {
        return moduleVariant.isEmpty() ? plan + " " + name : plan + " " + name + " " + moduleVariant;
    }
}
