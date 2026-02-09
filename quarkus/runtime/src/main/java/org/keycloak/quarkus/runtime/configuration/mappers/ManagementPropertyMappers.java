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

import java.util.List;

import org.keycloak.config.HealthOptions;
import org.keycloak.config.HttpOptions;
import org.keycloak.config.ManagementOptions;
import org.keycloak.config.ManagementOptions.Scheme;
import org.keycloak.config.MetricsOptions;
import org.keycloak.config.OpenApiOptions;
import org.keycloak.quarkus.runtime.configuration.Configuration;

import static org.keycloak.config.ManagementOptions.LEGACY_OBSERVABILITY_INTERFACE;
import static org.keycloak.quarkus.runtime.configuration.Configuration.isTrue;
import static org.keycloak.quarkus.runtime.configuration.mappers.PropertyMapper.fromOption;

public class ManagementPropertyMappers implements PropertyMapperGrouping {

    private static final String HTTP_MANAGEMENT_SCHEME_IS_INHERITED = "http-management-scheme is inherited";

    @Override
    public List<PropertyMapper<?>> getPropertyMappers() {
        return List.of(
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
                // HTTPS
                fromOption(ManagementOptions.HTTP_MANAGEMENT_SCHEME)
                        .paramLabel("scheme")
                        .build(),
                fromOption(ManagementOptions.HTTPS_MANAGEMENT_CLIENT_AUTH)
                        .mapFrom(HttpOptions.HTTPS_CLIENT_AUTH) // we can't check inherited because this is a build time option
                        .to("quarkus.management.ssl.client-auth")
                        .paramLabel("auth")
                        .build(),
                fromOption(ManagementOptions.HTTPS_MANAGEMENT_CIPHER_SUITES)
                        .isEnabled(ManagementPropertyMappers::isInheritedScheme, HTTP_MANAGEMENT_SCHEME_IS_INHERITED)
                        .mapFrom(HttpOptions.HTTPS_CIPHER_SUITES)
                        .to("quarkus.management.ssl.cipher-suites")
                        .paramLabel("ciphers")
                        .build(),
                fromOption(ManagementOptions.HTTPS_MANAGEMENT_PROTOCOLS)
                        .isEnabled(ManagementPropertyMappers::isInheritedScheme, HTTP_MANAGEMENT_SCHEME_IS_INHERITED)
                        .mapFrom(HttpOptions.HTTPS_PROTOCOLS)
                        .to("quarkus.management.ssl.protocols")
                        .paramLabel("protocols")
                        .build(),
                fromOption(ManagementOptions.HTTPS_MANAGEMENT_CERTIFICATES_RELOAD_PERIOD)
                        .isEnabled(ManagementPropertyMappers::isInheritedScheme, HTTP_MANAGEMENT_SCHEME_IS_INHERITED)
                        .mapFrom(HttpOptions.HTTPS_CERTIFICATES_RELOAD_PERIOD)
                        .to("quarkus.management.ssl.certificate.reload-period")
                        .transformer(HttpPropertyMappers::transformNegativeReloadPeriod)
                        .paramLabel("reload period")
                        .build(),
                fromOption(ManagementOptions.HTTPS_MANAGEMENT_CERTIFICATE_FILE)
                        .isEnabled(ManagementPropertyMappers::isInheritedScheme, HTTP_MANAGEMENT_SCHEME_IS_INHERITED)
                        .mapFrom(HttpOptions.HTTPS_CERTIFICATE_FILE)
                        .to("quarkus.management.ssl.certificate.files")
                        .paramLabel("file")
                        .build(),
                fromOption(ManagementOptions.HTTPS_MANAGEMENT_CERTIFICATE_KEY_FILE)
                        .isEnabled(ManagementPropertyMappers::isInheritedScheme, HTTP_MANAGEMENT_SCHEME_IS_INHERITED)
                        .mapFrom(HttpOptions.HTTPS_CERTIFICATE_KEY_FILE)
                        .to("quarkus.management.ssl.certificate.key-files")
                        .paramLabel("file")
                        .build(),
                fromOption(ManagementOptions.HTTPS_MANAGEMENT_KEY_STORE_FILE)
                        .isEnabled(ManagementPropertyMappers::isInheritedScheme, HTTP_MANAGEMENT_SCHEME_IS_INHERITED)
                        .mapFrom(HttpOptions.HTTPS_KEY_STORE_FILE)
                        .to("quarkus.management.ssl.certificate.key-store-file")
                        .paramLabel("file")
                        .build(),
                fromOption(ManagementOptions.HTTPS_MANAGEMENT_KEY_STORE_PASSWORD)
                        .isEnabled(ManagementPropertyMappers::isInheritedScheme, HTTP_MANAGEMENT_SCHEME_IS_INHERITED)
                        .mapFrom(HttpOptions.HTTPS_KEY_STORE_PASSWORD)
                        .to("quarkus.management.ssl.certificate.key-store-password")
                        .paramLabel("password")
                        .isMasked(true)
                        .build(),
                fromOption(ManagementOptions.HTTPS_MANAGEMENT_KEY_STORE_TYPE)
                        .isEnabled(ManagementPropertyMappers::isInheritedScheme, HTTP_MANAGEMENT_SCHEME_IS_INHERITED)
                        .mapFrom(HttpOptions.HTTPS_KEY_STORE_TYPE)
                        .to("quarkus.management.ssl.certificate.key-store-file-type")
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

    public static boolean isManagementTlsEnabled() {
        if (isInheritedScheme()) {
            var key = Configuration.getOptionalKcValue(ManagementOptions.HTTPS_MANAGEMENT_CERTIFICATE_KEY_FILE.getKey());
            var cert = Configuration.getOptionalKcValue(ManagementOptions.HTTPS_MANAGEMENT_CERTIFICATE_FILE.getKey());
            if (key.isPresent() && cert.isPresent()) {
                return true;
            }

            var keystore = Configuration.getOptionalKcValue(ManagementOptions.HTTPS_MANAGEMENT_KEY_STORE_FILE.getKey());
            return keystore.isPresent();
        }
        return false;
    }

}
