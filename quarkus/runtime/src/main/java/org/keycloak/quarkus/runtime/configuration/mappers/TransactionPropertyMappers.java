package org.keycloak.quarkus.runtime.configuration.mappers;

import java.util.List;

import org.keycloak.config.TransactionOptions;

import io.smallrye.config.ConfigSourceInterceptorContext;

import static org.keycloak.quarkus.runtime.configuration.mappers.PropertyMapper.fromOption;

public class TransactionPropertyMappers implements PropertyMapperGrouping {

    @Override
    public List<PropertyMapper<?>> getPropertyMappers() {
        return List.of(
                fromOption(TransactionOptions.TRANSACTION_XA_ENABLED)
                        .to("quarkus.datasource.jdbc.transactions")
                        .transformer(TransactionPropertyMappers::getQuarkusTransactionsValue)
                        .build(),
                fromOption(TransactionOptions.TRANSACTION_XA_ENABLED_DATASOURCE)
                        .to("quarkus.datasource.\"<datasource>\".jdbc.transactions")
                        .transformer(TransactionPropertyMappers::getQuarkusTransactionsValue)
                        .build()
        );
    }

    private static String getQuarkusTransactionsValue(String txValue, ConfigSourceInterceptorContext context) {
        boolean isXaEnabled = Boolean.parseBoolean(txValue);

        if (isXaEnabled) {
            return "xa";
        }

        return "enabled";
    }
}
