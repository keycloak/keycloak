package org.keycloak.quarkus.runtime;

import org.keycloak.common.Profile;
import org.keycloak.common.profile.CommaSeparatedListProfileConfigResolver;
import org.keycloak.common.profile.ProfileConfigResolver;
import org.keycloak.common.profile.SingleProfileConfigResolver;
import org.keycloak.config.FeatureOptions;
import org.keycloak.quarkus.runtime.configuration.Configuration;
import org.keycloak.quarkus.runtime.configuration.mappers.WildcardPropertyMapper;

import java.util.ArrayList;
import java.util.List;

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

    protected List<SingleProfileConfigResolver.FeatureState> getQuarkusFeatureState() {
        var list = new ArrayList<SingleProfileConfigResolver.FeatureState>();
        var index = FeatureOptions.FEATURE.getKey().indexOf(WildcardPropertyMapper.WILDCARD_FROM_START);
        var featureEnabledOptionPrefix = NS_KEYCLOAK_PREFIX + FeatureOptions.FEATURE.getKey().substring(0, index);

        Configuration.getPropertyNames().forEach(property -> {
            if (property.startsWith(NS_KEYCLOAK_PREFIX) && property.startsWith(featureEnabledOptionPrefix)) {
                var feature = property.substring(featureEnabledOptionPrefix.length());
                list.add(new SingleProfileConfigResolver.FeatureState(
                        feature, Configuration.getOptionalValue(property).orElseThrow(
                                () -> new IllegalArgumentException("Missing value for feature '%s'".formatted(feature))))
                );
            }
        });

        return list;
    }

    @Override
    public Profile.ProfileName getProfileName() {
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
