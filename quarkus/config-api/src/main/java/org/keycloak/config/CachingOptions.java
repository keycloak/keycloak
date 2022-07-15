package org.keycloak.config;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class CachingOptions {

    public enum Mechanism {
        ispn,
        local
    }

    public static final Option CACHE = new OptionBuilder<>("cache", Mechanism.class)
            .category(OptionCategory.CACHE)
            .description("Defines the cache mechanism for high-availability. "
                    + "By default, a 'ispn' cache is used to create a cluster between multiple server nodes. "
                    + "A 'local' cache disables clustering and is intended for development and testing purposes.")
            .defaultValue(Mechanism.ispn)
            .buildTime(true)
            .build();

    public enum Stack {
        tcp,
        udp,
        kubernetes,
        ec2,
        azure,
        google;
    }

    public static final Option CACHE_STACK = new OptionBuilder<>("cache-stack", Stack.class)
            .category(OptionCategory.CACHE)
            .description("Define the default stack to use for cluster communication and node discovery. This option only takes effect "
                    + "if 'cache' is set to 'ispn'. Default: udp.")
            .buildTime(true)
            .expectedValues(Stack.values())
            .build();

    public static final Option<File> CACHE_CONFIG_FILE = new OptionBuilder<>("cache-config-file", File.class)
            .category(OptionCategory.CACHE)
            .description("Defines the file from which cache configuration should be loaded from. "
                    + "The configuration file is relative to the 'conf/' directory.")
            .buildTime(true)
            .build();

    public static final List<Option<?>> ALL_OPTIONS = new ArrayList<>();

    static {
        ALL_OPTIONS.add(CACHE);
        ALL_OPTIONS.add(CACHE_STACK);
        ALL_OPTIONS.add(CACHE_CONFIG_FILE);
    }
}
