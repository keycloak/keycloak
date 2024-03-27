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
package org.keycloak.config;

import java.io.File;
import java.util.List;

/**
 * Options for the management interface that handles management endpoints (f.e. health and metrics endpoints)
 */
public class ManagementOptions {

    public static final Option<Boolean> MANAGEMENT_ENABLED = new OptionBuilder<>("management-enabled", Boolean.class)
            .category(OptionCategory.MANAGEMENT)
            .description("If separate interface/port should be used for exposing the management endpoints.")
            .defaultValue(Boolean.TRUE)
            .buildTime(true)
            .build();

    public static final Option<String> MANAGEMENT_RELATIVE_PATH = new OptionBuilder<>("management-relative-path", String.class)
            .category(OptionCategory.MANAGEMENT)
            .description("Set the path relative to '/' for serving resources from management interface. The path must start with a '/'.")
            .defaultValue("/")
            .buildTime(true)
            .build();

    public static final Option<Integer> MANAGEMENT_PORT = new OptionBuilder<>("management-port", Integer.class)
            .category(OptionCategory.MANAGEMENT)
            .description("Port of the management interface.")
            .defaultValue(9000)
            .build();

    public static final Option<String> MANAGEMENT_HOST = new OptionBuilder<>("management-host", String.class)
            .hidden()
            .category(OptionCategory.MANAGEMENT)
            .description("Host of the management interface.")
            .defaultValue("0.0.0.0")
            .build();

    //HTTPS
    public static final Option<HttpOptions.ClientAuth> MANAGEMENT_HTTPS_CLIENT_AUTH = new OptionBuilder<>("management-https-client-auth", HttpOptions.ClientAuth.class)
            .category(OptionCategory.MANAGEMENT)
            .description("Configures the management interface to require/request client authentication.")
            .defaultValue(HttpOptions.ClientAuth.none)
            .buildTime(true)
            .build();

    public static final Option<String> MANAGEMENT_HTTPS_CIPHER_SUITES = new OptionBuilder<>("management-https-cipher-suites", String.class)
            .hidden()
            .category(OptionCategory.MANAGEMENT)
            .description("The cipher suites to use for the management server. If none is given, a reasonable default is selected.")
            .hidden()
            .build();

    public static final Option<List<String>> MANAGEMENT_HTTPS_PROTOCOLS = OptionBuilder.listOptionBuilder("management-https-protocols", String.class)
            .hidden()
            .category(OptionCategory.MANAGEMENT)
            .description("The list of protocols to explicitly enable for the management server.")
            .defaultValue(List.of("TLSv1.3,TLSv1.2"))
            .hidden()
            .build();

    public static final Option<File> MANAGEMENT_HTTPS_CERTIFICATE_FILE = new OptionBuilder<>("management-https-certificate-file", File.class)
            .category(OptionCategory.MANAGEMENT)
            .description("The file path to a server certificate or certificate chain in PEM format for the management server.")
            .build();

    public static final Option<File> MANAGEMENT_HTTPS_CERTIFICATE_KEY_FILE = new OptionBuilder<>("management-https-certificate-key-file", File.class)
            .category(OptionCategory.MANAGEMENT)
            .description("The file path to a private key in PEM format for the management server.")
            .build();

    public static final Option<File> MANAGEMENT_HTTPS_KEY_STORE_FILE = new OptionBuilder<>("management-https-key-store-file", File.class)
            .category(OptionCategory.MANAGEMENT)
            .description("The key store which holds the certificate information instead of specifying separate files for the management server.")
            .build();

    public static final Option<String> MANAGEMENT_HTTPS_KEY_STORE_PASSWORD = new OptionBuilder<>("management-https-key-store-password", String.class)
            .category(OptionCategory.MANAGEMENT)
            .description("The password of the key store file for the management server.")
            .defaultValue("password")
            .build();

    public static final Option<String> MANAGEMENT_HTTPS_KEY_STORE_TYPE = new OptionBuilder<>("management-https-key-store-type", String.class)
            .hidden()
            .category(OptionCategory.MANAGEMENT)
            .description("The type of the key store file for the management server. " +
                    "If not given, the type is automatically detected based on the file name.")
            .build();
}
