package org.keycloak.config;

public class ClassLoaderOptions {

    public static final Option<String> IGNORE_ARTIFACTS = new OptionBuilder<>("class-loader-ignore-artifacts", String.class)
            .category(OptionCategory.GENERAL)
            .hidden()
            .build();
}
