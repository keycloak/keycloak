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

import org.keycloak.config.HealthOptions;
import org.keycloak.config.HttpOptions;
import org.keycloak.config.ManagementOptions;
import org.keycloak.config.MetricsOptions;
import org.keycloak.quarkus.runtime.Messages;
import org.keycloak.quarkus.runtime.cli.PropertyException;
import org.keycloak.quarkus.runtime.configuration.Configuration;

import static org.keycloak.config.ManagementOptions.LEGACY_OBSERVABILITY_INTERFACE;
import static org.keycloak.quarkus.runtime.configuration.Configuration.isTrue;
import static org.keycloak.quarkus.runtime.configuration.mappers.PropertyMapper.fromOption;

public class ManagementPropertyMappers {

    private ManagementPropertyMappers() {
    }

    public static PropertyMapper<?>[] getManagementPropertyMappers() {
        return new PropertyMapper[]{
                fromOption(ManagementOptions.HTTP_MANAGEMENT_ENABLED)
                        .to("quarkus.management.enabled")
                        .transformer((val, ctx) -> managementEnabledTransformer())
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
                fromOption(ManagementOptions.HTTPS_MANAGEMENT_CLIENT_AUTH)
                        .mapFrom(HttpOptions.HTTPS_CLIENT_AUTH)
                        .to("quarkus.management.ssl.client-auth")
                        .paramLabel("auth")
                        .build(),
                fromOption(ManagementOptions.HTTPS_MANAGEMENT_CIPHER_SUITES)
                        .mapFrom(HttpOptions.HTTPS_CIPHER_SUITES)
                        .to("quarkus.management.ssl.cipher-suites")
                        .paramLabel("ciphers")
                        .build(),
                fromOption(ManagementOptions.HTTPS_MANAGEMENT_PROTOCOLS)
                        .mapFrom(HttpOptions.HTTPS_PROTOCOLS)
                        .to("quarkus.management.ssl.protocols")
                        .paramLabel("protocols")
                        .build(),
                fromOption(ManagementOptions.HTTPS_MANAGEMENT_CERTIFICATES_RELOAD_PERIOD)
                        .mapFrom(HttpOptions.HTTPS_CERTIFICATES_RELOAD_PERIOD)
                        .to("quarkus.management.ssl.certificate.reload-period")
                        // -1 means no reload
                        .transformer((value, context) -> "-1".equals(value) ? null : value)
                        .paramLabel("reload period")
                        .build(),
                fromOption(ManagementOptions.HTTPS_MANAGEMENT_CERTIFICATE_FILE)
                        .mapFrom(HttpOptions.HTTPS_CERTIFICATE_FILE)
                        .to("quarkus.management.ssl.certificate.files")
                        .validator(value -> validateTlsProperties())
                        .paramLabel("file")
                        .build(),
                fromOption(ManagementOptions.HTTPS_MANAGEMENT_CERTIFICATE_KEY_FILE)
                        .mapFrom(HttpOptions.HTTPS_CERTIFICATE_KEY_FILE)
                        .to("quarkus.management.ssl.certificate.key-files")
                        .validator(value -> validateTlsProperties())
                        .paramLabel("file")
                        .build(),
                fromOption(ManagementOptions.HTTPS_MANAGEMENT_KEY_STORE_FILE)
                        .mapFrom(HttpOptions.HTTPS_KEY_STORE_FILE)
                        .to("quarkus.management.ssl.certificate.key-store-file")
                        .validator(value -> validateTlsProperties())
                        .paramLabel("file")
                        .build(),
                fromOption(ManagementOptions.HTTPS_MANAGEMENT_KEY_STORE_PASSWORD)
                        .mapFrom(HttpOptions.HTTPS_KEY_STORE_PASSWORD)
                        .to("quarkus.management.ssl.certificate.key-store-password")
                        .validator(value -> validateTlsProperties())
                        .paramLabel("password")
                        .isMasked(true)
                        .build(),
                fromOption(ManagementOptions.HTTPS_MANAGEMENT_KEY_STORE_TYPE)
                        .mapFrom(HttpOptions.HTTPS_KEY_STORE_TYPE)
                        .to("quarkus.management.ssl.certificate.key-store-file-type")
                        .paramLabel("type")
                        .build(),
        };
    }

    public static boolean isManagementEnabled() {
        if (isTrue(LEGACY_OBSERVABILITY_INTERFACE)) {
            return false;
        }
        var isManagementOccupied = isTrue(HealthOptions.HEALTH_ENABLED) || isTrue(MetricsOptions.METRICS_ENABLED);
        return isManagementOccupied;
    }

    private static String managementEnabledTransformer() {
        return Boolean.toString(isManagementEnabled());
    }

    public static boolean isManagementTlsEnabled() {
        var key = Configuration.getOptionalKcValue(ManagementOptions.HTTPS_MANAGEMENT_CERTIFICATE_KEY_FILE.getKey());
        var cert = Configuration.getOptionalKcValue(ManagementOptions.HTTPS_MANAGEMENT_CERTIFICATE_FILE.getKey());
        if (key.isPresent() && cert.isPresent()) return true;

        var keystore = Configuration.getOptionalKcValue(ManagementOptions.HTTPS_MANAGEMENT_KEY_STORE_FILE.getKey());
        return keystore.isPresent();
    }

    private static void validateTlsProperties() {
        var isHttpEnabled = Configuration.isTrue(HttpOptions.HTTP_ENABLED);
        if (!isHttpEnabled && !isManagementTlsEnabled()) {
            throw new PropertyException(Messages.httpsConfigurationNotSet());
        }
    }
}
