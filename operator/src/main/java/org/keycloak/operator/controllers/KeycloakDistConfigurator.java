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
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import org.keycloak.common.util.CollectionUtil;
import org.keycloak.operator.Constants;
import org.keycloak.operator.crds.v2alpha1.deployment.Keycloak;
import org.keycloak.operator.crds.v2alpha1.deployment.KeycloakStatusAggregator;
import org.keycloak.operator.crds.v2alpha1.deployment.ValueOrSecret;
import org.keycloak.operator.crds.v2alpha1.deployment.spec.BootstrapAdminSpec;
import org.keycloak.operator.crds.v2alpha1.deployment.spec.DatabaseSpec;
import org.keycloak.operator.crds.v2alpha1.deployment.spec.FeatureSpec;
import org.keycloak.operator.crds.v2alpha1.deployment.spec.HostnameSpec;
import org.keycloak.operator.crds.v2alpha1.deployment.spec.HttpManagementSpec;
import org.keycloak.operator.crds.v2alpha1.deployment.spec.HttpSpec;
import org.keycloak.operator.crds.v2alpha1.deployment.spec.ProxySpec;
import org.keycloak.operator.crds.v2alpha1.deployment.spec.TracingSpec;
import org.keycloak.operator.crds.v2alpha1.deployment.spec.TransactionsSpec;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import static io.smallrye.config.common.utils.StringUtil.replaceNonAlphanumericByUnderscores;

/**
 * Configuration for the Keycloak Statefulset
 */
@ApplicationScoped
public class KeycloakDistConfigurator {

    public static final String KC_PREFIX = "KC_";
    /**
     * Specify first-class citizens fields which should not be added as general server configuration property
     */
    @SuppressWarnings("rawtypes")
    private final Map<String, org.keycloak.operator.controllers.KeycloakDistConfigurator.OptionMapper.Mapper> firstClassConfigOptions = new LinkedHashMap<>();

    public KeycloakDistConfigurator() {
        // register the configuration mappers for the various parts of the keycloak cr
        configureHostname();
        configureFeatures();
        configureTracing();
        configureTransactions();
        configureHttp();
        configureDatabase();
        configureCache();
        configureProxy();
        configureManagement();
        configureBootstrapAdmin();
    }

    /**
     * Validate all deployment configuration properties and update status of the Keycloak deployment
     *
     * @param status Keycloak Status builder
     */
    public void validateOptions(Keycloak keycloakCR, KeycloakStatusAggregator status) {
        assumeFirstClassCitizens(keycloakCR, status);
    }

    /* ---------- Configuration of first-class citizen fields ---------- */

    void configureBootstrapAdmin() {
        optionMapper(Function.identity())
                .mapOption("bootstrap-admin-username",
                        keycloakCR -> Optional.ofNullable(keycloakCR.getSpec().getBootstrapAdminSpec())
                                .map(BootstrapAdminSpec::getUser).map(BootstrapAdminSpec.User::getSecret)
                                .or(() -> Optional.of(KeycloakAdminSecretDependentResource.getName(keycloakCR)))
                                .map(s -> new SecretKeySelector("username", s, null)).orElse(null))
                .mapOption("bootstrap-admin-password",
                        keycloakCR -> Optional.ofNullable(keycloakCR.getSpec().getBootstrapAdminSpec())
                                .map(BootstrapAdminSpec::getUser).map(BootstrapAdminSpec.User::getSecret)
                                .or(() -> Optional.of(KeycloakAdminSecretDependentResource.getName(keycloakCR)))
                                .map(s -> new SecretKeySelector("password", s, null)).orElse(null));

        optionMapper(keycloakCR -> keycloakCR.getSpec().getBootstrapAdminSpec())
                .mapOption("bootstrap-admin-client-id",
                        spec -> Optional.ofNullable(spec.getService()).map(BootstrapAdminSpec.Service::getSecret).map(s -> new SecretKeySelector("client-id", s, null)).orElse(null))
                .mapOption("bootstrap-admin-client-secret",
                        spec -> Optional.ofNullable(spec.getService()).map(BootstrapAdminSpec.Service::getSecret).map(s -> new SecretKeySelector("client-secret", s, null)).orElse(null));
    }

    void configureHostname() {
        optionMapper(keycloakCR -> keycloakCR.getSpec().getHostnameSpec())
                .mapOption("hostname", HostnameSpec::getHostname)
                .mapOption("hostname-admin", HostnameSpec::getAdmin)
                .mapOption("hostname-admin-url", HostnameSpec::getAdminUrl)
                .mapOption("hostname-strict", HostnameSpec::isStrict)
                .mapOption("hostname-strict-backchannel", HostnameSpec::isStrictBackchannel)
                .mapOption("hostname-backchannel-dynamic", HostnameSpec::isBackchannelDynamic);
    }

    void configureFeatures() {
        optionMapper(keycloakCR -> keycloakCR.getSpec().getFeatureSpec())
                .mapOptionFromCollection("features", FeatureSpec::getEnabledFeatures)
                .mapOptionFromCollection("features-disabled", FeatureSpec::getDisabledFeatures);
    }

    void configureTracing() {
        optionMapper(keycloakCR -> keycloakCR.getSpec().getTracingSpec())
                .mapOption("tracing-enabled", TracingSpec::getEnabled)
                .mapOption("tracing-service-name", TracingSpec::getServiceName)
                .mapOption("tracing-endpoint", TracingSpec::getEndpoint)
                .mapOption("tracing-protocol", TracingSpec::getProtocol)
                .mapOption("tracing-sampler-type", TracingSpec::getSamplerType)
                .mapOption("tracing-sampler-ratio", TracingSpec::getSamplerRatio)
                .mapOption("tracing-compression", TracingSpec::getCompression)
                .mapOption("tracing-resource-attributes", TracingSpec::getResourceAttributesString);
    }

