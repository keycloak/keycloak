package org.keycloak.testframework.server;

import io.quarkus.maven.dependency.ArtifactDependency;
import io.quarkus.maven.dependency.DependencyBuilder;

public class KeycloakDependency extends ArtifactDependency {

    private final boolean hotDeployable;

    private KeycloakDependency(Builder dependencyBuilder) {
        super(dependencyBuilder);
        this.hotDeployable = dependencyBuilder.hotDeployable;
    }

    public boolean isHotDeployable() {
        return this.hotDeployable;
    }

    public static class Builder extends DependencyBuilder {

        private boolean hotDeployable;

        public Builder hotDeployable(boolean hotDeployable) {
            this.hotDeployable = hotDeployable;
            return this;
        }

        @Override
        public Builder setGroupId(String groupId) {
            super.setGroupId(groupId);
            return this;
        }

        @Override
        public Builder setArtifactId(String artifactId) {
            super.setArtifactId(artifactId);
            return this;
        }

        @Override
        public KeycloakDependency build() {
            return new KeycloakDependency(this);
        }

    }
}
