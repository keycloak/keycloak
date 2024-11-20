package org.keycloak.test.framework.server;

import io.quarkus.maven.dependency.Dependency;
import io.quarkus.maven.dependency.DependencyBuilder;
import org.keycloak.common.Profile;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class KeycloakTestServerConfigBuilder {

    private Set<Profile.Feature> enabledFeatures = new HashSet<>();
    private Set<Profile.Feature> disabledFeatures = new HashSet<>();
    private Map<String, String> options = new HashMap<>();
    private Set<Dependency> dependencies = new HashSet<>();
    private boolean syslogEnabled;

    public static KeycloakTestServerConfigBuilder create() {
        return new KeycloakTestServerConfigBuilder();
    }

    private KeycloakTestServerConfigBuilder() {
    }

    public KeycloakTestServerConfigBuilder option(String key, String value) {
        options.put(key, value);
        return this;
    }

    public KeycloakTestServerConfigBuilder options(Map<String, String> options) {
        this.options.putAll(options);
        return this;
    }

    public KeycloakTestServerConfigBuilder enableFeatures(Profile.Feature... features) {
        enabledFeatures.addAll(List.of(features));
        return this;
    }

    public KeycloakTestServerConfigBuilder disableFeatures(Profile.Feature... features) {
        disabledFeatures.addAll(List.of(features));
        return this;
    }

    public KeycloakTestServerConfigBuilder dependency(String groupId, String artifactId) {
        dependencies.add(new DependencyBuilder().setGroupId(groupId).setArtifactId(artifactId).build());
        return this;
    }

    public KeycloakTestServerConfigBuilder syslog() {
        syslogEnabled = true;
        return this;
    }

    Set<Profile.Feature> updateCommandBuilder(CommandBuilder commandBuilder) {
        if (!options.isEmpty()) {
            commandBuilder.options(options);
        }
        if (!enabledFeatures.isEmpty()) {
            commandBuilder.features(toFeatureStrings(enabledFeatures));
        }
        if (!disabledFeatures.isEmpty()) {
            commandBuilder.featuresDisabled(toFeatureStrings(disabledFeatures));
        }
        return enabledFeatures;
    }

    boolean isSyslogEnabled() {
        return syslogEnabled;
    }

    Set<Dependency> getDependencies() {
        return dependencies;
    }

    private Set<String> toFeatureStrings(Set<Profile.Feature> features) {
        return features.stream().map(this::toFeatureString).collect(Collectors.toSet());
    }

    private String toFeatureString(Profile.Feature feature) {
        return feature.name().toLowerCase().replace('_', '-');
    }

}
