/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.quarkus.runtime;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.keycloak.Config;
import org.keycloak.authentication.authenticators.browser.WebAuthnAuthenticatorMetadata;
import org.keycloak.authentication.authenticators.browser.WebAuthnMetadataService;
import org.keycloak.common.Profile;
import org.keycloak.common.Profile.Enablement;
import org.keycloak.common.Profile.Feature;
import org.keycloak.common.crypto.CryptoIntegration;
import org.keycloak.common.crypto.CryptoProvider;
import org.keycloak.common.crypto.FipsMode;
import org.keycloak.common.util.KeystoreUtil.TruststoreFormat;
import org.keycloak.config.DatabaseOptions;
import org.keycloak.config.HealthOptions;
import org.keycloak.config.HostnameV2Options;
import org.keycloak.config.HttpAccessLogOptions;
import org.keycloak.config.HttpOptions;
import org.keycloak.config.MetricsOptions;
import org.keycloak.config.OpenApiOptions;
import org.keycloak.config.Option;
import org.keycloak.config.ProxyOptions;
import org.keycloak.config.TruststoreOptions;
import org.keycloak.headers.SecurityHeadersUtils;
import org.keycloak.marshalling.Marshalling;
import org.keycloak.models.BrowserSecurityHeaders;
import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.provider.Spi;
import org.keycloak.quarkus.runtime.configuration.Configuration;
import org.keycloak.quarkus.runtime.configuration.MicroProfileConfigProvider;
import org.keycloak.quarkus.runtime.configuration.mappers.HttpPropertyMappers;
import org.keycloak.quarkus.runtime.integration.QuarkusKeycloakSessionFactory;
import org.keycloak.quarkus.runtime.services.MisdirectedFilter;
import org.keycloak.quarkus.runtime.services.RejectNonNormalizedPathFilter;
import org.keycloak.quarkus.runtime.storage.database.liquibase.FastServiceLocator;
import org.keycloak.representations.userprofile.config.UPConfig;
import org.keycloak.theme.ClasspathThemeProviderFactory;
import org.keycloak.truststore.TruststoreBuilder;
import org.keycloak.userprofile.DeclarativeUserProfileProviderFactory;

import io.quarkus.hibernate.orm.runtime.integration.HibernateOrmIntegrationRuntimeInitListener;
import io.quarkus.hibernate.orm.runtime.integration.HibernateOrmIntegrationStaticInitListener;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.vertx.http.runtime.security.SecurityHandlerPriorities;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import liquibase.Scope;
import liquibase.servicelocator.ServiceLocator;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.spi.BootstrapContext;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.jpa.HibernateHints;
import org.infinispan.protostream.SerializationContextInitializer;

@Recorder
public class KeycloakRecorder {

    public void initConfig() {
        Config.init(new MicroProfileConfigProvider());
    }

    public void createHttpAccessLogDirectory() {
        if (Configuration.isTrue(HttpAccessLogOptions.HTTP_ACCESS_LOG_FILE_ENABLED)) {
            Environment.getHomeDir().ifPresent(homeDir -> {
                File logDir = new File(homeDir, "data" + File.separator + "log");
                if (!logDir.exists() && !logDir.mkdirs() && !logDir.exists()) {
                    throw new RuntimeException("Failed to create HTTP Access log directory");
                }
            });
        }
    }

    public void configureProfile(Profile.ProfileName profileName, Map<Profile.Feature, Boolean> features, Map<Feature, Enablement> enablements) {
        Profile.init(profileName, features, enablements);
    }

    // default handler for redirecting to specific path
    public Handler<RoutingContext> getRedirectHandler(String redirectPath) {
        return routingContext -> {
            HttpServerResponse response = routingContext.response();
            for (BrowserSecurityHeaders header : BrowserSecurityHeaders.REDIRECT_HEADERS) {
                addDefaultSecurityHeader(response, header);
            }

            response.setStatusCode(302);
            response.putHeader(HttpHeaders.LOCATION, redirectPath);
            response.end();
        };
    }

    private static void addDefaultSecurityHeader(HttpServerResponse response, BrowserSecurityHeaders header) {
        SecurityHeadersUtils.addDefaultHeaderIfAbsent(header, response.headers()::contains, (headerName, value) -> response.putHeader(headerName, value));
    }

    private static final List<ManagementInterfaceItem> MANAGEMENT_INTERFACE_ENDPOINTS = List.of(
            new ManagementInterfaceItem("/health", "Health endpoint", () -> Configuration.isTrue(HealthOptions.HEALTH_ENABLED)),
            new ManagementInterfaceItem("/metrics", "Metrics endpoint", () -> Configuration.isTrue(MetricsOptions.METRICS_ENABLED)),
            new ManagementInterfaceItem("/openapi", "OpenAPI specification", () -> Configuration.isTrue(OpenApiOptions.OPENAPI_ENABLED)),
            new ManagementInterfaceItem("/openapi/ui", "OpenAPI UI specification (Swagger)", () -> Configuration.isTrue(OpenApiOptions.OPENAPI_UI_ENABLED))
    );

