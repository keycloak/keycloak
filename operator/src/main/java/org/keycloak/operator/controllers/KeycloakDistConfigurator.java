/*
 * Copyright 2022 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.operator.controllers;

import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.EnvVarBuilder;
import io.fabric8.kubernetes.api.model.EnvVarSourceBuilder;
import io.fabric8.kubernetes.api.model.SecretKeySelector;
import io.fabric8.kubernetes.api.model.VolumeBuilder;
import io.fabric8.kubernetes.api.model.VolumeMountBuilder;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.quarkus.logging.Log;
import org.keycloak.common.util.CollectionUtil;
import org.keycloak.operator.Constants;
import org.keycloak.operator.crds.v2alpha1.deployment.Keycloak;
import org.keycloak.operator.crds.v2alpha1.deployment.KeycloakStatusAggregator;
import org.keycloak.operator.crds.v2alpha1.deployment.ValueOrSecret;
import org.keycloak.operator.crds.v2alpha1.deployment.spec.DatabaseSpec;
import org.keycloak.operator.crds.v2alpha1.deployment.spec.FeatureSpec;
import org.keycloak.operator.crds.v2alpha1.deployment.spec.HostnameSpec;
import org.keycloak.operator.crds.v2alpha1.deployment.spec.HttpSpec;
import org.keycloak.operator.crds.v2alpha1.deployment.spec.TransactionsSpec;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static io.smallrye.config.common.utils.StringUtil.replaceNonAlphanumericByUnderscores;
import static org.keycloak.operator.crds.v2alpha1.CRDUtils.isTlsConfigured;

/**
 * Configuration for the KeycloakDeployment
 */
public class KeycloakDistConfigurator {
    private final Keycloak keycloakCR;
    private final StatefulSet deployment;
    private final KubernetesClient client;

    public KeycloakDistConfigurator(Keycloak keycloakCR, StatefulSet deployment, KubernetesClient client) {
        this.keycloakCR = keycloakCR;
        this.deployment = deployment;
        this.client = client;
    }

    /**
     * Specify first-class citizens fields which should not be added as general server configuration property
     */
    private final Set<String> firstClassConfigOptions = new HashSet<>();

    /**
     * Configure configuration properties for the KeycloakDeployment
     */
    public void configureDistOptions() {
        configureHostname();
        configureFeatures();
        configureTransactions();
        configureHttp();
        configureDatabase();
    }

    /**
     * Validate all deployment configuration properties and update status of the Keycloak deployment
     *
     * @param status Keycloak Status builder
     */
    public void validateOptions(KeycloakStatusAggregator status) {
        assumeFirstClassCitizens(status);
    }

    /* ---------- Configuration of first-class citizen fields ---------- */

    public void configureHostname() {
        optionMapper(keycloakCR.getSpec().getHostnameSpec())
                .mapOption("hostname", HostnameSpec::getHostname)
                .mapOption("hostname-admin", HostnameSpec::getAdmin)
                .mapOption("hostname-admin-url", HostnameSpec::getAdminUrl)
                .mapOption("hostname-strict", HostnameSpec::isStrict)
                .mapOption("hostname-strict-backchannel", HostnameSpec::isStrictBackchannel);
    }

    public void configureFeatures() {
        optionMapper(keycloakCR.getSpec().getFeatureSpec())
                .mapOptionFromCollection("features", FeatureSpec::getEnabledFeatures)
                .mapOptionFromCollection("features-disabled", FeatureSpec::getDisabledFeatures);
    }

    public void configureTransactions() {
        optionMapper(keycloakCR.getSpec().getTransactionsSpec())
                .mapOption("transaction-xa-enabled", TransactionsSpec::isXaEnabled);
    }

    public void configureHttp() {
        var optionMapper = optionMapper(keycloakCR.getSpec().getHttpSpec())
                .mapOption("http-enabled", HttpSpec::getHttpEnabled)
                .mapOption("http-port", HttpSpec::getHttpPort)
                .mapOption("https-port", HttpSpec::getHttpsPort);

        configureTLS(optionMapper);
    }

    public void configureTLS(OptionMapper<HttpSpec> optionMapper) {
        final String certFileOptionName = "https-certificate-file";
        final String keyFileOptionName = "https-certificate-key-file";

        if (!isTlsConfigured(keycloakCR)) {
            // for mapping and triggering warning in status if someone uses the fields directly
            optionMapper.mapOption(certFileOptionName);
            optionMapper.mapOption(keyFileOptionName);
            return;
        }

        optionMapper.mapOption(certFileOptionName, Constants.CERTIFICATES_FOLDER + "/tls.crt");
        optionMapper.mapOption(keyFileOptionName, Constants.CERTIFICATES_FOLDER + "/tls.key");

        var kcContainer = deployment.getSpec().getTemplate().getSpec().getContainers().get(0);

        var volume = new VolumeBuilder()
                .withName("keycloak-tls-certificates")
                .withNewSecret()
                .withSecretName(keycloakCR.getSpec().getHttpSpec().getTlsSecret())
                .withOptional(false)
                .endSecret()
                .build();

        var volumeMount = new VolumeMountBuilder()
                .withName(volume.getName())
                .withMountPath(Constants.CERTIFICATES_FOLDER)
                .build();

        deployment.getSpec().getTemplate().getSpec().getVolumes().add(volume);
        kcContainer.getVolumeMounts().add(volumeMount);
    }

