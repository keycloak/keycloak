package org.keycloak.config;

import java.util.ArrayList;
import java.util.List;

public class TransactionOptions {

    public static final Option<Boolean> TRANSACTION_XA_ENABLED = new OptionBuilder<>("transaction-xa-enabled", Boolean.class)
            .category(OptionCategory.TRANSACTION)
            .description("Manually override the transaction type. Transaction type XA and the appropriate driver is used by default.")
            .buildTime(true)
            .defaultValue(Boolean.TRUE)
            .expectedValues(Boolean.TRUE, Boolean.FALSE)
            .build();

    public static final List<Option<?>> ALL_OPTIONS = new ArrayList<>();

    static {
         ALL_OPTIONS.add(TRANSACTION_XA_ENABLED);
    }
}