    // default handler for the management interface
    public Handler<RoutingContext> getManagementHandler() {
        String itemsHtml = "<ul>%s</ul>".formatted(MANAGEMENT_INTERFACE_ENDPOINTS.stream()
                .filter(f -> f.isEnabled.getAsBoolean())
                .map(ManagementInterfaceItem::getListItem)
                .collect(Collectors.joining("\n")));

        return routingContext -> routingContext.response().end("""
                <html>
                <h2>Keycloak Management Interface</h2>
                %s
                </html>
                """.formatted(itemsHtml));
    }

    private record ManagementInterfaceItem(String path, String description, BooleanSupplier isEnabled) {
        String getListItem() {
            return "<li><a href=\"%s\">%s</a> - %s</li>".formatted(path, path, description);
        }
    }

    public void rejectNonNormalizedPathFilter(RuntimeValue<Router> runtimeValue) {
        if (Configuration.isTrue(HttpOptions.HTTP_ACCEPT_NON_NORMALIZED_PATHS)) {
            return;
        }
        runtimeValue.getValue().route().order(-1 * (SecurityHandlerPriorities.CORS + 1)).handler(new RejectNonNormalizedPathFilter());
    }
    
    public void misdirectedRequestFilter(RuntimeValue<Router> runtimeValue) {
        // not checking for http/2 enablement - it is enabled by default and not exposed as a supported configuration option
        if (!HttpPropertyMappers.isHttpsEnabled() || Configuration.getConfigValue(ProxyOptions.PROXY_HEADERS).getValue() != null) {
            return;
        }
        
        Set<String> allowedHosts = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        extractHost(HostnameV2Options.HOSTNAME, allowedHosts);
        extractHost(HostnameV2Options.HOSTNAME_ADMIN, allowedHosts);
        runtimeValue.getValue().route().order(-1 * (SecurityHandlerPriorities.CORS + 2)).handler(new MisdirectedFilter(allowedHosts));
    }
    
    private static void extractHost(Option<String> option, Set<String> allowedHosts) {
        String hostnameOrUrl = Configuration.getConfigValue(option).getValue();
        if (hostnameOrUrl != null) {
            allowedHosts.add((hostnameOrUrl.startsWith("http://") || hostnameOrUrl.startsWith("https://"))
                    ? URI.create(hostnameOrUrl).getHost()
                    : hostnameOrUrl);
        }
    }

    public void configureTruststore(FipsMode fipsMode) {
        List<String> truststores = new ArrayList<>();
        Configuration.getOptionalKcValue(TruststoreOptions.TRUSTSTORE_PATHS.getKey())
                .ifPresent(s -> Stream.of(s.split(",")).forEach(truststores::add));

        boolean includeKubernetesCa = Configuration.getOptionalKcValue(TruststoreOptions.TRUSTSTORE_KUBERNETES_CA_ENABLED.getKey())
                .map(Boolean::parseBoolean).orElse(true);
        if (includeKubernetesCa) {
            TruststoreBuilder.includeKubernetesTrustStorePaths(truststores);
        }

        Optional<String> dataDir = Environment.getDataDir();

        File truststoresDir = Environment.getHomePath().map(p -> p.resolve("conf").resolve("truststores").toFile()).orElse(null);

        if (truststoresDir != null && truststoresDir.exists() && Optional.ofNullable(truststoresDir.list()).map(a -> a.length).orElse(0) > 0) {
            truststores.add(truststoresDir.getAbsolutePath());
        } else if (truststores.size() == 0) {
            return; // nothing to configure, we'll just use the system default
        }

        TruststoreFormat truststoreType = fipsMode == FipsMode.STRICT ? TruststoreFormat.BCFKS : null;

        TruststoreBuilder.setSystemTruststore(truststores.toArray(String[]::new), true, dataDir.orElseThrow(), truststoreType);
    }

    public void configureLiquibase(Map<String, List<String>> services) {
        ServiceLocator locator = Scope.getCurrentScope().getServiceLocator();
        if (locator instanceof FastServiceLocator) {
            ((FastServiceLocator) locator).initServices(services);
        }
    }

    public RuntimeValue<QuarkusKeycloakSessionFactory> createSessionFactory(
            Map<Spi, Map<Class<? extends Provider>, Map<String, Class<? extends ProviderFactory>>>> factories,
            Map<Class<? extends Provider>, String> defaultProviders,
            Map<String, ProviderFactory> preConfiguredProviders,
            List<ClasspathThemeProviderFactory.ThemesRepresentation> themes) {
        return new RuntimeValue<QuarkusKeycloakSessionFactory>(new QuarkusKeycloakSessionFactory(factories, defaultProviders, preConfiguredProviders, themes));
    }

    public void setDefaultUserProfileConfiguration(UPConfig configuration) {
        DeclarativeUserProfileProviderFactory.setDefaultConfig(configuration);
    }

    public void setDefaultWebAuthnMetadata(Map<String, WebAuthnAuthenticatorMetadata> metadata) {
        WebAuthnMetadataService.setDefaultMetadata(metadata);
    }


