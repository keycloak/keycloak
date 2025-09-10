package org.keycloak.quarkus.runtime.configuration.mappers;

import io.smallrye.config.ConfigSourceInterceptorContext;

import org.keycloak.config.TransactionOptions;

import static org.keycloak.quarkus.runtime.configuration.mappers.PropertyMapper.fromOption;

import java.util.List;

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
                        .transformer(TransactionPropertyMappers::getQuarkusTransactionsValueDatasource)
                        .allowQuarkusPropertiesFallback(true) // we allow to use properties in quarkus.properties due to backwards compatibility for datasources
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

    // temporarily allow to get direct values from the Quarkus property for the datasource
    private static String getQuarkusTransactionsValueDatasource(String name, String txValue, ConfigSourceInterceptorContext context) {
        return switch (txValue) {
            case "true", "xa" -> "xa";
            case "disabled" -> "disabled";
            default -> "enabled";
        };
    }
}
