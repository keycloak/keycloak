package org.keycloak.config;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class HttpOptions {

    public final static Option httpPort = new OptionBuilder<Integer>("http-port", Integer.class)
            .description("The used HTTP port.")
            .category(OptionCategory.HTTP)
            .defaultValue(8080)
            .build();

    public final static List<Option<?>> ALL_OPTIONS = new ArrayList<>();

    static {
        ALL_OPTIONS.add(httpPort);
    }
}
