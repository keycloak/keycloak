package org.keycloak.compatibility;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.keycloak.common.Profile;
import org.keycloak.common.profile.ProfileConfigResolver;

public class FeatureCompatibilityMetadataProviderTest extends AbstractCompatibilityMetadataProviderTest {

    private static final Map<Profile.Feature, Set<Profile.Feature>> DEPENDENT_FEATURES = new HashMap<>();
    static {
        Arrays.stream(Profile.Feature.values())
              .filter(f -> !f.getDependencies().isEmpty())
              .forEach(f -> {
                  for (Profile.Feature dep : f.getDependencies())
                      DEPENDENT_FEATURES.compute(dep, (k, v) -> v == null ? new HashSet<>() : v).add(f);
              });
    }

    @ParameterizedTest
    @MethodSource("rollingFeatures")
    public void testRollingPolicy(Profile.Feature feature) {
        FeatureCompatibilityMetadataProvider provider = new FeatureCompatibilityMetadataProvider();
        Profile.configure(new FeatureResolver(feature, true));

        var f = FeatureCompatibilityMetadataProvider.Feature.from(feature);
        assertFeature(f, true, feature.getVersion(), Profile.FeatureUpdatePolicy.ROLLING);
        assertCompatibility(CompatibilityResult.ExitCode.ROLLING, provider.isCompatible(Map.of(feature.getUnversionedKey(), FeatureCompatibilityMetadataProvider.toJson(f))));

        Profile.configure(new FeatureResolver(feature, false));
        f = FeatureCompatibilityMetadataProvider.Feature.from(feature);
        assertFeature(f, false, feature.getVersion(), Profile.FeatureUpdatePolicy.ROLLING);
        var featureDisabledMap = Map.of(feature.getUnversionedKey(), FeatureCompatibilityMetadataProvider.toJson(f));
        Profile.reset();
        Profile.configure();
        assertCompatibility(CompatibilityResult.ExitCode.ROLLING, provider.isCompatible(featureDisabledMap));
    }

    private static Stream<Arguments> rollingFeatures() {
        return Arrays.stream(Profile.Feature.values())
              .filter(f -> f != Profile.Feature.HOSTNAME_V2)
              .filter(f -> f.getUpdatePolicy() == Profile.FeatureUpdatePolicy.ROLLING)
              .map(Arguments::of);
    }

    @ParameterizedTest
    @MethodSource("shutdownFeatures")
    public void testFeatureShutdownPolicy(Profile.Feature feature) {
        FeatureCompatibilityMetadataProvider provider = new FeatureCompatibilityMetadataProvider();

        // Test both old and new have enabled feature resulting in Rolling result
        Profile.configure(new FeatureResolver(feature, true));
        var f = FeatureCompatibilityMetadataProvider.Feature.from(feature);
        assertFeature(f, true, feature.getVersion(), Profile.FeatureUpdatePolicy.SHUTDOWN);

        var featureEnabledMap = Map.of(feature.getUnversionedKey(), FeatureCompatibilityMetadataProvider.toJson(f));
        assertEquals(Profile.FeatureUpdatePolicy.SHUTDOWN, f.updatePolicy());
        assertCompatibility(CompatibilityResult.ExitCode.ROLLING, provider.isCompatible(featureEnabledMap));

        // Test new metadata has feature enabled and old metadata has feature disabled results in Shutdown
        Profile.configure(new FeatureResolver(feature, false));
        f = FeatureCompatibilityMetadataProvider.Feature.from(feature);
        assertFeature(f, false, feature.getVersion(), Profile.FeatureUpdatePolicy.SHUTDOWN);
        var featureDisabledMap = Map.of(feature.getUnversionedKey(), FeatureCompatibilityMetadataProvider.toJson(f));
        Profile.reset();
        Profile.configure(new FeatureResolver(feature, true));
        assertCompatibility(CompatibilityResult.ExitCode.RECREATE, provider.isCompatible(featureDisabledMap));

        // Test old metadata has feature enabled and new metadata has feature disabled results in Shutdown
        Profile.reset();
        Profile.configure(new FeatureResolver(feature, false));
        assertCompatibility(CompatibilityResult.ExitCode.RECREATE, provider.isCompatible(featureEnabledMap));
    }

