package org.keycloak.quarkus.runtime;

import java.util.HashMap;
import java.util.Map;

import org.keycloak.common.profile.SingleProfileConfigResolver;
import org.keycloak.config.FeatureOptions;
import org.keycloak.quarkus.runtime.cli.PropertyException;
import org.keycloak.quarkus.runtime.configuration.Configuration;
import org.keycloak.quarkus.runtime.configuration.mappers.PropertyMappers;

import static org.keycloak.quarkus.runtime.configuration.MicroProfileConfigProvider.NS_KEYCLOAK_PREFIX;

public class QuarkusSingleProfileConfigResolver extends SingleProfileConfigResolver {

    public QuarkusSingleProfileConfigResolver() {
        super(getQuarkusFeatureState());
    }

    protected static Map<String, Boolean> getQuarkusFeatureState() {
        var map = new HashMap<String, Boolean>();
        var wildcard = PropertyMappers.getWildcardPropertyMapper(FeatureOptions.FEATURE).orElseThrow();

        Configuration.getPropertyNames().forEach(property -> {
            if (property.startsWith(NS_KEYCLOAK_PREFIX)) {
                wildcard.extractWildcardValue(property).ifPresent(feature -> {
                    var value = Configuration.getOptionalValue(property).orElseThrow(
                            () -> new PropertyException("Missing value for feature '%s'".formatted(feature)));

                    if (value.startsWith("v")) {
                        map.put(feature + ":" + value, true);
                    } else {
                        map.put(feature, switch (value) {
                            case "enabled" -> Boolean.TRUE;
                            case "disabled" -> Boolean.FALSE;
                            default -> null;
                        });
                    }
                });
            }
        });

        return map;
    }
}
