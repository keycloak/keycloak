package org.keycloak.config;

public class IdentityOptions {
  public static final Option<Boolean> USE_IDENTITY = new OptionBuilder<>("use-identity", Boolean.class)
    .category(OptionCategory.DATABASE_IDENTITY)
    .defaultValue(false)
    .description("boolean value to define is managed identity is used.")
    .build();
  public static final Option<String> CLOUD_PROVIDER = new OptionBuilder<>("cloud-provider", String.class)
    .category(OptionCategory.DATABASE_IDENTITY)
    .defaultValue("")
    .description("boolean value to define is managed identity is used.")
    .build();

  public static final Option<String> AWS_REGION = new OptionBuilder<>("aws-region", String.class)
    .category(OptionCategory.DATABASE_IDENTITY)
    .defaultValue("")
    .description("boolean value to define is managed identity is used.")
    .build();
  public static final Option<String> AWS_PORT = new OptionBuilder<>("aws-port", String.class)
    .category(OptionCategory.DATABASE_IDENTITY)
    .defaultValue("0")
    .description("boolean value to define is managed identity is used.")
    .build();
  public static final Option<String> AWS_HOSTNAME = new OptionBuilder<>("aws-hostname", String.class)
    .category(OptionCategory.DATABASE_IDENTITY)
    .defaultValue("")
    .description("boolean value to define is managed identity is used.")
    .build();
}
