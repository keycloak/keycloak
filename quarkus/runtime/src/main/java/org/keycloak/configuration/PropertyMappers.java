/*
 * Copyright 202 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.configuration;

import static org.keycloak.configuration.PropertyMapper.create;
import static org.keycloak.configuration.PropertyMapper.createWithDefault;
import static org.keycloak.configuration.PropertyMapper.forBuildTimeProperty;

import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import io.quarkus.runtime.configuration.ProfileManager;
import io.smallrye.config.ConfigSourceInterceptorContext;
import io.smallrye.config.ConfigValue;

/**
 * Configures the {@link PropertyMapper} instances for all Keycloak configuration properties that should be mapped to their
 * corresponding properties in Quarkus.
 */
public final class PropertyMappers {

    static {
        configureDatabasePropertyMappers();
        configureHttpPropertyMappers();
        configureProxyMappers();
    }

    private static void configureHttpPropertyMappers() {
        createWithDefault("http.enabled", "quarkus.http.insecure-requests", "disabled", (value, context) -> {
            Boolean enabled = Boolean.valueOf(value);
            ConfigValue proxy = context.proceed(MicroProfileConfigProvider.NS_KEYCLOAK_PREFIX + "proxy");

            if ("dev".equalsIgnoreCase(ProfileManager.getActiveProfile()) || 
                    (proxy != null && "edge".equalsIgnoreCase(proxy.getValue()))) {
                enabled = true;
            }
            
            return enabled ? "enabled" : "disabled";
        }, "Enables the HTTP listener.");
        createWithDefault("http.port", "quarkus.http.port", String.valueOf(8080), "The HTTP port.");
        createWithDefault("https.port", "quarkus.http.ssl-port", String.valueOf(8443), "The HTTPS port.");
        createWithDefault("https.client-auth", "quarkus.http.ssl.client-auth", "none", "Configures the server to require/request client authentication. none, request, required.");
        create("https.cipher-suites", "quarkus.http.ssl.cipher-suites", "The cipher suites to use. If none is given, a reasonable default is selected.");
        create("https.protocols", "quarkus.http.ssl.protocols", "The list of protocols to explicitly enable.");
        create("https.certificate.file", "quarkus.http.ssl.certificate.file", "The file path to a server certificate or certificate chain in PEM format.");
        create("https.certificate.key-store-file", "quarkus.http.ssl.certificate.key-store-file", "An optional key store which holds the certificate information instead of specifying separate files.");
        create("https.certificate.key-store-password", "quarkus.http.ssl.certificate.key-store-password", "A parameter to specify the password of the key store file. If not given, the default (\"password\") is used.");
        create("https.certificate.key-store-file-type", "quarkus.http.ssl.certificate.key-store-file-type", "An optional parameter to specify type of the key store file. If not given, the type is automatically detected based on the file name.");
        create("https.certificate.trust-store-file", "quarkus.http.ssl.certificate.trust-store-file", "An optional trust store which holds the certificate information of the certificates to trust.");
        create("https.certificate.trust-store-password", "quarkus.http.ssl.certificate.trust-store-password", "A parameter to specify the password of the trust store file.");
        create("https.certificate.trust-store-file-type", "quarkus.http.ssl.certificate.trust-store-file-type", "An optional parameter to specify type of the trust store file. If not given, the type is automatically detected based on the file name.");
    }

    private static void configureProxyMappers() {
        createWithDefault("proxy", "quarkus.http.proxy.proxy-address-forwarding", "none", (mode, context) -> {
            switch (mode) {
                case "none":
                    return "false";
                case "edge":
                case "reencrypt":
                case "passthrough":
                    return "true";
            }
            throw new RuntimeException("Invalid value [" + mode + "] for configuration property [proxy]");
        }, "The proxy mode if the server is behind a reverse proxy. Possible values are: none, edge, reencrypt, and passthrough.");
    }

