/*
 * Copyright 2026 Andrzej Świętek
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

package org.keycloak.organization.authentication.authenticators.conditional;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static org.keycloak.organization.authentication.authenticators.conditional.ConditionalOrganizationAttributeAuthenticatorFactory.CONF_ATTRIBUTE_EXPECTED_VALUE;
import static org.keycloak.organization.authentication.authenticators.conditional.ConditionalOrganizationAttributeAuthenticatorFactory.CONF_ATTRIBUTE_NAME;
import static org.keycloak.organization.authentication.authenticators.conditional.ConditionalOrganizationAttributeAuthenticatorFactory.CONF_NOT;
import static org.keycloak.organization.authentication.authenticators.conditional.ConditionalOrganizationAttributeAuthenticatorFactory.CONF_ORGANIZATION_SOURCE;

/**
 * The configuration and decision of the {@code Condition - organization attribute} execution.
 */
final class OrganizationAttributeCondition {

    private OrganizationAttributeCondition() {
    }

    enum Source {

        ANY_MEMBERSHIP("any-membership"),
        SELECTED("selected");

        private final String configValue;

        Source(String configValue) {
            this.configValue = configValue;
        }

        String configValue() {
            return configValue;
        }

        static Optional<Source> fromConfigValue(String value) {
            return Arrays.stream(values()).filter(source -> source.configValue.equals(value)).findFirst();
        }

        static List<String> configValues() {
            return Arrays.stream(values()).map(Source::configValue).toList();
        }
    }

    record Config(String attributeName, String expectedValue, Source source, boolean negate) {

        /**
         * The configuration, or empty when the attribute name or the expected value is missing or empty, or the
         * organization option holds an unknown value. Values are taken verbatim.
         */
        static Optional<Config> from(Map<String, String> config) {
            return hasValue(config, CONF_ATTRIBUTE_NAME) && hasValue(config, CONF_ATTRIBUTE_EXPECTED_VALUE)
                    ? source(config).map(source -> withSource(config, source))
                    : Optional.empty();
        }

        private static Config withSource(Map<String, String> config, Source source) {
            return new Config(config.get(CONF_ATTRIBUTE_NAME), config.get(CONF_ATTRIBUTE_EXPECTED_VALUE), source, negate(config));
        }

        private static boolean negate(Map<String, String> config) {
            return Boolean.parseBoolean(config.get(CONF_NOT));
        }

        private static Optional<Source> source(Map<String, String> config) {
            return Source.fromConfigValue(value(config, CONF_ORGANIZATION_SOURCE).orElse(Source.ANY_MEMBERSHIP.configValue()));
        }

        private static boolean hasValue(Map<String, String> config, String key) {
            return value(config, key).isPresent();
        }

        private static Optional<String> value(Map<String, String> config, String key) {
            return Optional.ofNullable(config.get(key)).filter(value -> !value.isEmpty());
        }
    }

    /**
     * Whether one of {@code organizationAttributes} holds the expected value under the attribute name, inverted when
     * negated.
     */
    static boolean matches(Config config, Stream<Map<String, List<String>>> organizationAttributes) {
        boolean found = organizationAttributes.anyMatch(attributes -> holdsExpectedValue(config, attributes));
        return config.negate() != found;
    }

    private static boolean holdsExpectedValue(Config config, Map<String, List<String>> attributes) {
        return attributes.getOrDefault(config.attributeName(), List.of()).contains(config.expectedValue());
    }
}
