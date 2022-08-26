package org.keycloak.quarkus.runtime.configuration.mappers;

import io.smallrye.config.ConfigSourceInterceptorContext;
import io.smallrye.config.ConfigValue;
import org.keycloak.config.HttpOptions;
import org.keycloak.quarkus.runtime.Environment;
import org.keycloak.quarkus.runtime.Messages;
import org.keycloak.quarkus.runtime.configuration.MicroProfileConfigProvider;

import java.io.File;
import java.nio.file.Paths;
import java.util.Optional;

import static java.util.Optional.of;
import static org.keycloak.quarkus.runtime.configuration.mappers.PropertyMapper.fromOption;
import static org.keycloak.quarkus.runtime.configuration.mappers.PropertyMappers.getMapper;
import static org.keycloak.quarkus.runtime.integration.QuarkusPlatform.addInitializationException;

final class HttpPropertyMappers {

    private HttpPropertyMappers(){}

    public static PropertyMapper[] getHttpPropertyMappers() {
        return new PropertyMapper[] {
                fromOption(HttpOptions.HTTP_ENABLED)
                        .to("quarkus.http.insecure-requests")
                        .transformer(HttpPropertyMappers::getHttpEnabledTransformer)
                        .paramLabel(Boolean.TRUE + "|" + Boolean.FALSE)
                        .build(),
                fromOption(HttpOptions.HTTP_HOST)
                        .to("quarkus.http.host")
                        .paramLabel("host")
                        .build(),
                fromOption(HttpOptions.HTTP_RELATIVE_PATH)
                        .to("quarkus.http.root-path")
                        .paramLabel("path")
                        .build(),
                fromOption(HttpOptions.HTTP_PORT)
                        .to("quarkus.http.port")
                        .paramLabel("port")
                        .build(),
                fromOption(HttpOptions.HTTPS_PORT)
                        .to("quarkus.http.ssl-port")
                        .paramLabel("port")
                        .build(),
                fromOption(HttpOptions.HTTPS_CLIENT_AUTH)
                        .to("quarkus.http.ssl.client-auth")
                        .paramLabel("auth")
                        .build(),
                fromOption(HttpOptions.HTTPS_CIPHER_SUITES)
                        .to("quarkus.http.ssl.cipher-suites")
                        .paramLabel("ciphers")
                        .build(),
                fromOption(HttpOptions.HTTPS_PROTOCOLS)
                        .to("quarkus.http.ssl.protocols")
                        .paramLabel("protocols")
                        .build(),
                fromOption(HttpOptions.HTTPS_CERTIFICATE_FILE)
                        .to("quarkus.http.ssl.certificate.file")
                        .paramLabel("file")
                        .build(),
                fromOption(HttpOptions.HTTPS_CERTIFICATE_KEY_FILE)
                        .to("quarkus.http.ssl.certificate.key-file")
                        .paramLabel("file")
                        .build(),
                fromOption(HttpOptions.HTTPS_KEY_STORE_FILE
                            .withRuntimeSpecificDefault(getDefaultKeystorePathValue()))
                        .to("quarkus.http.ssl.certificate.key-store-file")
                        .paramLabel("file")
                        .build(),
                fromOption(HttpOptions.HTTPS_KEY_STORE_PASSWORD)
                        .to("quarkus.http.ssl.certificate.key-store-password")
                        .paramLabel("password")
                        .isMasked(true)
                        .build(),
                fromOption(HttpOptions.HTTPS_KEY_STORE_TYPE)
                        .to("quarkus.http.ssl.certificate.key-store-file-type")
                        .paramLabel("type")
                        .build(),
                fromOption(HttpOptions.HTTPS_TRUST_STORE_FILE)
                        .to("quarkus.http.ssl.certificate.trust-store-file")
                        .paramLabel("file")
                        .build(),
                fromOption(HttpOptions.HTTPS_TRUST_STORE_PASSWORD)
                        .to("quarkus.http.ssl.certificate.trust-store-password")
                        .paramLabel("password")
                        .isMasked(true)
                        .build(),
                fromOption(HttpOptions.HTTPS_TRUST_STORE_TYPE)
                        .to("quarkus.http.ssl.certificate.trust-store-file-type")
                        .paramLabel("type")
                        .build()
        };
    }

    private static Optional<String> getHttpEnabledTransformer(Optional<String> value, ConfigSourceInterceptorContext context) {
        boolean enabled = Boolean.parseBoolean(value.get());
        ConfigValue proxy = context.proceed(MicroProfileConfigProvider.NS_KEYCLOAK_PREFIX + "proxy");

        if (Environment.isDevMode() || Environment.isImportExportMode()
                || (proxy != null && "edge".equalsIgnoreCase(proxy.getValue()))) {
            enabled = true;
        }

        if (!enabled) {
            ConfigValue proceed = context.proceed("kc.https-certificate-file");

            if (proceed == null || proceed.getValue() == null) {
                proceed = getMapper("quarkus.http.ssl.certificate.key-store-file").getConfigValue(context);
            }

            if (proceed == null || proceed.getValue() == null) {
                addInitializationException(Messages.httpsConfigurationNotSet());
            }
        }

        return of(enabled ? "enabled" : "disabled");
    }

    private static String getDefaultKeystorePathValue() {
        String homeDir = Environment.getHomeDir();

        if (homeDir != null) {
            File file = Paths.get(homeDir, "conf", "server.keystore").toFile();

            if (file.exists()) {
                return file.getAbsolutePath();
            }
        }

        return null;
    }

}

