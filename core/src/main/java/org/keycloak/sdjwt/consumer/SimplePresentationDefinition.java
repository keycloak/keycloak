/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.sdjwt.consumer;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.keycloak.common.VerificationException;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * A simple presentation definition of the kind of credential expected.
 *
 * <p>
 * The credential's type and required claims are configured using regex patterns.
 * The values of these fields are JSON-ified prior to matching the regex pattern.
 * </p>
 *
 * @author <a href="mailto:Ingrid.Kamga@adorsys.com">Ingrid Kamga</a>
 */
public class SimplePresentationDefinition implements PresentationRequirements {

    private final Map<String, Pattern> requirements;

    public SimplePresentationDefinition(Map<String, Pattern> requirements) {
        this.requirements = requirements;
    }

    /**
     * Checks if the provided JSON payload satisfies all required field patterns.
     *
     * <p>
     * For each required field, the corresponding JSON field value in the disclosed Issuer-signed JWT's payload
     * is matched against the associated regex pattern. If any required field is missing or does not match the
     * pattern, a {@link VerificationException} is thrown.
     * </p>
     *
     * @param disclosedPayload The fully disclosed Issuer-signed JWT of the presented token.
     * @throws VerificationException If any required field is missing or fails the pattern check.
     */
    @Override
    public void checkIfSatisfiedBy(JsonNode disclosedPayload) throws VerificationException {
        for (Map.Entry<String, Pattern> requirement : requirements.entrySet()) {
            String field = requirement.getKey();
            Pattern pattern = requirement.getValue();

            // Retrieve the value of the required field from the payload
            JsonNode presented = disclosedPayload.get(field);

            // Check if the required field is present in the payload
            if (presented == null || presented.isNull()) {
                throw new VerificationException(
                        String.format("A required field was not presented: `%s`", field)
                );
            }

            // Extract the JSON representation of the field's value
            String json = presented.toString();

            // Match the field value against the configured regex pattern
            Matcher matcher = pattern.matcher(json);
            if (!matcher.matches()) {
                throw new VerificationException(String.format(
                        "Pattern matching failed for required field: `%s`. Expected pattern: /%s/, but got: %s",
                        field, pattern.pattern(), json
                ));
            }
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final Map<String, Pattern> requirements = new HashMap<>();

        public Builder addClaimRequirement(String field, String regexPattern) {
            this.requirements.put(field, Pattern.compile(regexPattern));
            return this;
        }

        public SimplePresentationDefinition build() {
            return new SimplePresentationDefinition(requirements);
        }
    }
}
