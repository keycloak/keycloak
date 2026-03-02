package org.keycloak.quarkus.runtime.configuration.mappers;

import java.time.Duration;
import java.util.List;

import org.keycloak.config.Option;
import org.keycloak.config.OptionsUtil;
import org.keycloak.config.TransactionOptions;
import org.keycloak.quarkus.runtime.cli.PropertyException;

import io.quarkus.runtime.configuration.DurationConverter;
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
                        .build(),
                fromOption(TransactionOptions.TRANSACTION_DEFAULT_TIMEOUT)
                        .to("quarkus.transaction-manager.default-transaction-timeout")
                        .validator(value -> validateDuration(TransactionOptions.TRANSACTION_DEFAULT_TIMEOUT, value))
                        .paramLabel("timeout")
                        .build(),
                fromOption(TransactionOptions.TRANSACTION_MIGRATION_TIMEOUT)
                        .to("kc.spi-connections-jpa--quarkus--migration-transaction-timeout")
                        .validator(value -> validateDuration(TransactionOptions.TRANSACTION_MIGRATION_TIMEOUT, value))
                        .paramLabel("timeout")
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

    private static void validateDuration(Option<?> option, String value) {
        try {
            Duration duration = DurationConverter.parseDuration(value);
            if (duration == null || duration.isNegative() || duration.isZero()) {
                throw new PropertyException("Invalid duration '%s' for option '%s. Duration must be positive.".formatted(value, option.getKey()));
            }
        } catch (IllegalArgumentException e) {
            throw new PropertyException("Invalid duration format '%s' for option '%s'. %s".formatted(value, option.getKey(), OptionsUtil.DURATION_DESCRIPTION));
        }
    }
}