    public void configureDatabase() {
        optionMapper(keycloakCR.getSpec().getDatabaseSpec())
                .mapOption("db", DatabaseSpec::getVendor)
                .mapOption("db-username", DatabaseSpec::getUsernameSecret)
                .mapOption("db-password", DatabaseSpec::getPasswordSecret)
                .mapOption("db-url-database", DatabaseSpec::getDatabase)
                .mapOption("db-url-host", DatabaseSpec::getHost)
                .mapOption("db-url-port", DatabaseSpec::getPort)
                .mapOption("db-schema", DatabaseSpec::getSchema)
                .mapOption("db-url", DatabaseSpec::getUrl)
                .mapOption("db-pool-initial-size", DatabaseSpec::getPoolInitialSize)
                .mapOption("db-pool-min-size", DatabaseSpec::getPoolMinSize)
                .mapOption("db-pool-max-size", DatabaseSpec::getPoolMaxSize);
    }

    /* ---------- END of configuration of first-class citizen fields ---------- */

    /**
     * Assume the specified first-class citizens are not included in the general server configuration
     *
     * @param status                    Status of the deployment
     */
    protected void assumeFirstClassCitizens(KeycloakStatusAggregator status) {
        final var serverConfigNames = keycloakCR
                .getSpec()
                .getAdditionalOptions()
                .stream()
                .map(ValueOrSecret::getName)
                .collect(Collectors.toSet());

        final var sameItems = CollectionUtil.intersection(serverConfigNames, firstClassConfigOptions);
        if (CollectionUtil.isNotEmpty(sameItems)) {
            status.addWarningMessage("You need to specify these fields as the first-class citizen of the CR: "
                    + CollectionUtil.join(sameItems, ","));
        }
    }

    public static String getKeycloakOptionEnvVarName(String kcConfigName) {
        // TODO make this use impl from Quarkus dist (Configuration.toEnvVarFormat)
        return "KC_" + replaceNonAlphanumericByUnderscores(kcConfigName).toUpperCase();
    }

    private <T> OptionMapper<T> optionMapper(T optionSpec) {
        return new OptionMapper<>(optionSpec);
    }

    public Collection<String> getSecretNames() {
        Set<String> names = new HashSet<>();

        if (isTlsConfigured(keycloakCR)) {
            names.add(keycloakCR.getSpec().getHttpSpec().getTlsSecret());
        }

        Optional.ofNullable(keycloakCR.getSpec().getDatabaseSpec()).map(DatabaseSpec::getUsernameSecret).map(SecretKeySelector::getName).ifPresent(names::add);
        Optional.ofNullable(keycloakCR.getSpec().getDatabaseSpec()).map(DatabaseSpec::getPasswordSecret).map(SecretKeySelector::getName).ifPresent(names::add);

        return names;
    }

    private class OptionMapper<T> {
        private final T categorySpec;
        private final List<EnvVar> envVars;

        public OptionMapper(T optionSpec) {
            this.categorySpec = optionSpec;

            var kcContainer = deployment.getSpec().getTemplate().getSpec().getContainers().get(0);
            var envVars = kcContainer.getEnv();
            if (envVars == null) {
                envVars = new ArrayList<>();
                kcContainer.setEnv(envVars);
            }
            this.envVars = envVars;
        }

        public <R> OptionMapper<T> mapOption(String optionName, Function<T, R> optionValueSupplier) {
            firstClassConfigOptions.add(optionName);

            if (categorySpec == null) {
                Log.debugf("No category spec provided for %s", optionName);
                return this;
            }

            R value = optionValueSupplier.apply(categorySpec);

            if (value == null || value.toString().trim().isEmpty()) {
                Log.debugf("No value provided for %s", optionName);
                return this;
            }

            EnvVarBuilder envVarBuilder = new EnvVarBuilder()
                    .withName(getKeycloakOptionEnvVarName(optionName));

            if (value instanceof SecretKeySelector) {
                envVarBuilder.withValueFrom(new EnvVarSourceBuilder().withSecretKeyRef((SecretKeySelector) value).build());
            } else {
                envVarBuilder.withValue(String.valueOf(value));
            }

            var toAdd = envVarBuilder.build();
            if (!envVars.stream().anyMatch(envVar -> envVar.getName().equals(toAdd.getName()))) {
                envVars.add(toAdd);
            }

            return this;
        }

        public <R> OptionMapper<T> mapOption(String optionName) {
            return mapOption(optionName, s -> null);
        }

        public <R> OptionMapper<T> mapOption(String optionName, R optionValue) {
            return mapOption(optionName, s -> optionValue);
        }

        protected <R extends Collection<?>> OptionMapper<T> mapOptionFromCollection(String optionName, Function<T, R> optionValueSupplier) {
            return mapOption(optionName, s -> {
                var value = optionValueSupplier.apply(s);
                if (value == null) return null;
                return value.stream().filter(Objects::nonNull).map(String::valueOf).collect(Collectors.joining(","));
            });
        }
    }
}
