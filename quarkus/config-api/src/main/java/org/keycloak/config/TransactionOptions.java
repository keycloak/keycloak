package org.keycloak.config;

import java.util.ArrayList;
import java.util.List;

public class TransactionOptions {

    public static final Option<Boolean> TRANSACTION_XA_ENABLED = new OptionBuilder<>("transaction-xa-enabled", Boolean.class)
            .category(OptionCategory.TRANSACTION)
            .description("If set to false, Keycloak uses a non-XA datasource in case the database does not support XA transactions.")
            .buildTime(true)
            .defaultValue(Boolean.TRUE)
            .build();

    public static final Option<Boolean> TRANSACTION_JTA_ENABLED = new OptionBuilder<>("transaction-jta-enabled", Boolean.class)
            .category(OptionCategory.TRANSACTION)
            .description("Set if distributed transactions are supported. If set to false, transactions are managed by the server and can not be joined if multiple data sources are used. By default, distributed transactions are enabled and only XA data sources can be used.")
            .buildTime(true)
            .hidden()
            .build();

    public static final List<Option<?>> ALL_OPTIONS = new ArrayList<>();

    static {
        ALL_OPTIONS.add(TRANSACTION_XA_ENABLED);
        ALL_OPTIONS.add(TRANSACTION_JTA_ENABLED);
    }
}
