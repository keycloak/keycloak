package org.keycloak.testframework.server;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.keycloak.common.Profile;
import org.keycloak.testframework.infinispan.CacheType;

import io.smallrye.config.SmallRyeConfig;
import org.eclipse.microprofile.config.spi.ConfigSource;

public class KeycloakServerConfigBuilder {

    private static final String SPI_OPTION = "spi-%s--%s--%s";

    private final String command;
    private final Map<String, String> options = new HashMap<>();
    private final Set<String> features = new HashSet<>();
    private final Set<String> featuresDisabled = new HashSet<>();
    private final LogBuilder log = new LogBuilder();
    private final Set<KeycloakDependency> dependencies = new HashSet<>();
    private CacheType cacheType = CacheType.LOCAL;
    private boolean externalInfinispan = false;

    private KeycloakServerConfigBuilder(String command) {
        this.command = command;
    }

    public static KeycloakServerConfigBuilder startDev() {
        return new KeycloakServerConfigBuilder("start-dev");
    }

    /**
     * Set the client id and secret to use for bootstrapping configuration
     *
     * @param clientId the client id
     * @param clientSecret the client secret
     * @return
     */
    public KeycloakServerConfigBuilder bootstrapAdminClient(String clientId, String clientSecret) {
        return option("bootstrap-admin-client-id", clientId)
                .option("bootstrap-admin-client-secret", clientSecret);
    }

    /**
     * Set the username and password to use for bootstrapping configuration
     *
     * @param username the username
     * @param password the secret
     * @return
     */
    public KeycloakServerConfigBuilder bootstrapAdminUser(String username, String password) {
        return option("bootstrap-admin-username", username)
                .option("bootstrap-admin-password", password);
    }

    /**
     * Configure if local caches or clustered caches should be used. Using local caches results in a faster startup
     * time
     *
     * @param cacheType
     * @return
     */
    public KeycloakServerConfigBuilder cache(CacheType cacheType) {
        this.cacheType = cacheType;
        return this;
    }

    /**
     * Connect to a managed external Infinispan server
     *
     * @param enabled
     * @return
     */
    public KeycloakServerConfigBuilder externalInfinispanEnabled(boolean enabled) {
        if (enabled) {
            this.externalInfinispan = true;
            cache(CacheType.ISPN);
        } else {
            this.externalInfinispan = false;
            cache(CacheType.LOCAL);
        }
        return this;
    }

    public boolean isExternalInfinispanEnabled() {
        return this.externalInfinispan;
    }

    /**
     * Configure logging
     *
     * @return
     */
    public LogBuilder log() {
        return log;
    }

    /**
     * Enable the specified features. In most cases used to enable features that are not enabled by default
     *
     * @param features the features to enable
     * @return
     */
    public KeycloakServerConfigBuilder features(Profile.Feature... features) {
        this.features.addAll(toFeatureStrings(features));
        return this;
    }

    /**
     * Disable the specified features. In most cases used to disable features that are enabled by default
     *
     * @param features the features to disable
     * @return
     */
    public KeycloakServerConfigBuilder featuresDisabled(Profile.Feature... features) {
        this.featuresDisabled.addAll(toFeatureStrings(features));
        return this;
    }

    /**
     * Set multiple CLI options
     *
     * @param options
     * @return
     */
    public KeycloakServerConfigBuilder options(Map<String, String> options) {
        this.options.putAll(options);
        return this;
    }

    /**
     * Set the specified CLI option
     *
     * @param key the key of the option
     * @param value the value of the option
     * @return
     */
    public KeycloakServerConfigBuilder option(String key, String value) {
        options.put(key, value);
        return this;
    }

    /**
     * Set an SPI configuration option
     *
     * @param spi the name of the SPI
     * @param provider the name of the provider
     * @param key the name of the option
     * @param value the value to set
     * @return
     */
    public KeycloakServerConfigBuilder spiOption(String spi, String provider, String key, String value) {
        options.put(String.format(SPI_OPTION, spi, provider, key), value);
        return this;
    }

    /**
     * Deploy a dependency to the server by specifying the Maven groupId and artifactId. The version is resolved from
     * the project pom files
     *
     * @param groupId the Maven groupId of the dependency
     * @param artifactId the Maven artifactId of the dependency
     * @return
     */
    public KeycloakServerConfigBuilder dependency(String groupId, String artifactId) {
        return dependency(groupId, artifactId, false, false);
    }

    public KeycloakServerConfigBuilder dependency(String groupId, String artifactId, boolean hotDeployable) {
        return dependency(groupId, artifactId, hotDeployable, false);
    }

