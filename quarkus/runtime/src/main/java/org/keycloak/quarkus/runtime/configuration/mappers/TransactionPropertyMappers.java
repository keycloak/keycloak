package org.keycloak.quarkus.runtime.configuration.mappers;

import io.smallrye.config.ConfigSourceInterceptorContext;
import io.smallrye.config.ConfigValue;

import org.keycloak.config.StorageOptions;
import org.keycloak.config.TransactionOptions;
import org.keycloak.quarkus.runtime.configuration.Configuration;

import static java.util.Optional.of;
import static org.keycloak.config.StorageOptions.STORAGE;
import static org.keycloak.quarkus.runtime.configuration.MicroProfileConfigProvider.NS_KEYCLOAK_PREFIX;
import static org.keycloak.quarkus.runtime.configuration.mappers.PropertyMapper.fromOption;

import java.util.Optional;

public class TransactionPropertyMappers {

    private static final String QUARKUS_TXPROP_TARGET = "quarkus.datasource.jdbc.transactions";

    private TransactionPropertyMappers(){}

    public static PropertyMapper[] getTransactionPropertyMappers() {
        return new PropertyMapper[] {
                fromOption(TransactionOptions.TRANSACTION_XA_ENABLED)
                        .to(QUARKUS_TXPROP_TARGET)
                        .transformer(TransactionPropertyMappers::getQuarkusTransactionsValue)
                        .build()
        };
    }

    private static Optional<String> getQuarkusTransactionsValue(Optional<String> txValue, ConfigSourceInterceptorContext context) {
        boolean isXaEnabled = Boolean.parseBoolean(txValue.get());
        ConfigValue storage = context.proceed(NS_KEYCLOAK_PREFIX.concat(STORAGE.getKey()));

        if (storage != null && StorageOptions.StorageType.jpa.name().equals(storage.getValue())) {
            isXaEnabled = true;
        }

        if (isXaEnabled) {
            return of("xa");
        }

        return of("enabled");
    }
}
