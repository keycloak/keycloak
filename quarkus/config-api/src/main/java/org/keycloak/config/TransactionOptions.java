package org.keycloak.config;

public class TransactionOptions {

    public static final Option<Boolean> TRANSACTION_XA_ENABLED = new OptionBuilder<>("transaction-xa-enabled", Boolean.class)
            .category(OptionCategory.TRANSACTION)
            .description("If set to false, Keycloak uses a non-XA datasource in case the database does not support XA transactions.")
            .buildTime(true)
            .defaultValue(Boolean.TRUE)
            .build();
}
