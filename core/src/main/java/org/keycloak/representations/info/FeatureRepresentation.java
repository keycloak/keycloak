package org.keycloak.representations.info;

import org.keycloak.common.Profile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class FeatureRepresentation {
    private String name;
    private String label;
    private Type type;
    private boolean isEnabled;
    private Set<String> dependencies;

    public FeatureRepresentation() {
    }

    public FeatureRepresentation(Profile.Feature feature, boolean isEnabled) {
        this.name = feature.name();
        this.label = feature.getLabel();
        this.type = Type.valueOf(feature.getType().name());
        this.isEnabled = isEnabled;
        this.dependencies = feature.getDependencies() != null ?
                feature.getDependencies().stream().map(Enum::name).collect(Collectors.toSet()) : Collections.emptySet();
    }

    public static List<FeatureRepresentation> create() {
        List<FeatureRepresentation> featureRepresentationList = new ArrayList<>();
        Profile profile = Profile.getInstance();
        final Map<Profile.Feature, Boolean> features = profile.getFeatures();
        features.forEach((f, enabled) -> featureRepresentationList.add(new FeatureRepresentation(f, enabled)));
        return featureRepresentationList;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public boolean isEnabled() {
        return isEnabled;
    }

    public void setEnabled(boolean enabled) {
        isEnabled = enabled;
    }

    public Set<String> getDependencies() {
        return dependencies;
    }

    public void setDependencies(Set<String> dependencies) {
        this.dependencies = dependencies;
    }
}

enum Type {
    DEFAULT,
    DISABLED_BY_DEFAULT,
    PREVIEW,
    PREVIEW_DISABLED_BY_DEFAULT,
    EXPERIMENTAL,
    DEPRECATED;
}