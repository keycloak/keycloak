/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.credential;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.keycloak.Config;
import org.keycloak.config.MetricsOptions;
import org.keycloak.models.KeycloakSession;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.Metrics;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class PasswordCredentialProviderFactory implements CredentialProviderFactory<PasswordCredentialProvider> {
    public static final String PROVIDER_ID = "keycloak-password";
    private static final String HASHES_COUNTER_TAGS = "validations-counter-tags";
    private static final String KEYCLOAK_METER_NAME_PREFIX = "keycloak.";
    private static final String LOGIN_PASSWORD_VERIFY_METER_NAME = KEYCLOAK_METER_NAME_PREFIX + "credentials.password.hashing";
    private static final String LOGIN_PASSWORD_VERIFY_METER_DESCRIPTION = "Password validations";

    public static final String METER_REALM_TAG = "realm";
    public static final String METER_ALGORITHM_TAG = "algorithm";
    public static final String METER_HASHING_STRENGTH_TAG = "hashing_strength";
    public static final String METER_VALIDATION_OUTCOME_TAG = "outcome";
    private static final String HASHES_COUNTER_TAGS_DEFAULT_VALUE = String.format("%s,%s,%s,%s", METER_REALM_TAG, METER_ALGORITHM_TAG, METER_HASHING_STRENGTH_TAG, METER_VALIDATION_OUTCOME_TAG);

    private boolean metricsEnabled;
    private boolean withRealmInMetric;
    private boolean withAlgorithmInMetric;
    private boolean withHashingStrengthInMetric;
    private boolean withOutcomeInMetric;

    private Meter.MeterProvider<Counter> meterProvider;

    @Override
    public PasswordCredentialProvider create(KeycloakSession session) {
        return new PasswordCredentialProvider(session, meterProvider, metricsEnabled, withRealmInMetric, withAlgorithmInMetric, withHashingStrengthInMetric, withOutcomeInMetric);
    }

    @Override
    public void init(Config.Scope config) {
        metricsEnabled = config.root().getBoolean(MetricsOptions.METRICS_ENABLED.getKey(), false);
        if (metricsEnabled) {
            meterProvider = Counter.builder(LOGIN_PASSWORD_VERIFY_METER_NAME)
                    .description(LOGIN_PASSWORD_VERIFY_METER_DESCRIPTION)
                    .baseUnit("validations")
                    .withRegistry(Metrics.globalRegistry);

            Set<String> tags = Arrays.stream(config.get(HASHES_COUNTER_TAGS, HASHES_COUNTER_TAGS_DEFAULT_VALUE).split(",")).collect(Collectors.toSet());
            withRealmInMetric = tags.contains(METER_REALM_TAG);
            withAlgorithmInMetric = tags.contains(METER_ALGORITHM_TAG);
            withHashingStrengthInMetric = tags.contains(METER_HASHING_STRENGTH_TAG);
            withOutcomeInMetric = tags.contains(METER_VALIDATION_OUTCOME_TAG);
        }
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public List<ProviderConfigProperty> getConfigMetadata() {
        return ProviderConfigurationBuilder.create()
                .property()
                    .name(HASHES_COUNTER_TAGS)
                    .type("string")
                    .helpText("Comma-separated list of tags to be used when publishing password validation counter metric.")
                    .options(METER_REALM_TAG, METER_ALGORITHM_TAG, METER_HASHING_STRENGTH_TAG, METER_VALIDATION_OUTCOME_TAG)
                    .defaultValue(HASHES_COUNTER_TAGS_DEFAULT_VALUE)
                    .add()
                .build();
    }
}
