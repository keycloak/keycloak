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

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

public record BrowserFlow(
        String match,
        // The suite uses this flow at most matchLimit times, then falls through to the next matching flow. This
        // lets a module that visits the authorization endpoint several times run a different flow per visit.
        @JsonProperty("match-limit") @JsonInclude(JsonInclude.Include.NON_NULL) Integer matchLimit,
        List<BrowserTask> tasks) {

    public BrowserFlow(String match, List<BrowserTask> tasks) {
        this(match, null, tasks);
    }

    public record BrowserTask(String task, String match, boolean optional, List<List<Object>> commands) {

        public static final String TEXT = "text";
        public static final String CLICK = "click";
        public static final String WAIT = "wait";
        // Predefined action of the wait command that snapshots the page into the screenshot placeholder
        public static final String UPDATE_IMAGE_PLACEHOLDER = "update-image-placeholder";

        // A required task: the suite fails the run if its URL is never visited
        public BrowserTask(String task, String match, List<List<Object>> commands) {
            this(task, match, false, commands);
        }
    }
}