    /**
     * Static-init listener that contributes the given properties to a persistence unit's boot configuration.
     * {@code contributeBootProperties} runs after Quarkus' build-time overwrites (e.g. dialect), so these values win.
     * Used for the default PU (dialect from --db-dialect, query startup checking, named queries, ...) and for
     * per-named-PU Keycloak options that must override values coming from a user's persistence.xml.
     */
    public HibernateOrmIntegrationStaticInitListener createStaticPropertiesListener(Map<String, ?> properties) {
        return new HibernateOrmIntegrationStaticInitListener() {
            @Override
            public void contributeBootProperties(BiConsumer<String, Object> propertyCollector) {
                for (Map.Entry<String, ?> entry : properties.entrySet()) {
                    if (entry.getValue() != null) {
                        propertyCollector.accept(entry.getKey(), entry.getValue());
                    }
                }
            }

            @Override
            public void onMetadataInitialized(Metadata metadata, BootstrapContext bootstrapContext,
                    BiConsumer<String, Object> propertyCollector) {
                // no-op
            }
        };
    }

    /**
     * Runtime-init listener re-applying persistence.xml properties that Quarkus overwrites at runtime for units not
     * built from a persistence.xml (schema-generation action). Runs after {@code injectRuntimeConfiguration}, so it wins.
     */
    public HibernateOrmIntegrationRuntimeInitListener createUserDefinedUnitRuntimeListener(Map<String, String> originalProps) {
        return propertyCollector -> {
            String schemaAction = resolveSchemaGenerationAction(originalProps);
            if (schemaAction != null) {
                propertyCollector.accept(AvailableSettings.JAKARTA_HBM2DDL_DATABASE_ACTION, schemaAction);
            }
            reapply(originalProps, propertyCollector, AvailableSettings.JAKARTA_HBM2DDL_CREATE_SCHEMAS, "javax.persistence.create-database-schemas");
            reapply(originalProps, propertyCollector, HibernateHints.HINT_FLUSH_MODE);
            reapply(originalProps, propertyCollector, AvailableSettings.JAKARTA_HBM2DDL_SCRIPTS_ACTION, "javax.persistence.schema-generation.scripts.action");
        };
    }

    private static void reapply(Map<String, String> originalProps, BiConsumer<String, Object> propertyCollector, String key, String... legacyKeys) {
        String value = originalProps.get(key);
        if (value == null) {
            for (String legacyKey : legacyKeys) {
                if (value == null) {
                    value = originalProps.get(legacyKey);
                }
            }
        }
        if (value != null) {
            propertyCollector.accept(key, value);
        }
    }

    /**
     * Resolve the schema-generation action a user persistence.xml requested, following Hibernate's precedence (the JPA
     * {@code jakarta}/{@code javax} action first, then {@code hibernate.hbm2ddl.auto} as a fallback), so it can be
     * restored after Quarkus resets it to "none" at runtime for property-configured persistence units. The legacy
     * {@code hibernate.hbm2ddl.auto=create} (drop-and-create) is mapped to the JPA key's {@code drop-and-create}, because
     * under the JPA-standard key a bare {@code create} means create-only.
     */
    private static String resolveSchemaGenerationAction(Map<String, String> originalProps) {
        String jakartaAction = originalProps.get(AvailableSettings.JAKARTA_HBM2DDL_DATABASE_ACTION);
        if (jakartaAction != null) {
            return jakartaAction;
        }
        // Hibernate 7 has no AvailableSettings constant for the deprecated javax key.
        String javaxAction = originalProps.get("javax.persistence.schema-generation.database.action");
        if (javaxAction != null) {
            return javaxAction;
        }
        String hbm2ddlAuto = originalProps.get(AvailableSettings.HBM2DDL_AUTO);
        if (hbm2ddlAuto != null) {
            return "create".equalsIgnoreCase(hbm2ddlAuto.trim()) ? "drop-and-create" : hbm2ddlAuto;
        }
        return null;
    }

    public HibernateOrmIntegrationRuntimeInitListener createDefaultUnitListener() {
        return propertyCollector -> propertyCollector.accept(AvailableSettings.DEFAULT_SCHEMA, Configuration.getConfigValue(DatabaseOptions.DB_SCHEMA).getValue());
    }

    public void setCryptoProvider(FipsMode fipsMode) {
        String cryptoProvider = fipsMode.getProviderClassName();

        try {
            CryptoIntegration.setProvider(
                    (CryptoProvider) Thread.currentThread().getContextClassLoader().loadClass(cryptoProvider).getDeclaredConstructor().newInstance());
        } catch (ClassNotFoundException | NoClassDefFoundError cause) {
            if (fipsMode.isFipsEnabled()) {
                throw new RuntimeException("Failed to configure FIPS. Make sure you have added the Bouncy Castle FIPS dependencies to the 'providers' directory.");
            }
            throw new RuntimeException("Unexpected error when configuring the crypto provider: " + cryptoProvider, cause);
        } catch (Exception cause) {
            throw new RuntimeException("Unexpected error when configuring the crypto provider: " + cryptoProvider, cause);
        }
    }

    public void configureProtoStreamSchemas(List<SerializationContextInitializer> schemas) {
        Marshalling.setSchemas(schemas);
    }
}
