package org.keycloak.quarkus.runtime;

import org.keycloak.common.Profile;
import org.keycloak.common.profile.CommaSeparatedListProfileConfigResolver;
import org.keycloak.common.profile.ProfileConfigResolver;
import org.keycloak.common.profile.SingleProfileConfigResolver;
import org.keycloak.config.FeatureOptions;
import org.keycloak.config.WildcardOptionsUtil;
import org.keycloak.quarkus.runtime.cli.PropertyException;
import org.keycloak.quarkus.runtime.configuration.Configuration;

import java.util.HashMap;
import java.util.Map;

import static org.keycloak.quarkus.runtime.configuration.MicroProfileConfigProvider.NS_KEYCLOAK_PREFIX;

public class QuarkusProfileConfigResolver implements ProfileConfigResolver {
    private final CommaSeparatedListProfileConfigResolver commaSeparatedResolver;
    private final SingleProfileConfigResolver singleResolver;

    public QuarkusProfileConfigResolver() {
        this.commaSeparatedResolver = new CommaSeparatedListProfileConfigResolver(getConfig("kc.features"), getConfig("kc.features-disabled"));
        this.singleResolver = new SingleProfileConfigResolver(getQuarkusFeatureState());
    }

    static String getConfig(String key) {
        return Configuration.getRawPersistedProperty(key)
                .orElse(Configuration.getConfigValue(key).getValue());
    }

    protected Map<String, Boolean> getQuarkusFeatureState() {
        var map = new HashMap<String, Boolean>();
        var featureEnabledOptionPrefix = NS_KEYCLOAK_PREFIX + WildcardOptionsUtil.getWildcardPrefix(FeatureOptions.FEATURE.getKey());

        Configuration.getPropertyNames().forEach(property -> {
            if (property.startsWith(NS_KEYCLOAK_PREFIX) && property.startsWith(featureEnabledOptionPrefix)) {
                var feature = WildcardOptionsUtil.getWildcardValue(FeatureOptions.FEATURE, property);
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
            }
        });

        return map;
    }

    @Override
    public Profile.ProfileName getProfileName() {
        var singleConfig = singleResolver.getProfileName();
        if (singleConfig != null) {
            return singleConfig;
        }
        return commaSeparatedResolver.getProfileName();
    }

    @Override
    public FeatureConfig getFeatureConfig(String feature) {
        var singleConfig = singleResolver.getFeatureConfig(feature);
        if (singleConfig != FeatureConfig.UNCONFIGURED) {
            return singleConfig;
        }
        return commaSeparatedResolver.getFeatureConfig(feature);
    }
}
