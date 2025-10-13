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

import static org.keycloak.config.OptionsUtil.DURATION_DESCRIPTION;

/**
 * Options for the management interface that handles management endpoints (f.e. health and metrics endpoints)
 */
public class ManagementOptions {

    public static final Option<Boolean> HTTP_MANAGEMENT_ENABLED = new OptionBuilder<>("http-management-enabled", Boolean.class)
            .category(OptionCategory.MANAGEMENT)
            .description("Placeholder for resolving state of the management interface. If set, the value is ignored.")
            .buildTime(true)
            .hidden()
            .build();

    public static final Option<Boolean> HTTP_MANAGEMENT_HEALTH_ENABLED = new OptionBuilder<>("http-management-health-enabled", Boolean.class)
            .category(OptionCategory.MANAGEMENT)
            .description("If health endpoints should be exposed on the management interface. If false, health endpoints will be exposed on the main interface.")
            .defaultValue(true)
            .buildTime(true)
            .build();

    public static final Option<Boolean> LEGACY_OBSERVABILITY_INTERFACE = new OptionBuilder<>("legacy-observability-interface", Boolean.class)
            .category(OptionCategory.MANAGEMENT)
            .deprecated()
            .description("If metrics/health endpoints should be exposed on the main HTTP server (not recommended). If set to true, the management interface is disabled.")
            .defaultValue(Boolean.FALSE)
            .buildTime(true)
            .build();

    static String RELEVANT_MSG = "Relevant only when something is exposed on the management interface - see the guide for details.";

    public static final Option<String> HTTP_MANAGEMENT_RELATIVE_PATH = new OptionBuilder<>("http-management-relative-path", String.class)
            .category(OptionCategory.MANAGEMENT)
            .description("Set the path relative to '/' for serving resources from management interface. The path must start with a '/'. If not given, the value is inherited from HTTP options. " + RELEVANT_MSG)
            .defaultValue("/")
            .buildTime(true)
            .build();

    public static final Option<Integer> HTTP_MANAGEMENT_PORT = new OptionBuilder<>("http-management-port", Integer.class)
            .category(OptionCategory.MANAGEMENT)
            .description("Port of the management interface. " + RELEVANT_MSG)
            .defaultValue(9000)
            .build();

    public static final Option<String> HTTP_MANAGEMENT_HOST = new OptionBuilder<>("http-management-host", String.class)
            .hidden()
            .category(OptionCategory.MANAGEMENT)
            .description("Host of the management interface. If not given, the value is inherited from HTTP options. " + RELEVANT_MSG)
            .defaultValue("0.0.0.0")
            .build();

    public enum Scheme {
        http,
        inherited
    }

    //HTTPS
    public static final Option<Scheme> HTTP_MANAGEMENT_SCHEME = new OptionBuilder<>("http-management-scheme", Scheme.class)
            .category(OptionCategory.MANAGEMENT)
            .description("Configures the management interface scheme. If 'inherited', the management interface will inherit the HTTPS settings of the main interface. If 'http', the management interface will be accessible via HTTP - it will not inherit HTTPS settings and cannot be configured for HTTPS.")
            .defaultValue(Scheme.inherited)
            .build();

    public static final Option<HttpOptions.ClientAuth> HTTPS_MANAGEMENT_CLIENT_AUTH = new OptionBuilder<>("https-management-client-auth", HttpOptions.ClientAuth.class)
            .category(OptionCategory.MANAGEMENT)
            .description("Configures the management interface to require/request client authentication. If not given, the value is inherited from HTTP options. " + RELEVANT_MSG)
            .defaultValue(HttpOptions.ClientAuth.none)
            .buildTime(true)
            .build();

    public static final Option<String> HTTPS_MANAGEMENT_CIPHER_SUITES = new OptionBuilder<>("https-management-cipher-suites", String.class)
            .hidden()
            .category(OptionCategory.MANAGEMENT)
            .description("The cipher suites to use for the management server. If not given, the value is inherited from HTTP options. " + RELEVANT_MSG)
            .hidden()
            .build();

    public static final Option<List<String>> HTTPS_MANAGEMENT_PROTOCOLS = OptionBuilder.listOptionBuilder("https-management-protocols", String.class)
            .hidden()
            .category(OptionCategory.MANAGEMENT)
            .description("The list of protocols to explicitly enable for the management server. If not given, the value is inherited from HTTP options. " + RELEVANT_MSG)
            .defaultValue(List.of("TLSv1.3,TLSv1.2"))
            .hidden()
            .build();

    public static final Option<String> HTTPS_MANAGEMENT_CERTIFICATES_RELOAD_PERIOD = new OptionBuilder<>("https-management-certificates-reload-period", String.class)
            .category(OptionCategory.MANAGEMENT)
            .description("Interval on which to reload key store, trust store, and certificate files referenced by https-management-* options for the management server. " +
                    DURATION_DESCRIPTION +
                    " Must be greater than 30 seconds. Use -1 to disable. " +
                    "If not given, the value is inherited from HTTP options. " + RELEVANT_MSG)
            .defaultValue("1h")
            .build();

    public static final Option<File> HTTPS_MANAGEMENT_CERTIFICATE_FILE = new OptionBuilder<>("https-management-certificate-file", File.class)
            .category(OptionCategory.MANAGEMENT)
            .description("The file path to a server certificate or certificate chain in PEM format for the management server. If not given, the value is inherited from HTTP options. " + RELEVANT_MSG)
            .build();

    public static final Option<File> HTTPS_MANAGEMENT_CERTIFICATE_KEY_FILE = new OptionBuilder<>("https-management-certificate-key-file", File.class)
            .category(OptionCategory.MANAGEMENT)
            .description("The file path to a private key in PEM format for the management server. If not given, the value is inherited from HTTP options. " + RELEVANT_MSG)
            .build();

    public static final Option<File> HTTPS_MANAGEMENT_KEY_STORE_FILE = new OptionBuilder<>("https-management-key-store-file", File.class)
            .category(OptionCategory.MANAGEMENT)
            .description("The key store which holds the certificate information instead of specifying separate files for the management server. If not given, the value is inherited from HTTP options. " + RELEVANT_MSG)
            .build();

    public static final Option<String> HTTPS_MANAGEMENT_KEY_STORE_PASSWORD = new OptionBuilder<>("https-management-key-store-password", String.class)
            .category(OptionCategory.MANAGEMENT)
            .description("The password of the key store file for the management server. If not given, the value is inherited from HTTP options. " + RELEVANT_MSG)
            .defaultValue("password")
            .build();

    public static final Option<String> HTTPS_MANAGEMENT_KEY_STORE_TYPE = new OptionBuilder<>("https-management-key-store-type", String.class)
            .hidden()
            .category(OptionCategory.MANAGEMENT)
            .description("The type of the key store file for the management server. If not given, the value is inherited from HTTP options. " + RELEVANT_MSG)
            .build();
}
