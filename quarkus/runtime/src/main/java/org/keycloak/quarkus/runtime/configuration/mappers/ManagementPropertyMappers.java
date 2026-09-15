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
package org.keycloak.quarkus.runtime.configuration.mappers;

import java.util.ArrayList;
import java.util.List;

import org.keycloak.config.HealthOptions;
import org.keycloak.config.HttpOptions;
import org.keycloak.config.ManagementOptions;
import org.keycloak.config.ManagementOptions.Scheme;
import org.keycloak.config.MetricsOptions;
import org.keycloak.config.OpenApiOptions;
import org.keycloak.config.Option;
import org.keycloak.config.OptionBuilder;
import org.keycloak.quarkus.runtime.configuration.Configuration;

import io.smallrye.config.ConfigSourceInterceptorContext;

import static org.keycloak.config.ManagementOptions.LEGACY_OBSERVABILITY_INTERFACE;
import static org.keycloak.quarkus.runtime.configuration.Configuration.getOptionalKcValue;
import static org.keycloak.quarkus.runtime.configuration.Configuration.isTrue;
import static org.keycloak.quarkus.runtime.configuration.mappers.HttpPropertyMappers.StoreType;
import static org.keycloak.quarkus.runtime.configuration.mappers.PropertyMapper.fromOption;

public class ManagementPropertyMappers implements PropertyMapperGrouping {

    private static final String HTTP_MANAGEMENT_SCHEME_IS_INHERITED = "http-management-scheme is inherited";

    static final String MGMT_TLS_BUCKET = "keycloak-management-server";
    public static final String MGMT_TLS_PREFIX = "quarkus.tls.\"" + MGMT_TLS_BUCKET + "\".";

    private static final Option<String> SYNTHETIC_MGMT_TLS_CONFIG_NAME = new OptionBuilder<>("https-management-tls-config-name-hidden", String.class)
            .buildTime(false)
            .synthetic()
            .build();

