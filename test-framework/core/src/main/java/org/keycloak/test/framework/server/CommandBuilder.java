package org.keycloak.test.framework.server;

import io.smallrye.config.SmallRyeConfig;
import org.eclipse.microprofile.config.spi.ConfigSource;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CommandBuilder {

    private final String command;
    private final Map<String, String> options = new HashMap<>();
    private final Set<String> features = new HashSet<>();
    private final LogBuilder log = new LogBuilder();

    private CommandBuilder(String command) {
        this.command = command;
    }

    public static CommandBuilder startDev() {
        return new CommandBuilder("start-dev");
    }

    public CommandBuilder bootstrapAdminClient(String clientId, String clientSecret) {
        return option("bootstrap-admin-client-id", clientId)
                .option("bootstrap-admin-client-secret", clientSecret);
    }

    public CommandBuilder cache(String cache) {
        return option("cache", cache);
    }

    public LogBuilder log() {
        return log;
    }

    public CommandBuilder features(Set<String> features) {
        this.features.addAll(features);
        return this;
    }

    public CommandBuilder databaseConfig(Map<String, String> databaseConfig) {
        for (String k : databaseConfig.keySet()) {
            if (!k.startsWith("db")) {
                throw new IllegalArgumentException("Database config supplied non-database configuration: " + k);
            }
        }
        return options(databaseConfig);
    }

    public CommandBuilder options(Map<String, String> options) {
        this.options.putAll(options);
        return this;
    }

    public CommandBuilder option(String key, String value) {
        options.put(key, value);
        return this;
    }

    public class LogBuilder {

        private Boolean color;
        private String format;
        private String rootLevel;
        private Map<String, String> categoryLevels = new HashMap<>();
        private String syslogEndpoint;

        public LogBuilder enableSyslog(String syslogEndpoint) {
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
            if (syslogEndpoint != null) {
                option("log", "console,syslog");
                option("log-syslog-level", "info");
                option("log-syslog-endpoint", syslogEndpoint);
                option("spi-events-listener-jboss-logging-success-level", "INFO");
                categoryLevels.put("org.keycloak.events", "INFO");
            }

            if (format != null) {
                option("log-console-format", format);
            }

            StringBuilder logLevel = new StringBuilder();
            if (rootLevel != null) {
                logLevel.append(rootLevel);
            }

            for (Map.Entry<String, String> e : categoryLevels.entrySet()) {
                if (!logLevel.isEmpty()) {
                    logLevel.append(",");
                }
                logLevel.append(e.getKey());
                logLevel.append(":");
                logLevel.append(e.getValue());
            }

            if (!logLevel.isEmpty()) {
                option("log-level", logLevel.toString());
            }

            if (color != null) {
                option("log-console-color", color.toString());
            }
        }
    }

    public List<String> toArgs() {
        log.build();

        List<String> args = new LinkedList<>();
        args.add(command);
        for (Map.Entry<String, String> e : options.entrySet()) {
            args.add("--" + e.getKey() + "=" + e.getValue());
        }
        if (!features.isEmpty()) {
            args.add("--features=" + String.join(",", features));
        }

        return args;
    }

}
