package org.keycloak.quarkus.runtime.configuration;

import java.util.Set;
import java.util.regex.Pattern;

import org.eclipse.microprofile.config.spi.ConfigSource;

/**
 * Emulates the default values of the Quarkus config mappings, which the augmentation supplies through a config source
 * of the lowest ordinal for any persistence unit name, e.g. {@code quarkus.hibernate-orm."<unit>".query.query-plan-cache-max-size}
 * is {@code 2048} for every unit. Like the Quarkus source, it does not enumerate property names.
 */
public class QuarkusDefaultsTestConfigSource implements ConfigSource {

    public static final String QUERY_PLAN_CACHE_MAX_SIZE_DEFAULT = "2048";

    private static final Pattern QUERY_PLAN_CACHE_MAX_SIZE = Pattern.compile("quarkus\\.hibernate-orm\\.(\"[^\"]+\"\\.)?query\\.query-plan-cache-max-size");

    @Override
    public Set<String> getPropertyNames() {
        return Set.of();
    }

    @Override
    public String getValue(String propertyName) {
        return QUERY_PLAN_CACHE_MAX_SIZE.matcher(propertyName).matches() ? QUERY_PLAN_CACHE_MAX_SIZE_DEFAULT : null;
    }

    @Override
    public String getName() {
        return "QuarkusDefaultsTestConfigSource";
    }

    @Override
    public int getOrdinal() {
        return Integer.MIN_VALUE;
    }
}