    void configureTransactions() {
        optionMapper(keycloakCR -> keycloakCR.getSpec().getTransactionsSpec())
                .mapOption("transaction-xa-enabled", TransactionsSpec::isXaEnabled);
    }

    void configureHttp() {
        optionMapper(keycloakCR -> keycloakCR.getSpec().getHttpSpec())
                .mapOption("http-enabled", HttpSpec::getHttpEnabled)
                .mapOption("http-port", HttpSpec::getHttpPort)
                .mapOption("https-port", HttpSpec::getHttpsPort)
                .mapOption("https-certificate-file", http -> (http.getTlsSecret() != null && !http.getTlsSecret().isEmpty()) ? Constants.CERTIFICATES_FOLDER + "/tls.crt" : null)
                .mapOption("https-certificate-key-file", http -> (http.getTlsSecret() != null && !http.getTlsSecret().isEmpty()) ? Constants.CERTIFICATES_FOLDER + "/tls.key" : null);
    }

    void configureCache() {
        optionMapper(keycloakCR -> keycloakCR.getSpec().getCacheSpec())
                .mapOption("cache-config-file", cache -> Optional.ofNullable(cache.getConfigMapFile()).map(c -> Constants.CACHE_CONFIG_SUBFOLDER + "/" + c.getKey()).orElse(null));
    }

    void configureDatabase() {
        optionMapper(keycloakCR -> keycloakCR.getSpec().getDatabaseSpec())
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

    void configureProxy() {
        optionMapper(keycloakCR -> keycloakCR.getSpec().getProxySpec())
                .mapOption("proxy-headers", ProxySpec::getHeaders);
    }

    void configureManagement() {
        optionMapper(keycloakCR -> keycloakCR.getSpec().getHttpManagementSpec())
                .mapOption("http-management-port", HttpManagementSpec::getPort);
    }

    /* ---------- END of configuration of first-class citizen fields ---------- */

    /**
     * Assume the specified first-class citizens are not included in the general server configuration
     *
     * @param status                    Status of the deployment
     */
    protected void assumeFirstClassCitizens(Keycloak keycloakCR, KeycloakStatusAggregator status) {
        final var serverConfigNames = keycloakCR
                .getSpec()
                .getAdditionalOptions()
                .stream()
                .map(ValueOrSecret::getName)
                .collect(Collectors.toCollection(HashSet::new));

        serverConfigNames.retainAll(firstClassConfigOptions.keySet());
        if (CollectionUtil.isNotEmpty(serverConfigNames)) {
            status.addWarningMessage("You need to specify these fields as the first-class citizen of the CR: "
                    + CollectionUtil.join(serverConfigNames, ","));
        }
    }

    public static String getKeycloakOptionEnvVarName(String kcConfigName) {
        // TODO make this use impl from Quarkus dist (Configuration.toEnvVarFormat)
        return KC_PREFIX + replaceNonAlphanumericByUnderscores(kcConfigName).toUpperCase();
    }

    private <T> OptionMapper<T> optionMapper(Function<Keycloak, T> optionSpec) {
        return new OptionMapper<>(optionSpec);
    }

    private class OptionMapper<T> {

        private class Mapper<R> {
            Function<T, R> optionValueSupplier;

            public Mapper(Function<T, R> optionValueSupplier) {
                this.optionValueSupplier = optionValueSupplier;
            }

            void map(String optionName, Keycloak keycloak, List<EnvVar> variables) {
                var categorySpec = optionSpec.apply(keycloak);

                if (categorySpec == null) {
                    Log.debugf("No category spec provided for %s", optionName);
                    return;
                }

                R value = optionValueSupplier.apply(categorySpec);

                if (value == null || value.toString().trim().isEmpty()) {
                    Log.debugf("No value provided for %s", optionName);
                    return;
                }

                EnvVarBuilder envVarBuilder = new EnvVarBuilder()
                        .withName(getKeycloakOptionEnvVarName(optionName));

                if (value instanceof SecretKeySelector) {
                    envVarBuilder.withValueFrom(new EnvVarSourceBuilder().withSecretKeyRef((SecretKeySelector) value).build());
                } else {
                    envVarBuilder.withValue(String.valueOf(value));
                }

                variables.add(envVarBuilder.build());
            }
        }

        private final Function<Keycloak, T> optionSpec;

        public OptionMapper(Function<Keycloak, T> optionSpec) {
            this.optionSpec = optionSpec;
        }

        public <R> OptionMapper<T> mapOption(String optionName, Function<T, R> optionValueSupplier) {
            firstClassConfigOptions.put(optionName, new Mapper<>(optionValueSupplier));
            return this;
        }

        protected <R extends Collection<?>> OptionMapper<T> mapOptionFromCollection(String optionName, Function<T, R> optionValueSupplier) {
            return mapOption(optionName, s -> {
                var value = optionValueSupplier.apply(s);
                if (value == null) {
                    return null;
                }
                return value.stream().filter(Objects::nonNull).map(String::valueOf).collect(Collectors.joining(","));
            });
        }
    }

    @SuppressWarnings("unchecked")
    public List<EnvVar> configureDistOptions(Keycloak keycloakCR) {
        List<EnvVar> result = new ArrayList<>();
        firstClassConfigOptions.entrySet().forEach(e -> e.getValue().map(e.getKey(), keycloakCR, result));
        return result;
    }
}