    @Override
    public List<PropertyMapper<?>> getPropertyMappers() {
        var mappers = new ArrayList<PropertyMapper<?>>();

        mappers.addAll(List.of(
                fromOption(ManagementOptions.HTTP_MANAGEMENT_ENABLED)
                        .to("quarkus.management.enabled")
                        .transformer((val, ctx) -> managementEnabledTransformer())
                        .build(),
                fromOption(ManagementOptions.HTTP_MANAGEMENT_HEALTH_ENABLED)
                        .to("quarkus.smallrye-health.management.enabled")
                        .isEnabled(() -> isTrue(HealthOptions.HEALTH_ENABLED), "health is enabled")
                        .build(),
                fromOption(ManagementOptions.LEGACY_OBSERVABILITY_INTERFACE)
                        .build(),
                fromOption(ManagementOptions.HTTP_MANAGEMENT_RELATIVE_PATH)
                        .mapFrom(HttpOptions.HTTP_RELATIVE_PATH)
                        .to("quarkus.management.root-path")
                        .paramLabel("path")
                        .build(),
                fromOption(ManagementOptions.HTTP_MANAGEMENT_PORT)
                        .to("quarkus.management.port")
                        .paramLabel("port")
                        .build(),
                fromOption(ManagementOptions.HTTP_MANAGEMENT_HOST)
                        .mapFrom(HttpOptions.HTTP_HOST)
                        .to("quarkus.management.host")
                        .paramLabel("host")
                        .build(),
                fromOption(ManagementOptions.HTTP_MANAGEMENT_SCHEME)
                        .paramLabel("scheme")
                        .build(),
                fromOption(ManagementOptions.HTTPS_MANAGEMENT_CLIENT_AUTH)
                        .mapFrom(HttpOptions.HTTPS_CLIENT_AUTH) // we can't check inherited because this is a build time option
                        .to("quarkus.management.ssl.client-auth")
                        .paramLabel("auth")
                        .build()
        ));

        mappers.add(
                fromOption(SYNTHETIC_MGMT_TLS_CONFIG_NAME)
                        .to("quarkus.management.tls-configuration-name")
                        .transformer((v, c) -> isManagementTlsEnabled() || (isInheritedScheme() && HttpPropertyMappers.isHttpsEnabled()) ? MGMT_TLS_BUCKET : null)
                        .build()
        );

        mappers.addAll(List.of(
                fromOption(ManagementOptions.HTTPS_MANAGEMENT_CIPHER_SUITES)
                        .isEnabled(ManagementPropertyMappers::isInheritedScheme, HTTP_MANAGEMENT_SCHEME_IS_INHERITED)
                        .mapFrom(HttpOptions.HTTPS_CIPHER_SUITES)
                        .to(MGMT_TLS_PREFIX + "cipher-suites")
                        .paramLabel("ciphers")
                        .build(),
                fromOption(ManagementOptions.HTTPS_MANAGEMENT_PROTOCOLS)
                        .isEnabled(ManagementPropertyMappers::isInheritedScheme, HTTP_MANAGEMENT_SCHEME_IS_INHERITED)
                        .mapFrom(HttpOptions.HTTPS_PROTOCOLS)
                        .to(MGMT_TLS_PREFIX + "protocols")
                        .paramLabel("protocols")
                        .build(),
                fromOption(ManagementOptions.HTTPS_MANAGEMENT_CERTIFICATES_RELOAD_PERIOD)
                        .isEnabled(ManagementPropertyMappers::isInheritedScheme, HTTP_MANAGEMENT_SCHEME_IS_INHERITED)
                        .mapFrom(HttpOptions.HTTPS_CERTIFICATES_RELOAD_PERIOD)
                        .to(MGMT_TLS_PREFIX + "reload-period")
                        .transformer(HttpPropertyMappers::transformNegativeReloadPeriod)
                        .paramLabel("reload period")
                        .build()
        ));

        mappers.addAll(List.of(
                fromOption(ManagementOptions.HTTPS_MANAGEMENT_CERTIFICATE_FILE)
                        .isEnabled(ManagementPropertyMappers::isInheritedScheme, HTTP_MANAGEMENT_SCHEME_IS_INHERITED)
                        .mapFrom(HttpOptions.HTTPS_CERTIFICATE_FILE, ManagementPropertyMappers::suppressInheritedPemWhenKeystoreSet)
                        .to(MGMT_TLS_PREFIX + "key-store.pem.default.cert")
                        .paramLabel("file")
                        .build(),
                fromOption(ManagementOptions.HTTPS_MANAGEMENT_CERTIFICATE_KEY_FILE)
                        .isEnabled(ManagementPropertyMappers::isInheritedScheme, HTTP_MANAGEMENT_SCHEME_IS_INHERITED)
                        .mapFrom(HttpOptions.HTTPS_CERTIFICATE_KEY_FILE, ManagementPropertyMappers::suppressInheritedPemWhenKeystoreSet)
                        .to(MGMT_TLS_PREFIX + "key-store.pem.default.key")
                        .paramLabel("file")
                        .build(),
                fromOption(ManagementOptions.HTTPS_MANAGEMENT_CERTIFICATE_KEY_FILE_PASSWORD)
                        .isEnabled(ManagementPropertyMappers::isInheritedScheme, HTTP_MANAGEMENT_SCHEME_IS_INHERITED)
                        .mapFrom(HttpOptions.HTTPS_CERTIFICATE_KEY_FILE_PASSWORD, ManagementPropertyMappers::suppressInheritedPemWhenKeystoreSet)
                        .to(MGMT_TLS_PREFIX + "key-store.pem.default.password")
                        .paramLabel("password")
                        .isMasked(true)
                        .build()
        ));

        addMgmtKeyStoreMappers(mappers);
        addMgmtTrustStoreMappers(mappers);

        return mappers;
    }

