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

package org.keycloak.testframework.conformance.runner;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The {@code options} of a suite configuration.
 */
public record SuiteOptions(
        // Whether the suite's browser processes the CSS of the pages it visits. Disabling it saves most of the time the
        // browser spends on Keycloak's pages, which the scripted interactions do not need.
        @JsonProperty("browsercontrol_css_enable") boolean browserControlCssEnabled) {

    public static final SuiteOptions WITHOUT_BROWSER_CSS = new SuiteOptions(false);
}
