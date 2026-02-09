package org.keycloak.compatibility;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.keycloak.common.Profile;
import org.keycloak.common.profile.ProfileConfigResolver;

import org.infinispan.commons.util.ReflectionUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

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

        Map<String, String> featureEnabled = getMeta(feature, true);
        Map<String, String> featureDisabled = getMeta(feature, false);

        // Current and other container the same feature
        Profile.configure(new FeatureResolver(feature, true));
        assertFeature(feature, true, feature.getVersion(), Profile.FeatureUpdatePolicy.ROLLING);
        assertCompatibility(CompatibilityResult.ExitCode.ROLLING, provider.isCompatible(featureEnabled));

        // Feature enabled in current, disabled in other
        assertCompatibility(CompatibilityResult.ExitCode.ROLLING, provider.isCompatible(featureDisabled));

        // Feature disabled in current and other
        Profile.reset();
        Profile.configure(new FeatureResolver(feature, false));
        assertFeature(feature, false, feature.getVersion(), Profile.FeatureUpdatePolicy.ROLLING);
        assertCompatibility(CompatibilityResult.ExitCode.ROLLING, provider.isCompatible(featureDisabled));

        // Feature disabled in current, enabled in other
        assertCompatibility(CompatibilityResult.ExitCode.ROLLING, provider.isCompatible(featureEnabled));
    }

    private static Stream<Arguments> rollingFeatures() {
        return Arrays.stream(Profile.Feature.values())
              // It's not possible to disable HOSTNAME_V2 so ignore it when testing
              .filter(f -> f != Profile.Feature.HOSTNAME_V2)
              .filter(f -> f.getUpdatePolicy() == Profile.FeatureUpdatePolicy.ROLLING)
              .filter(f ->
                    // Filter features that have a dependency that does not support Rolling update as these will cause a cluster recreate
                    DEPENDENT_FEATURES.getOrDefault(f, Set.of())
                          .stream()
                          .noneMatch(dep -> dep.getUpdatePolicy() != Profile.FeatureUpdatePolicy.ROLLING))
              .map(Arguments::of);
    }

    @ParameterizedTest
    @MethodSource("shutdownFeatures")
    public void testFeatureShutdownPolicy(Profile.Feature feature) {
        FeatureCompatibilityMetadataProvider provider = new FeatureCompatibilityMetadataProvider();

        Map<String, String> featureEnabled = getMeta(feature, true);
        Map<String, String> featureDisabled = getMeta(feature, false);

        // Test both old and new have enabled feature resulting in Rolling result
        Profile.configure(new FeatureResolver(feature, true));
        assertFeature(feature, true, feature.getVersion(), Profile.FeatureUpdatePolicy.SHUTDOWN);
        assertCompatibility(CompatibilityResult.ExitCode.ROLLING, provider.isCompatible(featureEnabled));

        // Test new metadata has feature enabled and old metadata has feature disabled results in Shutdown
        assertFeature(feature, true, feature.getVersion(), Profile.FeatureUpdatePolicy.SHUTDOWN);
        assertCompatibility(CompatibilityResult.ExitCode.RECREATE, provider.isCompatible(featureDisabled));

        // Test old metadata has feature enabled and new metadata has feature disabled results in Shutdown
        Profile.reset();
        Profile.configure(new FeatureResolver(feature, false));
        assertFeature(feature, false, feature.getVersion(), Profile.FeatureUpdatePolicy.SHUTDOWN);
        assertCompatibility(CompatibilityResult.ExitCode.RECREATE, provider.isCompatible(featureEnabled));
    }

    private static Stream<Arguments> shutdownFeatures() {
        return Arrays.stream(Profile.Feature.values())
              .filter(f -> f != Profile.Feature.HOSTNAME_V2)
              .filter(f -> f.getUpdatePolicy() == Profile.FeatureUpdatePolicy.SHUTDOWN)
              .map(Arguments::of);
    }

    @Test
    public void testRollingNoUpgradePolicy() {
        Profile.Feature v1 = Profile.Feature.LOGIN_V1;
        Profile.Feature v2 = Profile.Feature.LOGIN_V2;

        FeatureCompatibilityMetadataProvider provider = new FeatureCompatibilityMetadataProvider();

        Map<String, String> v1Meta = getMeta(v1, true);
        Map<String, String> v2Meta = getMeta(v2, true);

        // Test v1 enabled switching to v2
        Profile.configure(new FeatureResolver(v1, true));
        assertFeature(v1, true, v1.getVersion(), Profile.FeatureUpdatePolicy.ROLLING_NO_UPGRADE);
        assertFeature(v2, false, v2.getVersion(), Profile.FeatureUpdatePolicy.ROLLING_NO_UPGRADE);
        assertCompatibility(CompatibilityResult.ExitCode.RECREATE, provider.isCompatible(v2Meta));

        // Test v2 enabled switching to v1
        Profile.reset();
        Profile.configure(new FeatureResolver(v2, true));
        assertFeature(v1, false, v1.getVersion(), Profile.FeatureUpdatePolicy.ROLLING_NO_UPGRADE);
        assertFeature(v2, true, v2.getVersion(), Profile.FeatureUpdatePolicy.ROLLING_NO_UPGRADE);
        assertCompatibility(CompatibilityResult.ExitCode.RECREATE, provider.isCompatible(v1Meta));
    }

    @ParameterizedTest
    @MethodSource("addedFeatures")
    public void testAddedFeature(CompatibilityResult.ExitCode exitCode, Profile.Feature featureToAdd) {
        Profile.configure();
        FeatureCompatibilityMetadataProvider provider = new FeatureCompatibilityMetadataProvider();
        Map<String, String> other = provider.metadata();

        // Remove an existing Feature from the profile to emulate a new Profile.Feature being added in a subsequent KC version
        Profile instance = Profile.getInstance();
        Map<Profile.Feature, Boolean> features = new HashMap<>(instance.getFeatures());
        features.remove(featureToAdd);
        Field featuresField = ReflectionUtil.getField("features", Profile.class);
        featuresField.setAccessible(true);
        ReflectionUtil.setField(instance, featuresField, features);
        assertCompatibility(exitCode, provider.isCompatible(other));
    }

    private static Stream<Arguments> addedFeatures() {
        return Stream.of(
              Arguments.of(CompatibilityResult.ExitCode.ROLLING, Profile.Feature.IMPERSONATION),
              Arguments.of(CompatibilityResult.ExitCode.RECREATE, Profile.Feature.PERSISTENT_USER_SESSIONS),
              // Expect a RECREATE as the Feature has the ROLLING_NO_UPGRADE policy
              Arguments.of(CompatibilityResult.ExitCode.RECREATE, Profile.Feature.LOGIN_V2)
        );
    }

    @ParameterizedTest
    @MethodSource("removedFeatures")
    public void testRemovedFeature(CompatibilityResult.ExitCode exitCode, Profile.FeatureUpdatePolicy updatePolicy) {
        FeatureCompatibilityMetadataProvider provider = new FeatureCompatibilityMetadataProvider();
        Profile.configure();
        Map<String, String> other = provider.metadata();
        other.put("deleted-feature", "{\"enabled\":true,\"version\":1,\"updatePolicy\":\"%s\"}".formatted(updatePolicy));
        assertCompatibility(exitCode, provider.isCompatible(other));
    }

    private static Stream<Arguments> removedFeatures() {
        return Stream.of(
              Arguments.of(CompatibilityResult.ExitCode.ROLLING, Profile.FeatureUpdatePolicy.ROLLING),
              Arguments.of(CompatibilityResult.ExitCode.ROLLING, Profile.FeatureUpdatePolicy.ROLLING_NO_UPGRADE),
              Arguments.of(CompatibilityResult.ExitCode.RECREATE, Profile.FeatureUpdatePolicy.SHUTDOWN)
        );
    }

    // Returns the expected metadata taking into account dependent features
    private Map<String, String> getMeta(Profile.Feature feature, boolean enabled) {
        Profile.reset();
        Profile.configure(new FeatureResolver(feature, enabled));
        Map<String, String> meta = new FeatureCompatibilityMetadataProvider().metadata();
        Profile.reset();
        return meta;
    }

    private void assertFeature(Profile.Feature feature, boolean enabled, int version, Profile.FeatureUpdatePolicy updatePolicy) {
        var f = FeatureCompatibilityMetadataProvider.Feature.from(feature);
        assertEquals(enabled, f.enabled());
        assertEquals(version, f.version());
        assertEquals(updatePolicy, f.updatePolicy());
    }

    record FeatureResolver(Profile.Feature feature, boolean enabled) implements ProfileConfigResolver {
        @Override
        public Profile.ProfileName getProfileName() {
            return null;
        }

        @Override
        public FeatureConfig getFeatureConfig(String featureName) {
            // No support for transitive dependencies but that should be fine for now
            if (enabled) {
                if (DEPENDENT_FEATURES.containsKey(feature)) {
                    for (Profile.Feature dep : DEPENDENT_FEATURES.get(feature)) {
                        if (dep.getVersionedKey().equals(featureName))
                            return FeatureConfig.ENABLED;
                    }
                }
                for (Profile.Feature dep : feature.getDependencies()) { // Explicitly enable dependencies that might be disabled by default
                    if (dep.getVersionedKey().equals(featureName))
                        return FeatureConfig.ENABLED;
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
