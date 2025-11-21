package org.keycloak.testframework.server;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.keycloak.common.Profile;
import org.keycloak.testframework.infinispan.CacheType;

import io.quarkus.maven.dependency.Dependency;
import io.quarkus.maven.dependency.DependencyBuilder;
import io.smallrye.config.SmallRyeConfig;
import org.eclipse.microprofile.config.spi.ConfigSource;

public class KeycloakServerConfigBuilder {

    private static final String SPI_OPTION = "spi-%s--%s--%s";

    private final String command;
    private final Map<String, String> options = new HashMap<>();
    private final Set<String> features = new HashSet<>();
    private final Set<String> featuresDisabled = new HashSet<>();
    private final LogBuilder log = new LogBuilder();
    private final Set<Dependency> dependencies = new HashSet<>();
    private final Set<Path> configFiles = new HashSet<>();
    private CacheType cacheType = CacheType.LOCAL;
    private boolean externalInfinispan = false;
    private boolean tlsEnabled = false;

    private KeycloakServerConfigBuilder(String command) {
        this.command = command;
    }

    public static KeycloakServerConfigBuilder startDev() {
        return new KeycloakServerConfigBuilder("start-dev");
    }

    public KeycloakServerConfigBuilder bootstrapAdminClient(String clientId, String clientSecret) {
        return option("bootstrap-admin-client-id", clientId)
                .option("bootstrap-admin-client-secret", clientSecret);
    }

    public KeycloakServerConfigBuilder bootstrapAdminUser(String username, String password) {
        return option("bootstrap-admin-username", username)
                .option("bootstrap-admin-password", password);
    }

    public KeycloakServerConfigBuilder cache(CacheType cacheType) {
        this.cacheType = cacheType;
        return this;
    }

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

    public LogBuilder log() {
        return log;
    }

    public KeycloakServerConfigBuilder features(Profile.Feature... features) {
        this.features.addAll(toFeatureStrings(features));
        return this;
    }

    public KeycloakServerConfigBuilder featuresDisabled(Profile.Feature... features) {
        this.featuresDisabled.addAll(toFeatureStrings(features));
        return this;
    }

    public KeycloakServerConfigBuilder options(Map<String, String> options) {
        this.options.putAll(options);
        return this;
    }

    public KeycloakServerConfigBuilder option(String key, String value) {
        options.put(key, value);
        return this;
    }

    public KeycloakServerConfigBuilder spiOption(String spi, String provider, String key, String value) {
        options.put(String.format(SPI_OPTION, spi, provider, key), value);
        return this;
    }

    public KeycloakServerConfigBuilder dependency(String groupId, String artifactId) {
        dependencies.add(new DependencyBuilder().setGroupId(groupId).setArtifactId(artifactId).build());
        return this;
    }

    public KeycloakServerConfigBuilder tlsEnabled(boolean enabled) {
        tlsEnabled = enabled;
        return this;
    }

    public boolean tlsEnabled() {
        return tlsEnabled ;
    }
    
    public KeycloakServerConfigBuilder cacheConfigFile(String resourcePath) {
        try {
            Path p = Paths.get(Objects.requireNonNull(getClass().getResource(resourcePath)).toURI());
            configFiles.add(p);
            option("cache-config-file", p.getFileName().toString());
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }

        return this;
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

    Set<Dependency> toDependencies() {
        return dependencies;
    }

    Set<Path> toConfigFiles() {
        return configFiles;
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
