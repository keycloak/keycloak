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

import io.smallrye.config.ConfigSourceInterceptorContext;
import org.keycloak.config.HttpOptions;
import org.keycloak.config.ManagementOptions;
import org.keycloak.quarkus.runtime.Messages;
import org.keycloak.quarkus.runtime.cli.PropertyException;
import org.keycloak.quarkus.runtime.configuration.Configuration;

import java.util.Optional;

import static org.keycloak.quarkus.runtime.configuration.Configuration.isTrue;
import static org.keycloak.quarkus.runtime.configuration.mappers.PropertyMapper.fromOption;

public class ManagementPropertyMappers {
    private static final String MANAGEMENT_ENABLED_MSG = "Management interface is enabled";

    private ManagementPropertyMappers() {
    }

    public static PropertyMapper<?>[] getManagementPropertyMappers() {
        return new PropertyMapper[]{
                fromOption(ManagementOptions.LEGACY_OBSERVABILITY_INTERFACE)
                        .to("quarkus.management.enabled") // ATM, the management interface state is only based on the legacy-observability-interface property
                        .paramLabel(Boolean.TRUE + "|" + Boolean.FALSE)
                        .transformer(ManagementPropertyMappers::managementEnabledTransformer)
                        .build(),
                fromOption(ManagementOptions.HTTP_MANAGEMENT_RELATIVE_PATH)
                        .isEnabled(ManagementPropertyMappers::isManagementEnabled, MANAGEMENT_ENABLED_MSG)
                        .mapFrom(HttpOptions.HTTP_RELATIVE_PATH.getKey())
                        .to("quarkus.management.root-path")
                        .paramLabel("path")
                        .build(),
                fromOption(ManagementOptions.HTTP_MANAGEMENT_PORT)
                        .isEnabled(ManagementPropertyMappers::isManagementEnabled, MANAGEMENT_ENABLED_MSG)
                        .to("quarkus.management.port")
                        .paramLabel("port")
                        .build(),
                fromOption(ManagementOptions.HTTP_MANAGEMENT_HOST)
                        .isEnabled(ManagementPropertyMappers::isManagementEnabled, MANAGEMENT_ENABLED_MSG)
                        .mapFrom(HttpOptions.HTTP_HOST.getKey())
                        .to("quarkus.management.host")
                        .paramLabel("host")
                        .build(),
                // HTTPS
                fromOption(ManagementOptions.HTTPS_MANAGEMENT_CLIENT_AUTH)
                        .isEnabled(ManagementPropertyMappers::isManagementEnabled, MANAGEMENT_ENABLED_MSG)
                        .mapFrom(HttpOptions.HTTPS_CLIENT_AUTH.getKey())
                        .to("quarkus.management.ssl.client-auth")
                        .paramLabel("auth")
                        .build(),
                fromOption(ManagementOptions.HTTPS_MANAGEMENT_CIPHER_SUITES)
                        .isEnabled(ManagementPropertyMappers::isManagementEnabled, MANAGEMENT_ENABLED_MSG)
                        .mapFrom(HttpOptions.HTTPS_CIPHER_SUITES.getKey())
                        .to("quarkus.management.ssl.cipher-suites")
                        .paramLabel("ciphers")
                        .build(),
                fromOption(ManagementOptions.HTTPS_MANAGEMENT_PROTOCOLS)
                        .isEnabled(ManagementPropertyMappers::isManagementEnabled, MANAGEMENT_ENABLED_MSG)
                        .mapFrom(HttpOptions.HTTPS_PROTOCOLS.getKey())
                        .to("quarkus.management.ssl.protocols")
                        .paramLabel("protocols")
                        .build(),
                fromOption(ManagementOptions.HTTPS_MANAGEMENT_CERTIFICATE_FILE)
                        .isEnabled(ManagementPropertyMappers::isManagementEnabled, MANAGEMENT_ENABLED_MSG)
                        .mapFrom(HttpOptions.HTTPS_CERTIFICATE_FILE.getKey())
                        .to("quarkus.management.ssl.certificate.files")
                        .validator((mapper, value) -> validateTlsProperties())
                        .paramLabel("file")
                        .build(),
                fromOption(ManagementOptions.HTTPS_MANAGEMENT_CERTIFICATE_KEY_FILE)
                        .isEnabled(ManagementPropertyMappers::isManagementEnabled, MANAGEMENT_ENABLED_MSG)
                        .mapFrom(HttpOptions.HTTPS_CERTIFICATE_KEY_FILE.getKey())
                        .to("quarkus.management.ssl.certificate.key-files")
                        .validator((mapper, value) -> validateTlsProperties())
                        .paramLabel("file")
                        .build(),
                fromOption(ManagementOptions.HTTPS_MANAGEMENT_KEY_STORE_FILE)
                        .isEnabled(ManagementPropertyMappers::isManagementEnabled, MANAGEMENT_ENABLED_MSG)
                        .mapFrom(HttpOptions.HTTPS_KEY_STORE_FILE.getKey())
                        .to("quarkus.management.ssl.certificate.key-store-file")
                        .validator((mapper, value) -> validateTlsProperties())
                        .paramLabel("file")
                        .build(),
                fromOption(ManagementOptions.HTTPS_MANAGEMENT_KEY_STORE_PASSWORD)
                        .isEnabled(ManagementPropertyMappers::isManagementEnabled, MANAGEMENT_ENABLED_MSG)
                        .mapFrom(HttpOptions.HTTPS_KEY_STORE_PASSWORD.getKey())
                        .to("quarkus.management.ssl.certificate.key-store-password")
                        .validator((mapper, value) -> validateTlsProperties())
                        .paramLabel("password")
                        .isMasked(true)
                        .build(),
                fromOption(ManagementOptions.HTTPS_MANAGEMENT_KEY_STORE_TYPE)
                        .isEnabled(ManagementPropertyMappers::isManagementEnabled, MANAGEMENT_ENABLED_MSG)
                        .mapFrom(HttpOptions.HTTPS_KEY_STORE_TYPE.getKey())
                        .to("quarkus.management.ssl.certificate.key-store-file-type")
                        .transformer((value, config) -> value.or(() -> Configuration.getOptionalKcValue(HttpOptions.HTTPS_KEY_STORE_TYPE.getKey())))
                        .paramLabel("type")
                        .build(),
        };
    }

    public static boolean isManagementEnabled() {
        return isTrue("quarkus.management.enabled");
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

    private static Optional<String> managementEnabledTransformer(Optional<String> value, ConfigSourceInterceptorContext ctx) {
        if (value.isPresent()) {
            var b = Boolean.parseBoolean(value.get());
            return Optional.of(Boolean.toString(!b)); // negate the output
        }
        return Optional.of(Boolean.TRUE.toString());
    }
}
