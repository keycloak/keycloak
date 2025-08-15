package org.keycloak.compatibility;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.keycloak.common.Profile;
import org.keycloak.util.JsonSerialization;

import com.fasterxml.jackson.core.JsonProcessingException;

public class FeatureCompatibilityMetadataProvider implements CompatibilityMetadataProvider {

    public static final String ID = "feature-compatibility";

    @Override
    public Map<String, String> metadata() {
        Set<Profile.Feature> features = Profile.getInstance().getAllFeatures();
        Map<String, String> metadata = new HashMap<>(features.size());
        for (Profile.Feature f : features) {
            Feature feature = Feature.from(f);
            metadata.compute(f.getUnversionedKey(), (k, v) -> {
                if (v == null) {
                    return toJson(feature);
                }
                Feature existing = fromJson(v);
                // Store the latest enabled feature or most recent if no versions are enabled
                if (!existing.enabled || feature.version > existing.version)
                    return toJson(feature);
                return v;
            });
        }
        return metadata;
    }

    @Override
    public CompatibilityResult isCompatible(Map<String, String> other) {
        Map<String, String> currentMeta = metadata();
        // Check all entries in the other metadata
        for (Map.Entry<String, String> entry : other.entrySet()) {
            String featureKey = entry.getKey();
            String otherJson = entry.getValue();
            Feature otherFeature = fromJson(otherJson);

            // Feature has been removed in current version
            if (!currentMeta.containsKey(featureKey)) {
                // Shutdown if the feature was previously enabled and it had the SHUTDOWN strategy
                if (otherFeature.enabled && otherFeature.updatePolicy == Profile.FeatureUpdatePolicy.SHUTDOWN)
                    return CompatibilityResult.incompatibleAttribute(ID, featureKey, otherJson, null);
                else
                    continue;
            }

            String json = currentMeta.get(featureKey);
            Feature feature = fromJson(json);
            // Feature version is different and rolling upgrades are not allowed between versions
            if (feature.version != otherFeature.version && feature.updatePolicy == Profile.FeatureUpdatePolicy.ROLLING_NO_UPGRADE)
                return CompatibilityResult.incompatibleAttribute(ID, featureKey, otherJson, json);

            // Feature has been enabled/disabled
            if (feature.enabled != otherFeature.enabled && feature.updatePolicy == Profile.FeatureUpdatePolicy.SHUTDOWN)
                return CompatibilityResult.incompatibleAttribute(ID, featureKey, otherJson, json);
        }

        // Check distinct entries in current metadata
        Map<String, String> distinct = currentMeta.entrySet().stream()
              .filter(e -> !other.containsKey(e.getKey()))
              .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        for (Map.Entry<String, String> entry : distinct.entrySet()) {
            String json = entry.getValue();
            Feature feature = fromJson(json);
            if (feature.enabled && feature.updatePolicy == Profile.FeatureUpdatePolicy.SHUTDOWN)
                return CompatibilityResult.incompatibleAttribute(ID, entry.getKey(), null, json);
        }
        return CompatibilityResult.providerCompatible(ID);
    }

    @Override
    public String getId() {
        return ID;
    }

    static String toJson(Feature feature) {
        try {
            return JsonSerialization.mapper.writeValueAsString(feature);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Should never happen!", e);
        }
    }

    static Feature fromJson(String json) {
        try {
            return JsonSerialization.mapper.readValue(json, Feature.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Should never happen!", e);
        }
    }

    record Feature(boolean enabled, int version, Profile.FeatureUpdatePolicy updatePolicy) {
        static Feature from(Profile.Feature feature) {
            return new Feature(
                  Profile.isFeatureEnabled(feature),
                  feature.getVersion(),
                  feature.getUpdatePolicy()
            );
        }
    }
}
