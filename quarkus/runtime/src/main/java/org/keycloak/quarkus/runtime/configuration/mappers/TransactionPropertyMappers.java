package org.keycloak.quarkus.runtime.configuration.mappers;

import io.smallrye.config.ConfigSourceInterceptorContext;

import java.util.Arrays;

public class TransactionPropertyMappers {

    private TransactionPropertyMappers(){}

    public static PropertyMapper[] getTransactionPropertyMappers() {
        return new PropertyMapper[] {
                builder().from("transaction-xa-enabled")
                        .to("quarkus.datasource.jdbc.transactions")
                        .defaultValue(Boolean.TRUE.toString())
                        .description("Manually override the transaction type. Transaction type XA and the appropriate driver is used by default.")
                        .paramLabel(Boolean.TRUE + "|" + Boolean.FALSE)
                        .expectedValues(Arrays.asList(Boolean.TRUE.toString(), Boolean.FALSE.toString()))
                        .isBuildTimeProperty(true)
                        .transformer(TransactionPropertyMappers::getQuarkusTransactionsValue)
                        .build(),
        };
    }

    private static String getQuarkusTransactionsValue(String txValue, ConfigSourceInterceptorContext context) {
        boolean isXaEnabled = Boolean.parseBoolean(txValue);

        if (isXaEnabled) {
            return "xa";
        }

        return "enabled";
    }

    private static PropertyMapper.Builder builder() {
        return PropertyMapper.builder(ConfigCategory.TRANSACTION);
    }

}
