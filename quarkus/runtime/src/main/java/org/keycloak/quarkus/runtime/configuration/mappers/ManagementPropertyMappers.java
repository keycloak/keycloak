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

import org.keycloak.config.HttpOptions;
import org.keycloak.config.ManagementOptions;
import org.keycloak.quarkus.runtime.Messages;
import org.keycloak.quarkus.runtime.cli.PropertyException;
import org.keycloak.quarkus.runtime.configuration.Configuration;

import static org.keycloak.quarkus.runtime.configuration.Configuration.isTrue;
import static org.keycloak.quarkus.runtime.configuration.mappers.PropertyMapper.fromOption;

public class ManagementPropertyMappers {
    private static final String MANAGEMENT_ENABLED_MSG = "Management interface is enabled";

    private ManagementPropertyMappers() {
    }

    public static PropertyMapper<?>[] getManagementPropertyMappers() {
        return new PropertyMapper[]{
                fromOption(ManagementOptions.MANAGEMENT_ENABLED)
                        .to("quarkus.management.enabled")
                        .paramLabel(Boolean.TRUE + "|" + Boolean.FALSE)
                        .build(),
                fromOption(ManagementOptions.MANAGEMENT_RELATIVE_PATH)
                        .isEnabled(ManagementPropertyMappers::isManagementEnabled, MANAGEMENT_ENABLED_MSG)
                        .to("quarkus.management.root-path")
                        .paramLabel("path")
                        .build(),
                fromOption(ManagementOptions.MANAGEMENT_PORT)
                        .isEnabled(ManagementPropertyMappers::isManagementEnabled, MANAGEMENT_ENABLED_MSG)
                        .to("quarkus.management.port")
                        .paramLabel("port")
                        .build(),
                fromOption(ManagementOptions.MANAGEMENT_HOST)
                        .isEnabled(ManagementPropertyMappers::isManagementEnabled, MANAGEMENT_ENABLED_MSG)
                        .mapFrom(HttpOptions.HTTP_HOST.getKey())
                        .to("quarkus.management.host")
                        .paramLabel("host")
                        .build(),
                // HTTPS
                fromOption(ManagementOptions.MANAGEMENT_HTTPS_CLIENT_AUTH)
                        .isEnabled(ManagementPropertyMappers::isManagementEnabled, MANAGEMENT_ENABLED_MSG)
                        .mapFrom(HttpOptions.HTTPS_CLIENT_AUTH.getKey())
                        .to("quarkus.management.ssl.client-auth")
                        .paramLabel("auth")
                        .build(),
                fromOption(ManagementOptions.MANAGEMENT_HTTPS_CIPHER_SUITES)
                        .isEnabled(ManagementPropertyMappers::isManagementEnabled, MANAGEMENT_ENABLED_MSG)
                        .mapFrom(HttpOptions.HTTPS_CIPHER_SUITES.getKey())
                        .to("quarkus.management.ssl.cipher-suites")
                        .paramLabel("ciphers")
                        .build(),
                fromOption(ManagementOptions.MANAGEMENT_HTTPS_PROTOCOLS)
                        .isEnabled(ManagementPropertyMappers::isManagementEnabled, MANAGEMENT_ENABLED_MSG)
                        .mapFrom(HttpOptions.HTTPS_PROTOCOLS.getKey())
                        .to("quarkus.management.ssl.protocols")
                        .paramLabel("protocols")
                        .build(),
                fromOption(ManagementOptions.MANAGEMENT_HTTPS_CERTIFICATE_FILE)
                        .isEnabled(ManagementPropertyMappers::isManagementEnabled, MANAGEMENT_ENABLED_MSG)
                        .mapFrom(HttpOptions.HTTPS_CERTIFICATE_FILE.getKey())
                        .to("quarkus.management.ssl.certificate.files")
                        .validator((mapper, value) -> validateTlsProperties())
                        .paramLabel("file")
                        .build(),
                fromOption(ManagementOptions.MANAGEMENT_HTTPS_CERTIFICATE_KEY_FILE)
                        .isEnabled(ManagementPropertyMappers::isManagementEnabled, MANAGEMENT_ENABLED_MSG)
                        .mapFrom(HttpOptions.HTTPS_CERTIFICATE_KEY_FILE.getKey())
                        .to("quarkus.management.ssl.certificate.key-files")
                        .validator((mapper, value) -> validateTlsProperties())
                        .paramLabel("file")
                        .build(),
                fromOption(ManagementOptions.MANAGEMENT_HTTPS_KEY_STORE_FILE)
                        .isEnabled(ManagementPropertyMappers::isManagementEnabled, MANAGEMENT_ENABLED_MSG)
                        .mapFrom(HttpOptions.HTTPS_KEY_STORE_FILE.getKey())
                        .to("quarkus.management.ssl.certificate.key-store-file")
                        .validator((mapper, value) -> validateTlsProperties())
                        .paramLabel("file")
                        .build(),
                fromOption(ManagementOptions.MANAGEMENT_HTTPS_KEY_STORE_PASSWORD)
                        .isEnabled(ManagementPropertyMappers::isManagementEnabled, MANAGEMENT_ENABLED_MSG)
                        .mapFrom(HttpOptions.HTTPS_KEY_STORE_PASSWORD.getKey())
                        .to("quarkus.management.ssl.certificate.key-store-password")
                        .validator((mapper, value) -> validateTlsProperties())
                        .paramLabel("password")
                        .isMasked(true)
                        .build(),
                fromOption(ManagementOptions.MANAGEMENT_HTTPS_KEY_STORE_TYPE)
                        .isEnabled(ManagementPropertyMappers::isManagementEnabled, MANAGEMENT_ENABLED_MSG)
                        .mapFrom(HttpOptions.HTTPS_KEY_STORE_TYPE.getKey())
                        .to("quarkus.management.ssl.certificate.key-store-file-type")
                        .transformer((value, config) -> value.or(() -> Configuration.getOptionalKcValue(HttpOptions.HTTPS_KEY_STORE_TYPE.getKey())))
                        .paramLabel("type")
                        .build(),
        };
    }

    public static boolean isManagementEnabled() {
        return isTrue(ManagementOptions.MANAGEMENT_ENABLED);
    }

    public static boolean isManagementTlsEnabled() {
        var key = Configuration.getOptionalKcValue(ManagementOptions.MANAGEMENT_HTTPS_CERTIFICATE_KEY_FILE.getKey());
        var cert = Configuration.getOptionalKcValue(ManagementOptions.MANAGEMENT_HTTPS_CERTIFICATE_FILE.getKey());
        if (key.isPresent() && cert.isPresent()) return true;

        var keystore = Configuration.getOptionalKcValue(ManagementOptions.MANAGEMENT_HTTPS_KEY_STORE_FILE.getKey());
        return keystore.isPresent();
    }

    private static void validateTlsProperties() {
        var isHttpEnabled = Configuration.isTrue(HttpOptions.HTTP_ENABLED);
        if (!isHttpEnabled && !isManagementTlsEnabled()) {
            throw new PropertyException(Messages.httpsConfigurationNotSet());
        }
    }
}
