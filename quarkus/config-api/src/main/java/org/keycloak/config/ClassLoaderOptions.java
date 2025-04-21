package org.keycloak.config;

public class ClassLoaderOptions {

    public static final String QUARKUS_REMOVED_ARTIFACTS_PROPERTY = "quarkus.class-loading.removed-artifacts";

    public static final Option<String> IGNORE_ARTIFACTS = new OptionBuilder<>("class-loader-ignore-artifacts", String.class)
            .category(OptionCategory.GENERAL)
            .hidden()
            .buildTime(true)
            .build();
}
