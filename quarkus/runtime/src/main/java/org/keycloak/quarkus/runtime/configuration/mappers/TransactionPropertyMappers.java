package org.keycloak.quarkus.runtime.configuration.mappers;

import io.smallrye.config.ConfigSourceInterceptorContext;
import io.smallrye.config.ConfigValue;

import org.keycloak.config.StorageOptions;
import org.keycloak.config.TransactionOptions;

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
                        .paramLabel(Boolean.TRUE + "|" + Boolean.FALSE)
                        .transformer(TransactionPropertyMappers::getQuarkusTransactionsValue)
                        .build(),
                fromOption(TransactionOptions.TRANSACTION_JTA_ENABLED)
                        .paramLabel(Boolean.TRUE + "|" + Boolean.FALSE)
                        .transformer(TransactionPropertyMappers::getQuarkusTransactionsValue)
                        .build()
        };
    }

    private static Optional<String> getQuarkusTransactionsValue(Optional<String> txValue, ConfigSourceInterceptorContext context) {
        boolean isXaEnabled = Boolean.parseBoolean(txValue.get());
        boolean isJtaEnabled = getBooleanValue("kc.transaction-jta-enabled", context, true);
        ConfigValue storage = context.proceed(NS_KEYCLOAK_PREFIX.concat(STORAGE.getKey()));

        if (storage != null && StorageOptions.StorageType.jpa.name().equals(storage.getValue())) {
            isJtaEnabled = false;
        }

        if (!isJtaEnabled) {
            return of("disabled");
        }

        if (isXaEnabled) {
            return of("xa");
        }

        return of("enabled");
    }

    private static boolean getBooleanValue(String key, ConfigSourceInterceptorContext context, boolean defaultValue) {
        boolean returnValue = defaultValue;
        ConfigValue configValue = context.proceed(key);

        if (configValue != null) {
            returnValue = Boolean.parseBoolean(configValue.getValue());
        }
        return returnValue;
    }
}
