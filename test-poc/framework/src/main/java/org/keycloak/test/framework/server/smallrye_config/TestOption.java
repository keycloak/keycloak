package org.keycloak.test.framework.server.smallrye_config;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class TestOption {

    private static final String PREFIX = "keycloak";

    private static final String SERVER_KEY = "server";

    private static final String EMBEDDED = "%embedded";

    private static final String REMOTE = "%remote";

    public static final String DEFAULT_SERVER_VAL = "embedded";

    public static final Set<String> DEFAULT_FEATURES_VAL = Collections.unmodifiableSet(new HashSet<>());


    public static String prefix() {
        return PREFIX;
    }

    public static String embeddedPrefix() {
        return EMBEDDED + "." + PREFIX;
    }

    public static String remotePrefix() {
        return REMOTE + "." + PREFIX;
    }

    public static String server() {
        return PREFIX + "." + SERVER_KEY;
    }

    public static String embedded() {
        return embeddedPrefix() + "." + SERVER_KEY;
    }

    public static String remote() {
        return remotePrefix() + "." + SERVER_KEY;
    }
}