    private static void configureDatabasePropertyMappers() {
        forBuildTimeProperty("db", "quarkus.hibernate-orm.dialect", (db, context) -> {
            switch (db.toLowerCase()) {
                case "h2-file":
                case "h2-mem":
                    return "io.quarkus.hibernate.orm.runtime.dialect.QuarkusH2Dialect";
                case "mariadb":
                    return "org.hibernate.dialect.MariaDBDialect";
                case "postgres-95":
                    return "io.quarkus.hibernate.orm.runtime.dialect.QuarkusPostgreSQL95Dialect";
                case "postgres-10":
                    return "io.quarkus.hibernate.orm.runtime.dialect.QuarkusPostgreSQL10Dialect";
            }
            return null;
        }, "The database vendor. Possible values are: h2-mem, h2-file, mariadb, postgres95, postgres10.");
        create("db", "quarkus.datasource.driver", (db, context) -> {
            switch (db.toLowerCase()) {
                case "h2-file":
                case "h2-mem":
                    return "org.h2.jdbcx.JdbcDataSource";
                case "mariadb":
                    return "org.mariadb.jdbc.MySQLDataSource";
                case "postgres-95":
                case "postgres-10":
                    return "org.postgresql.xa.PGXADataSource";
            }
            return null;
        }, null);
        create("db", "quarkus.datasource.jdbc.transactions", (db, context) -> "xa", null);
        create("db.url", "db", "quarkus.datasource.url", (db, context) -> {
            switch (db.toLowerCase()) {
                case "h2-file":
                    return "jdbc:h2:file:${kc.home.dir:${kc.db.url.path:~}}/data/keycloakdb${kc.db.url.properties:;;AUTO_SERVER=TRUE}";
                case "h2-mem":
                    return "jdbc:h2:mem:keycloakdb${kc.db.url.properties:}";
                case "mariadb":
                    return "jdbc:mariadb://${kc.db.url.host:localhost}/${kc.db.url.database:keycloak}${kc.db.url.properties:}";
                case "postgres-95":
                case "postgres-10":
                    return "jdbc:postgresql://${kc.db.url.host:localhost}/${kc.db.url.database}${kc.db.url.properties:}";
            }
            return null;
        }, "The database JDBC URL. If not provided a default URL is set based on the selected database vendor.");
        create("db.username", "quarkus.datasource.username", "The database username.");
        create("db.password", "quarkus.datasource.password", "The database password");
        create("db.schema", "quarkus.datasource.schema", "The database schema.");
        create("db.pool.initial-size", "quarkus.datasource.jdbc.initial-size", "The initial size of the connection pool.");
        create("db.pool.min-size", "quarkus.datasource.jdbc.min-size", "The minimal size of the connection pool.");
        createWithDefault("db.pool.max-size", "quarkus.datasource.jdbc.max-size", String.valueOf(100), "The maximum size of the connection pool.");
    }

    static ConfigValue getValue(ConfigSourceInterceptorContext context, String name) {
        return PropertyMapper.MAPPERS.getOrDefault(name, PropertyMapper.IDENTITY)
                .getOrDefault(name, context, context.proceed(name));
    }
    
    public static boolean isBuildTimeProperty(String name) {
        return PropertyMapper.MAPPERS.entrySet().stream()
                .anyMatch(entry -> entry.getValue().getFrom().equals(name) && entry.getValue().isBuildTime());
    }

    public static boolean isSupported(String name) {
        return PropertyMapper.MAPPERS.entrySet().stream()
                .anyMatch(entry -> toCLIFormat(entry.getValue().getFrom()).equals(name));
    }

    public static String toCLIFormat(String name) {
        if (name.indexOf('.') == -1) {
            return name;
        }
        return MicroProfileConfigProvider.NS_KEYCLOAK_PREFIX
                .concat(name.substring(3, name.lastIndexOf('.') + 1)
                        .replaceAll("\\.", "-") + name.substring(name.lastIndexOf('.') + 1));
    }

    public static List<PropertyMapper> getRuntimeMappers() {
        return PropertyMapper.MAPPERS.values().stream()
                .filter(entry -> !entry.isBuildTime()).collect(Collectors.toList());
    }

    public static List<PropertyMapper> getBuiltTimeMappers() {
        return PropertyMapper.MAPPERS.values().stream()
                .filter(entry -> entry.isBuildTime()).collect(Collectors.toList());
    }
}