    private static Stream<Arguments> shutdownFeatures() {
        return Arrays.stream(Profile.Feature.values())
              .filter(f -> f != Profile.Feature.HOSTNAME_V2)
              .filter(f -> f.getUpdatePolicy() == Profile.FeatureUpdatePolicy.SHUTDOWN)
              .map(Arguments::of);
    }

    @ParameterizedTest
    @MethodSource("noUpgradeFeatures")
    public void testRollingNoUpgradePolicy(Profile.Feature v1, Profile.Feature v2) {
        FeatureCompatibilityMetadataProvider provider = new FeatureCompatibilityMetadataProvider();
        Profile.configure(new FeatureResolver(v1, true));

        var f = FeatureCompatibilityMetadataProvider.Feature.from(v1);
        assertFeature(f, true, 1, Profile.FeatureUpdatePolicy.ROLLING_NO_UPGRADE);
        var featureV1Map = Map.of(v1.getUnversionedKey(), FeatureCompatibilityMetadataProvider.toJson(f));

        Profile.reset();
        Profile.configure(new FeatureResolver(v2, true));
        f = FeatureCompatibilityMetadataProvider.Feature.from(v2);
        assertFeature(f, true, 2, Profile.FeatureUpdatePolicy.ROLLING_NO_UPGRADE);
        assertCompatibility(CompatibilityResult.ExitCode.RECREATE, provider.isCompatible(featureV1Map));
    }

    private static Stream<Arguments> noUpgradeFeatures() {
        return Stream.of(
              Arguments.of(Profile.Feature.ADMIN_FINE_GRAINED_AUTHZ, Profile.Feature.ADMIN_FINE_GRAINED_AUTHZ_V2),
              Arguments.of(Profile.Feature.LOGIN_V1, Profile.Feature.LOGIN_V2)
        );
    }

    @Test
    public void testRemovedFeatureCausesShutdown() {
        FeatureCompatibilityMetadataProvider provider = new FeatureCompatibilityMetadataProvider();
        Profile.configure();
        var featureJson = "{\"enabled\":true,\"version\":1,\"updatePolicy\":\"SHUTDOWN\"}";
        assertCompatibility(CompatibilityResult.ExitCode.RECREATE, provider.isCompatible(Map.of("deleted-feature", featureJson)));
    }

    private void assertFeature(FeatureCompatibilityMetadataProvider.Feature feature, boolean enabled, int version, Profile.FeatureUpdatePolicy updatePolicy) {
        assertEquals(enabled, feature.enabled());
        assertEquals(version, feature.version());
        assertEquals(updatePolicy, feature.updatePolicy());
    }

    record FeatureResolver(Profile.Feature feature, boolean enabled) implements ProfileConfigResolver {
        @Override
        public Profile.ProfileName getProfileName() {
            return null;
        }

        @Override
        public FeatureConfig getFeatureConfig(String featureName) {
            if (enabled) {
                if (DEPENDENT_FEATURES.containsKey(feature)) {
                    for (Profile.Feature dep : DEPENDENT_FEATURES.get(feature)) {
                        if (dep.getVersionedKey().equals(featureName))
                            return FeatureConfig.ENABLED;
                    }
                }
                return feature.getVersionedKey().equals(featureName) ? FeatureConfig.ENABLED : FeatureConfig.UNCONFIGURED;
            } else {
                if (DEPENDENT_FEATURES.containsKey(feature)) {
                    for (Profile.Feature dep : DEPENDENT_FEATURES.get(feature)) {
                        if (dep.getUnversionedKey().equals(featureName))
                            return FeatureConfig.DISABLED;
                    }
                }
                return feature.getUnversionedKey().equals(featureName) ? FeatureConfig.DISABLED : FeatureConfig.UNCONFIGURED;
            }
        }
    }
}