    private void addMgmtKeyStoreMappers(List<PropertyMapper<?>> mappers) {
        mappers.add(
                fromOption(ManagementOptions.HTTPS_MANAGEMENT_KEY_STORE_FILE)
                        .isEnabled(ManagementPropertyMappers::isInheritedScheme, HTTP_MANAGEMENT_SCHEME_IS_INHERITED)
                        .mapFrom(HttpOptions.HTTPS_KEY_STORE_FILE)
                        .paramLabel("file")
                        .build()
        );

        for (StoreType type : StoreType.values()) {
            if (type == StoreType.PEM) continue;
            mappers.add(
                    fromOption(ManagementOptions.HTTPS_MANAGEMENT_KEY_STORE_FILE)
                            .isEnabled(ManagementPropertyMappers::isInheritedScheme, HTTP_MANAGEMENT_SCHEME_IS_INHERITED)
                            .mapFrom(ManagementOptions.HTTPS_MANAGEMENT_KEY_STORE_FILE, (name, fileValue, context) ->
                                    HttpPropertyMappers.dispatchStoreFile(fileValue, ManagementOptions.HTTPS_MANAGEMENT_KEY_STORE_TYPE, type,
                                            HttpPropertyMappers.StoreRole.KEY_STORE, ManagementOptions.HTTPS_MANAGEMENT_CERTIFICATE_FILE))
                            .to(MGMT_TLS_PREFIX + HttpPropertyMappers.keyStoreSubPath(type, "path"))
                            .paramLabel("file")
                            .build()
            );
        }

        mappers.add(
                fromOption(ManagementOptions.HTTPS_MANAGEMENT_KEY_STORE_PASSWORD)
                        .isEnabled(ManagementPropertyMappers::isInheritedScheme, HTTP_MANAGEMENT_SCHEME_IS_INHERITED)
                        .mapFrom(HttpOptions.HTTPS_KEY_STORE_PASSWORD)
                        .paramLabel("password")
                        .isMasked(true)
                        .build()
        );

        for (StoreType type : StoreType.values()) {
            if (type == StoreType.PEM) continue;
            mappers.add(
                    fromOption(ManagementOptions.HTTPS_MANAGEMENT_KEY_STORE_PASSWORD)
                            .isEnabled(ManagementPropertyMappers::isInheritedScheme, HTTP_MANAGEMENT_SCHEME_IS_INHERITED)
                            .mapFrom(ManagementOptions.HTTPS_MANAGEMENT_KEY_STORE_PASSWORD, (name, passwordValue, context) ->
                                    HttpPropertyMappers.dispatchStoreProperty(passwordValue,
                                            ManagementOptions.HTTPS_MANAGEMENT_KEY_STORE_FILE,
                                            ManagementOptions.HTTPS_MANAGEMENT_KEY_STORE_TYPE, type,
                                            HttpPropertyMappers.StoreRole.KEY_STORE, ManagementOptions.HTTPS_MANAGEMENT_CERTIFICATE_FILE))
                            .to(MGMT_TLS_PREFIX + HttpPropertyMappers.keyStoreSubPath(type, "password"))
                            .paramLabel("password")
                            .isMasked(true)
                            .build()
            );
        }

        mappers.add(
                fromOption(ManagementOptions.HTTPS_MANAGEMENT_KEY_STORE_TYPE)
                        .isEnabled(ManagementPropertyMappers::isInheritedScheme, HTTP_MANAGEMENT_SCHEME_IS_INHERITED)
                        .mapFrom(HttpOptions.HTTPS_KEY_STORE_TYPE)
                        .paramLabel("type")
                        .build()
        );

        mappers.add(
                fromOption(ManagementOptions.HTTPS_MANAGEMENT_KEY_STORE_TYPE)
                        .isEnabled(ManagementPropertyMappers::isInheritedScheme, HTTP_MANAGEMENT_SCHEME_IS_INHERITED)
                        .mapFrom(ManagementOptions.HTTPS_MANAGEMENT_KEY_STORE_TYPE, (name, value, context) ->
                                HttpPropertyMappers.filterOtherStoreType(value, ManagementOptions.HTTPS_MANAGEMENT_KEY_STORE_FILE, ManagementOptions.HTTPS_MANAGEMENT_CERTIFICATE_FILE))
                        .to(MGMT_TLS_PREFIX + "key-store.other.type")
                        .paramLabel("type")
                        .build()
        );
    }

