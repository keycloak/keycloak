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

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import io.quarkus.bootstrap.resolver.maven.BootstrapMavenContext;
import io.quarkus.bootstrap.resolver.maven.BootstrapMavenException;
import io.quarkus.bootstrap.resolver.maven.MavenArtifactResolver;
import io.quarkus.bootstrap.resolver.maven.workspace.LocalProject;
import io.quarkus.bootstrap.utils.BuildToolHelper;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyFilter;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactDescriptorRequest;
import org.eclipse.aether.resolution.ArtifactDescriptorResult;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResolutionException;

public final class Maven {

    public static Path resolveArtifact(String groupId, String artifactId) {
        try {
            BootstrapMavenContext ctx = bootstrapCurrentMavenContext();
            LocalProject project = ctx.getCurrentProject();
            RepositorySystem repositorySystem = ctx.getRepositorySystem();
            List<RemoteRepository> remoteRepositories = ctx.getRemoteRepositories();
            ArtifactDescriptorResult projectDescriptor = repositorySystem.readArtifactDescriptor(
                    ctx.getRepositorySystemSession(),
                    new ArtifactDescriptorRequest()
                            .setArtifact(new DefaultArtifact(project.getGroupId(), project.getArtifactId(), "pom", project.getVersion()))
                            .setRepositories(remoteRepositories));
            List<Dependency> dependencies = new ArrayList<>(projectDescriptor.getDependencies());
            dependencies.addAll(projectDescriptor.getManagedDependencies());
            Artifact artifact = resolveArtifact(groupId, artifactId, dependencies);

            if (artifact == null) {
                artifact = resolveArtifactRecursively(ctx, projectDescriptor, groupId, artifactId);
            }

            if (artifact == null) {
                throw new RuntimeException("Failed to resolve artifact [" + groupId + ":" + artifactId + "] from project [" + projectDescriptor.getArtifact() + "] dependency graph");
            }

            return repositorySystem.resolveArtifact(
                            ctx.getRepositorySystemSession(),
                            new ArtifactRequest().setArtifact(artifact)
                                    .setRepositories(remoteRepositories))
                    .getArtifact().getFile().toPath();
        } catch (Exception cause) {
            throw new RuntimeException("Failed to resolve artifact: " + groupId + ":" + artifactId, cause);
        }
    }

    private static Artifact resolveArtifact(String groupId, String artifactId, List<Dependency> dependencies) {
        for (Dependency dependency : dependencies) {
            Artifact artifact = dependency.getArtifact();

            if (artifact.getGroupId().equals(groupId) && artifact.getArtifactId().equals(artifactId)) {
                return artifact;
            }
        }

        return null;
    }

    private static Artifact resolveArtifactRecursively(BootstrapMavenContext ctx, ArtifactDescriptorResult artifactDescriptor, String groupId, String artifactId) throws BootstrapMavenException, DependencyResolutionException {
        CollectRequest collectRequest = MavenArtifactResolver.newCollectRequest(artifactDescriptor.getArtifact(), artifactDescriptor.getDependencies(),
                List.of(),
                List.of(),
                ctx.getRemoteRepositories());
        List<ArtifactResult> artifactResults = ctx.getRepositorySystem().resolveDependencies(ctx.getRepositorySystemSession(),
                        new DependencyRequest()
                                .setFilter(new DependencyFilter() {
                                    @Override
                                    public boolean accept(DependencyNode node, List<DependencyNode> parents) {
                                        Dependency dependency = node.getDependency();

                                        if (dependency == null) {
                                            return false;
                                        }

                                        Artifact artifact = dependency.getArtifact();

                                        return artifact.getGroupId().equals(groupId)
                                                && artifact.getArtifactId().equals(artifactId);
                                    }
                                })
                                .setCollectRequest(collectRequest))
                .getArtifactResults();

        if (artifactResults.isEmpty()) {
            return null;
        }

        if (artifactResults.size() > 1) {
            throw new RuntimeException("Unexpected number of resolved artifacts: " + artifactResults);
        }

        return artifactResults.get(0).getArtifact();
    }

    public static Path getKeycloakQuarkusModulePath() {
        // Find keycloak-parent module first
        BootstrapMavenContext ctx = null;
        try {
            ctx = bootstrapCurrentMavenContext();
        } catch (BootstrapMavenException | URISyntaxException e) {
            throw new RuntimeException("Failed bootstrap maven context", e);
        }
        for (LocalProject m = ctx.getCurrentProject(); m != null; m = m.getLocalParent()) {
            if ("keycloak-parent".equals(m.getArtifactId())) {
                // When found, advance to quarkus module
                return m.getDir().resolve("quarkus");
            }
        }

        throw new RuntimeException("Failed to find keycloak-parent module.");
    }

    private static BootstrapMavenContext bootstrapCurrentMavenContext() throws BootstrapMavenException, URISyntaxException {
        Path classPathDir = Paths.get(Thread.currentThread().getContextClassLoader().getResource(".").toURI());
        Path projectDir = BuildToolHelper.getProjectDir(classPathDir);
        return new BootstrapMavenContext(
                BootstrapMavenContext.config().setPreferPomsFromWorkspace(true).setWorkspaceModuleParentHierarchy(true)
                        .setCurrentProject(projectDir.toString()));
    }
}
