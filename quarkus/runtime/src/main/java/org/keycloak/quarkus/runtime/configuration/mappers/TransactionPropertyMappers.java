package org.keycloak.quarkus.runtime.configuration.mappers;

import io.smallrye.config.ConfigSourceInterceptorContext;

import org.keycloak.config.TransactionOptions;

import static org.keycloak.quarkus.runtime.configuration.mappers.PropertyMapper.fromOption;

public class TransactionPropertyMappers implements PropertyMapperGrouping {

    @Override
    public PropertyMapper<?>[] getPropertyMappers() {
        return new PropertyMapper[] {
                fromOption(TransactionOptions.TRANSACTION_XA_ENABLED)
                        .to("quarkus.datasource.jdbc.transactions")
                        .transformer(TransactionPropertyMappers::getQuarkusTransactionsValue)
                        .build(),
                fromOption(TransactionOptions.TRANSACTION_XA_ENABLED_DATASOURCE)
                        .to("quarkus.datasource.\"<datasource>\".jdbc.transactions")
                        .transformer(TransactionPropertyMappers::getQuarkusTransactionsValue)
                        .build()
        };
    }

    private static String getQuarkusTransactionsValue(String txValue, ConfigSourceInterceptorContext context) {
        boolean isXaEnabled = Boolean.parseBoolean(txValue);

        if (isXaEnabled) {
            return "xa";
        }

        return "enabled";
    }
}
