/*
 * Copyright 2022 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.it.utils;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactDescriptorRequest;
import org.eclipse.aether.resolution.ArtifactDescriptorResult;
import org.eclipse.aether.resolution.ArtifactRequest;

import io.quarkus.bootstrap.resolver.maven.BootstrapMavenContext;
import io.quarkus.bootstrap.resolver.maven.workspace.LocalProject;
import io.quarkus.bootstrap.utils.BuildToolHelper;

public final class Maven {

    public static Path resolveArtifact(String groupId, String artifactId) {
        try {
            Path projectDir = BuildToolHelper.getProjectDir(Paths.get(Maven.class.getResource(".").toURI()));
            BootstrapMavenContext ctx = new BootstrapMavenContext(
                    BootstrapMavenContext.config().setPreferPomsFromWorkspace(true).setWorkspaceModuleParentHierarchy(true)
                            .setCurrentProject(projectDir.toString()));
            LocalProject project = ctx.getCurrentProject();
            RepositorySystem repositorySystem = ctx.getRepositorySystem();
            List<RemoteRepository> remoteRepositories = ctx.getRemoteRepositories();
            ArtifactDescriptorResult descrResult = repositorySystem.readArtifactDescriptor(
                    ctx.getRepositorySystemSession(),
                    new ArtifactDescriptorRequest()
                            .setArtifact(new DefaultArtifact(project.getGroupId(), project.getArtifactId(),                                    "pom", project.getVersion()))
                            .setRepositories(remoteRepositories));

            for (org.eclipse.aether.graph.Dependency dependency : descrResult.getManagedDependencies()) {
                Artifact artifact = dependency.getArtifact();

                if (artifact.getGroupId().equals(groupId) && artifact.getArtifactId().equals(artifactId)) {
                    return repositorySystem.resolveArtifact(
                                    ctx.getRepositorySystemSession(),
                                    new ArtifactRequest().setArtifact(artifact)
                                            .setRepositories(remoteRepositories))
                            .getArtifact().getFile().toPath();
                }
            }
        } catch (Exception cause) {
            throw new RuntimeException("Failed to resolve artifact", cause);
        }

        return null;
    }
}
