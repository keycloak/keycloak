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
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import com.fasterxml.jackson.databind.JsonNode;

public record ConformanceModuleResult(
        String plan,
        Map<String, String> planVariant,
        String module,
        Map<String, String> moduleVariant,
        String planId,
        String moduleId,
        String status,
        String result,
        JsonNode logs) {

    public boolean finishedWith(ConformanceResult expectedResult) {
        return "FINISHED".equals(status) && expectedResult.name().equals(result);
    }

    public String failureSummary() {
        return "Conformance module failed"
                + "\n  plan=" + plan
                + "\n  planVariant=" + planVariant
                + "\n  module=" + module
                + "\n  moduleVariant=" + moduleVariant
                + "\n  planId=" + planId
                + "\n  moduleId=" + moduleId
                + "\n  status=" + status
                + "\n  result=" + result
                + "\n  logExcerpt=" + failureLogExcerpt();
    }

    private String failureLogExcerpt() {
        if (logs == null || !logs.isArray()) {
            return "<no logs>";
        }
        String excerpt = StreamSupport.stream(logs.spliterator(), false)
                .filter(log -> "FAILURE".equals(log.path("result").asText())
                        || "WARNING".equals(log.path("result").asText()))
                .limit(20)
                .map(this::formatLog)
                .collect(Collectors.joining(" | "));
        return excerpt.isBlank() ? "<no failure or warning log entries>" : excerpt;
    }

    private String formatLog(JsonNode log) {
        String source = log.path("src").asText(log.path("condition").asText(""));
        String message = log.path("msg").asText(log.path("message").asText(log.path("error").asText("")));
        StringBuilder formatted = new StringBuilder(log.path("result").asText() + ":" + source + " " + message);
        for (String detail : new String[] {"url", "match", "detail"}) {
            if (log.hasNonNull(detail)) {
                formatted.append(" ").append(detail).append("=").append(log.get(detail).asText());
            }
        }
        return formatted.toString();
    }
}
