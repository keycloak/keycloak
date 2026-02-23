package org.keycloak.testframework.server;

import io.quarkus.maven.dependency.ArtifactDependency;
import io.quarkus.maven.dependency.DependencyBuilder;

public class KeycloakDependency extends ArtifactDependency {

    private final boolean hotDeployable;
    private final boolean dependencyCurrentProject;

    private KeycloakDependency(Builder dependencyBuilder) {
        super(dependencyBuilder);
        this.hotDeployable = dependencyBuilder.hotDeployable;
        this.dependencyCurrentProject = dependencyBuilder.dependencyCurrentProject;
    }

    public boolean isHotDeployable() {
        return this.hotDeployable;
    }

    public boolean dependencyCurrentProject() {
        return this.dependencyCurrentProject;
    }

    public static class Builder extends DependencyBuilder {

        private boolean hotDeployable = false;
        private boolean dependencyCurrentProject = false;

        public Builder hotDeployable(boolean hotDeployable) {
            this.hotDeployable = hotDeployable;
            return this;
        }

        public Builder dependencyCurrentProject(boolean dependencyCurrentProject) {
            this.dependencyCurrentProject = dependencyCurrentProject;
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
