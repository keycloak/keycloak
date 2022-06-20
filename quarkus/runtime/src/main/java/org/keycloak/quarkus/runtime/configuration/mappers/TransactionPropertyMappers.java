package org.keycloak.quarkus.runtime.configuration.mappers;

import io.smallrye.config.ConfigSourceInterceptorContext;
import io.smallrye.config.ConfigValue;
import org.keycloak.config.TransactionOptions;

import static org.keycloak.quarkus.runtime.configuration.mappers.PropertyMapper.fromOption;

public class TransactionPropertyMappers {

    private static final String QUARKUS_TXPROP_TARGET = "quarkus.datasource.jdbc.transactions";

    private TransactionPropertyMappers(){}

    public static PropertyMapper[] getTransactionPropertyMappers() {
        return new PropertyMapper[] {
                fromOption(TransactionOptions.TRANSACTION_XA_ENABLED)
                        .to(QUARKUS_TXPROP_TARGET)
                        .paramLabel(Boolean.TRUE + "|" + Boolean.FALSE)
                        .transformer(TransactionPropertyMappers::getQuarkusTransactionValue)
                        .build(),
                fromOption(TransactionOptions.TRANSACTION_JTA_ENABLED)
                        .paramLabel(Boolean.TRUE + "|" + Boolean.FALSE)
                        .transformer(TransactionPropertyMappers::getQuarkusTransactionValue)
                        .build()
        };
    }

    private static String getQuarkusTransactionValue(String txValue, ConfigSourceInterceptorContext context) {

        boolean isXaEnabled = getBooleanValue("kc.transaction-xa-enabled", context, true);
        boolean isJtaEnabled = getBooleanValue("kc.transaction-jta-enabled", context, true);

        if (!isJtaEnabled) {
            return "disabled";
        }

        if (isXaEnabled) {
            return "xa";
        }

        return "enabled";
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
