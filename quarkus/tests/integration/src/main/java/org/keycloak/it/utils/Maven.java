/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates
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

import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.resolution.ArtifactResult;

import io.quarkus.bootstrap.resolver.maven.BootstrapMavenContext;
import io.quarkus.bootstrap.resolver.maven.MavenArtifactResolver;
import io.quarkus.bootstrap.resolver.maven.workspace.LocalProject;
import io.quarkus.bootstrap.utils.BuildToolHelper;

public final class Maven {

    private static Path resolveProjectDir() {
        try {
            String classFilePath = KeycloakDistribution.class.getName().replace(".", "/") + ".class";
            URL classFileResource = Thread.currentThread().getContextClassLoader().getResource(classFilePath);
            String classPath = classFileResource.getPath();
            classPath = classPath.substring(0, classPath.length() - classFilePath.length());
            URL newResource = new URL(classFileResource.getProtocol(), classFileResource.getHost(), classFileResource.getPort(),
                    classPath);

            return BuildToolHelper.getProjectDir(Paths.get(newResource.toURI()));
        } catch (Exception cause) {
            throw new RuntimeException("Failed to resolve project dir", cause);
        }
    }

    static Optional<Artifact> resolveArtifact(String groupId, String artifactId) {
        return resolveArtifact(groupId, artifactId, "jar");
    }

    static Optional<Artifact> resolveArtifact(String groupId, String artifactId, String extension) {
        BootstrapMavenContext mvnCtx = createBootstrapContext();
        LocalProject project = mvnCtx.getCurrentProject();

        try {
            MavenArtifactResolver mvnResolver = new MavenArtifactResolver(mvnCtx);
            ArtifactResult resolve = mvnResolver.resolve(new DefaultArtifact(groupId, artifactId, extension,
                    project.getVersion()));

            if (resolve.isResolved()) {
                return Optional.of(resolve.getArtifact());
            }
        } catch (Exception cause) {
            throw new RuntimeException("Failed to resolve project artifact [" + groupId + ":" + artifactId + ":" + project.getVersion() + ":" + extension, cause);
        }

        return Optional.empty();
    }

    private static BootstrapMavenContext createBootstrapContext() {
        try {
            return new BootstrapMavenContext(BootstrapMavenContext.config().setCurrentProject(resolveProjectDir().toString()));
        } catch (Exception cause) {
            throw new RuntimeException("Failed to create maven boorstrap context", cause);
        }
    }
}