    private void addMgmtTrustStoreMappers(List<PropertyMapper<?>> mappers) {
        mappers.add(
                fromOption(ManagementOptions.HTTPS_MANAGEMENT_TRUST_STORE_FILE)
                        .isEnabled(ManagementPropertyMappers::isInheritedScheme, HTTP_MANAGEMENT_SCHEME_IS_INHERITED)
                        .mapFrom(HttpOptions.HTTPS_TRUST_STORE_FILE)
                        .paramLabel("file")
                        .build()
        );

        for (StoreType type : StoreType.values()) {
            mappers.add(
                    fromOption(ManagementOptions.HTTPS_MANAGEMENT_TRUST_STORE_FILE)
                            .isEnabled(ManagementPropertyMappers::isInheritedScheme, HTTP_MANAGEMENT_SCHEME_IS_INHERITED)
                            .mapFrom(ManagementOptions.HTTPS_MANAGEMENT_TRUST_STORE_FILE, (name, fileValue, context) ->
                                    HttpPropertyMappers.dispatchStoreFile(fileValue, ManagementOptions.HTTPS_MANAGEMENT_TRUST_STORE_TYPE, type, HttpPropertyMappers.StoreRole.TRUST_STORE))
                            .to(MGMT_TLS_PREFIX + HttpPropertyMappers.trustStoreSubPath(type, "path"))
                            .paramLabel("file")
                            .build()
            );
        }

        mappers.add(
                fromOption(ManagementOptions.HTTPS_MANAGEMENT_TRUST_STORE_PASSWORD)
                        .isEnabled(ManagementPropertyMappers::isInheritedScheme, HTTP_MANAGEMENT_SCHEME_IS_INHERITED)
                        .mapFrom(HttpOptions.HTTPS_TRUST_STORE_PASSWORD)
                        .paramLabel("password")
                        .isMasked(true)
                        .build()
        );

        for (StoreType type : StoreType.values()) {
            if (type == StoreType.PEM) continue;
            mappers.add(
                    fromOption(ManagementOptions.HTTPS_MANAGEMENT_TRUST_STORE_PASSWORD)
                            .isEnabled(ManagementPropertyMappers::isInheritedScheme, HTTP_MANAGEMENT_SCHEME_IS_INHERITED)
                            .mapFrom(ManagementOptions.HTTPS_MANAGEMENT_TRUST_STORE_PASSWORD, (name, passwordValue, context) ->
                                    HttpPropertyMappers.dispatchStoreProperty(passwordValue,
                                            ManagementOptions.HTTPS_MANAGEMENT_TRUST_STORE_FILE,
                                            ManagementOptions.HTTPS_MANAGEMENT_TRUST_STORE_TYPE, type, HttpPropertyMappers.StoreRole.TRUST_STORE))
                            .to(MGMT_TLS_PREFIX + HttpPropertyMappers.trustStoreSubPath(type, "password"))
                            .paramLabel("password")
                            .isMasked(true)
                            .build()
            );
        }

        mappers.add(
                fromOption(ManagementOptions.HTTPS_MANAGEMENT_TRUST_STORE_TYPE)
                        .isEnabled(ManagementPropertyMappers::isInheritedScheme, HTTP_MANAGEMENT_SCHEME_IS_INHERITED)
                        .mapFrom(HttpOptions.HTTPS_TRUST_STORE_TYPE)
                        .paramLabel("type")
                        .build()
        );

        mappers.add(
                fromOption(ManagementOptions.HTTPS_MANAGEMENT_TRUST_STORE_TYPE)
                        .isEnabled(ManagementPropertyMappers::isInheritedScheme, HTTP_MANAGEMENT_SCHEME_IS_INHERITED)
                        .mapFrom(ManagementOptions.HTTPS_MANAGEMENT_TRUST_STORE_TYPE, (name, value, context) ->
                                HttpPropertyMappers.filterOtherStoreType(value, ManagementOptions.HTTPS_MANAGEMENT_TRUST_STORE_FILE))
                        .to(MGMT_TLS_PREFIX + "trust-store.other.type")
                        .paramLabel("type")
                        .build()
        );
    }

    public static boolean isManagementEnabled() {
        if (isTrue(LEGACY_OBSERVABILITY_INTERFACE)) {
            return false;
        }
        return (isTrue(HealthOptions.HEALTH_ENABLED) && isTrue(ManagementOptions.HTTP_MANAGEMENT_HEALTH_ENABLED))
            || isTrue(MetricsOptions.METRICS_ENABLED)
            || isTrue(OpenApiOptions.OPENAPI_ENABLED);
    }

    private static String managementEnabledTransformer() {
        return Boolean.toString(isManagementEnabled());
    }

    public static boolean isInheritedScheme() {
        return !Scheme.http.name()
                .equals(Configuration.getKcConfigValue(ManagementOptions.HTTP_MANAGEMENT_SCHEME.getKey()).getValue());
    }

    private static String suppressInheritedPemWhenKeystoreSet(String value,
            ConfigSourceInterceptorContext context) {
        if (Configuration.isSet(ManagementOptions.HTTPS_MANAGEMENT_KEY_STORE_FILE)) {
            return null;
        }
        return value;
    }

    public static boolean isManagementTlsEnabled() {
        if (isInheritedScheme()) {
            var key = getOptionalKcValue(ManagementOptions.HTTPS_MANAGEMENT_CERTIFICATE_KEY_FILE.getKey());
            var cert = getOptionalKcValue(ManagementOptions.HTTPS_MANAGEMENT_CERTIFICATE_FILE.getKey());
            if (key.isPresent() && cert.isPresent()) {
                return true;
            }

            var keystore = getOptionalKcValue(ManagementOptions.HTTPS_MANAGEMENT_KEY_STORE_FILE.getKey());
            return keystore.isPresent();
        }
        return false;
    }

}