    private KeycloakServerConfigBuilder dependency(String groupId, String artifactId, boolean hotDeployable, boolean dependencyCurrentProject) {
        dependencies.add(
                new KeycloakDependency.Builder()
                        .setGroupId(groupId)
                        .setArtifactId(artifactId)
                        .hotDeployable(hotDeployable)
                        .dependencyCurrentProject(dependencyCurrentProject)
                        .build()
        );
        return this;
    }

    public KeycloakServerConfigBuilder dependencyCurrentProject() {
        return dependency("", "", false, true);
    }

    public class LogBuilder {

        private Boolean color;
        private String format;
        private String rootLevel;
        private final Map<String, String> categoryLevels = new HashMap<>();
        private final Map<String, String> handlerLevels = new HashMap<>();
        private final Set<String> handlers = new HashSet<>();
        private String syslogEndpoint;

        public LogBuilder handlers(LogHandlers... handlers) {
            this.handlers.addAll(Arrays.stream(handlers).map(l -> l.name().toLowerCase()).collect(Collectors.toSet()));
            return this;
        }

        public LogBuilder handlerLevel(LogHandlers handler, String logLevel) {
            handlerLevels.put(handler.name().toLowerCase(), logLevel);
            return this;
        }

        public LogBuilder categoryLevel(String category, String logLevel) {
            categoryLevels.put(category, logLevel);
            return this;
        }

        public LogBuilder syslogEndpoint(String syslogEndpoint) {
            this.syslogEndpoint = syslogEndpoint;
            return this;
        }

        public LogBuilder fromConfig(SmallRyeConfig config) {
            List<ConfigSource> sources = new LinkedList<>();
            for (ConfigSource source : config.getConfigSources()) {
                if (source.getName().startsWith("EnvConfigSource") || source.getName().equals("KeycloakTestConfig")) {
                    sources.add(source);
                }
            }

            for (ConfigSource source : sources) {
                for (String p : source.getPropertyNames()) {
                    if (p.equals("kc.test.log.console.format") && format == null) {
                        format = source.getValue(p);
                    }
                    if (p.equals("kc.test.console.color") && color == null) {
                        color = Boolean.parseBoolean(source.getValue(p));
                    } else if (p.equals("kc.test.log.level") && rootLevel == null) {
                        rootLevel = source.getValue(p);
                    } else if (p.startsWith("kc.test.log.category.")) {
                        String category = p.split("\"")[1];
                        String level = source.getValue(p);

                        if (!categoryLevels.containsKey(category)) {
                            categoryLevels.put(category, level);
                        }
                    }
                }
            }
            return this;
        }

        private void build() {
            if (!handlers.isEmpty()) {
                option("log", String.join(",", handlers));
            }

            if (!handlerLevels.isEmpty()) {
                handlerLevels.forEach((key, value) -> option("log-" + key + "-level", value));
            }

            if (syslogEndpoint != null) {
                option("log-syslog-endpoint", syslogEndpoint);
            }

            if (format != null) {
                option("log-console-format", format);
            }

            if (rootLevel != null) {
                option("log-level", rootLevel);
            }

            for (Map.Entry<String, String> e : categoryLevels.entrySet()) {
                option("log-level-" + e.getKey(), e.getValue());
            }

            if (color != null) {
                option("log-console-color", color.toString());
            }
        }
    }

    List<String> toArgs() {
        // Cache setup -> supported values: local or ispn
        option("cache", cacheType.name().toLowerCase());

        log.build();

        List<String> args = new LinkedList<>();
        args.add(command);
        for (Map.Entry<String, String> e : options.entrySet()) {
            args.add("--" + e.getKey() + "=" + e.getValue());
        }
        if (!features.isEmpty()) {
            args.add("--features=" + String.join(",", features));
        }
        if (!featuresDisabled.isEmpty()) {
            args.add("--features-disabled=" + String.join(",", featuresDisabled));
        }

        return args;
    }

    Set<KeycloakDependency> toDependencies() {
        return dependencies;
    }

    private Set<String> toFeatureStrings(Profile.Feature... features) {
        return Arrays.stream(features).map(f -> {
            if (f.getVersion() > 1 || Profile.getFeatureVersions(f.getKey()).size() > 1) {
                return f.getVersionedKey();
            }
            return f.getUnversionedKey();
        }).collect(Collectors.toSet());
    }

    public enum LogHandlers {
        CONSOLE,
        FILE,
        SYSLOG
    }

}
