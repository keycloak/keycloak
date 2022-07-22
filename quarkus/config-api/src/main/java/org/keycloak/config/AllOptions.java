package org.keycloak.config;

import java.util.ArrayList;
import java.util.List;

public class AllOptions {

    public static final List<Option<?>> ALL_OPTIONS = new ArrayList<>();

    static {
        ALL_OPTIONS.addAll(CachingOptions.ALL_OPTIONS);
        ALL_OPTIONS.addAll(DatabaseOptions.ALL_OPTIONS);
        ALL_OPTIONS.addAll(FeatureOptions.ALL_OPTIONS);
        ALL_OPTIONS.addAll(HealthOptions.ALL_OPTIONS);
        ALL_OPTIONS.addAll(HostnameOptions.ALL_OPTIONS);
        ALL_OPTIONS.addAll(HttpOptions.ALL_OPTIONS);
        ALL_OPTIONS.addAll(LoggingOptions.ALL_OPTIONS);
        ALL_OPTIONS.addAll(MetricsOptions.ALL_OPTIONS);
        ALL_OPTIONS.addAll(ProxyOptions.ALL_OPTIONS);
        ALL_OPTIONS.addAll(TransactionOptions.ALL_OPTIONS);
        ALL_OPTIONS.addAll(VaultOptions.ALL_OPTIONS);
        ALL_OPTIONS.addAll(StorageOptions.ALL_OPTIONS);
    }
}
