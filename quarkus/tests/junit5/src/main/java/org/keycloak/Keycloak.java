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

package org.keycloak;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;

import org.keycloak.common.Version;
import org.keycloak.common.crypto.FipsMode;
import org.keycloak.config.HttpOptions;
import org.keycloak.config.LoggingOptions;
import org.keycloak.config.Option;
import org.keycloak.config.SecurityOptions;
import org.keycloak.platform.Platform;
import org.keycloak.quarkus.runtime.Environment;
import org.keycloak.quarkus.runtime.cli.Picocli;
import org.keycloak.quarkus.runtime.configuration.ConfigArgsConfigSource;
import org.keycloak.quarkus.runtime.configuration.Configuration;
import org.keycloak.quarkus.runtime.configuration.IgnoredArtifacts;

import io.quarkus.bootstrap.app.AugmentAction;
import io.quarkus.bootstrap.app.CuratedApplication;
import io.quarkus.bootstrap.app.QuarkusBootstrap;
import io.quarkus.bootstrap.app.RunningQuarkusApplication;
import io.quarkus.bootstrap.app.StartupAction;
import io.quarkus.bootstrap.forkjoin.QuarkusForkJoinWorkerThreadFactory;
import io.quarkus.bootstrap.model.ApplicationModel;
import io.quarkus.bootstrap.resolver.AppModelResolverException;
import io.quarkus.bootstrap.resolver.BootstrapAppModelResolver;
import io.quarkus.bootstrap.resolver.maven.BootstrapMavenException;
import io.quarkus.bootstrap.resolver.maven.MavenArtifactResolver;
import io.quarkus.bootstrap.workspace.WorkspaceModule;
import io.quarkus.bootstrap.workspace.WorkspaceModuleId;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.maven.dependency.DependencyBuilder;
import io.quarkus.runtime.configuration.QuarkusConfigFactory;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;

import static java.util.Optional.ofNullable;

public class Keycloak {

    static {
        System.setProperty("java.util.logging.manager", "org.jboss.logmanager.LogManager");
        System.setProperty(Environment.KC_CONFIG_BUILT, "true");
        System.setProperty("quarkus.http.test-port", "${kc.http-port}");
        System.setProperty("quarkus.http.test-ssl-port", "${kc.https-port}");
        System.setProperty("java.util.concurrent.ForkJoinPool.common.threadFactory", QuarkusForkJoinWorkerThreadFactory.class.getName());
    }

    public static void main(String[] args) {
        Keycloak.builder().start(args);
    }

    public static class Builder {

        private String version;
        private Path homeDir;
        private List<Dependency> dependencies = new ArrayList<>();

        private Builder() {

        }

        public Builder setVersion(String version) {
            this.version = version;
            return this;
        }

        public Builder setHomeDir(Path path) {
            this.homeDir = path;
            return this;
        }

        public Builder addDependency(String groupId, String artifactId, String version) {
            addDependency(groupId, artifactId, version, null);
            return this;
        }

        public Builder addDependency(String groupId, String artifactId, String version, String classifier) {
            this.dependencies.add(DependencyBuilder.newInstance()
                    .setGroupId(groupId)
                    .setArtifactId(artifactId)
                    .setVersion(version)
                    .setClassifier(classifier)
                    .build());
            return this;
        }

        public Keycloak start(String... args) {
            return start(List.of(args));
        }

        public Keycloak start(List<String> rawArgs) {
            if (homeDir == null) {
                homeDir = Platform.getPlatform().getTmpDirectory().toPath();
            }

            List<String> args = new ArrayList<>(rawArgs);
            if (args.isEmpty()) {
                args.add("start-dev");
            }

            addOptionIfNotSet(args, HttpOptions.HTTP_ENABLED, true);
            addOptionIfNotSet(args, HttpOptions.HTTP_PORT);
            addOptionIfNotSet(args, HttpOptions.HTTPS_PORT);

            boolean isFipsEnabled = ofNullable(getOptionValue(args, SecurityOptions.FIPS_MODE)).map(FipsMode::valueOfOption).orElse(FipsMode.DISABLED).isFipsEnabled();

            if (isFipsEnabled) {
                String logLevel = getOptionValue(args, LoggingOptions.LOG_LEVEL);

                if (logLevel == null) {
                    args.add("--log-level=org.keycloak.common.crypto:TRACE,org.keycloak.crypto:TRACE");
                }
            }

            return new Keycloak(homeDir, version, dependencies, isFipsEnabled).start(args);
        }

        private <T> void addOptionIfNotSet(List<String> args, Option<T> option) {
            addOptionIfNotSet(args, option, null);
        }

        private <T> void addOptionIfNotSet(List<String> args, Option<T> option, T defaultValue) {
            String value = getOptionValue(args, option);

            if (value == null) {
                defaultValue = ofNullable(defaultValue).orElseGet(option.getDefaultValue()::get);
                args.add(Configuration.toCliFormat(option.getKey()) + "=" + Option.getDefaultValueString(defaultValue));
            }
        }

        private String getOptionValue(List<String> args, Option<?> option) {
            for (String arg : args) {
                if (arg.contains(option.getKey())) {
                    if (arg.endsWith(option.getKey())) {
                        throw new IllegalArgumentException("Option '" + arg + "' value must be set using '=' as a separator");
                    }

                    return arg.substring(Picocli.ARG_PREFIX.length() + option.getKey().length() + 1);
                }
            }

            return null;
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    private CuratedApplication curated;
    private RunningQuarkusApplication application;
    private ApplicationModel applicationModel;
    private Path homeDir;
    private List<Dependency> dependencies;
    private boolean fipsEnabled;

    public Keycloak() {
        this(null, Version.VERSION, List.of(), false);
    }

    public Keycloak(Path homeDir, String version, List<Dependency> dependencies, boolean fipsEnabled) {
        this.homeDir = homeDir;
        this.dependencies = dependencies;
        this.fipsEnabled = fipsEnabled;
        try {
            applicationModel = createApplicationModel(version);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Keycloak start(List<String> args) {
        QuarkusBootstrap.Builder builder = QuarkusBootstrap.builder()
                .setExistingModel(applicationModel)
                .setApplicationRoot(applicationModel.getApplicationModule().getModuleDir().toPath())
                .setTargetDirectory(applicationModel.getApplicationModule().getModuleDir().toPath())
                .setIsolateDeployment(true)
                .setFlatClassPath(true)
                .setMode(QuarkusBootstrap.Mode.TEST);

        try {
            curated = builder.build().bootstrap();
            AugmentAction action = curated.createAugmentor();
            Environment.setHomeDir(homeDir);
            ConfigArgsConfigSource.setCliArgs(args.toArray(new String[0]));
            StartupAction startupAction = action.createInitialRuntimeApplication();

            application = startupAction.runMainClass(args.toArray(new String[0]));

            return this;
        } catch (Exception cause) {
            throw new RuntimeException("Fail to start the server", cause);
        }
    }

    public void stop() throws TimeoutException {
        if (isRunning()) {
            closeApplication();
        }
    }

    private ApplicationModel createApplicationModel(String keycloakVersion)
            throws AppModelResolverException {
        // initialize Quarkus application model resolver
        BootstrapAppModelResolver appModelResolver = new BootstrapAppModelResolver(getMavenArtifactResolver());

        // configure server dependencies
        WorkspaceModule module = createWorkspaceModule(keycloakVersion);

        // resolve Keycloak server Quarkus application model
        return appModelResolver.resolveModel(module);
    }

    private WorkspaceModule createWorkspaceModule(String keycloakVersion) {
        Path moduleDir = createModuleDir();
        DependencyBuilder serverDependency = DependencyBuilder.newInstance()
                .setGroupId("org.keycloak")
                .setArtifactId("keycloak-quarkus-server")
                .setVersion(keycloakVersion)
                .addExclusion("org.jboss.logmanager", "log4j-jboss-logmanager");

        if (fipsEnabled) {
            IgnoredArtifacts.FIPS_ENABLED.stream().map(s -> s.split(":")).forEach(d -> serverDependency.addExclusion(d[0], d[1]));
        } else {
            IgnoredArtifacts.FIPS_DISABLED.stream().map(s -> s.split(":")).forEach(d -> serverDependency.addExclusion(d[0], d[1]));
        }

        WorkspaceModule.Mutable builder = WorkspaceModule.builder()
                .setModuleId(WorkspaceModuleId.of("org.keycloak", "keycloak-embedded", "1"))
                .setModuleDir(moduleDir)
                .setBuildDir(moduleDir)
                .addDependencyConstraint(
                        Dependency.pomImport("org.keycloak", "keycloak-quarkus-parent", keycloakVersion))
                .addDependency(serverDependency.build());

        for (Dependency dependency : dependencies) {
            builder.addDependency(dependency);
        }

        return builder.build();
    }

    private static Path createModuleDir() {
        Path moduleDir;

        try {
            moduleDir = Files.createTempDirectory("kc-embedded");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return moduleDir;
    }

    MavenArtifactResolver getMavenArtifactResolver() throws BootstrapMavenException {
        return MavenArtifactResolver.builder()
                .setWorkspaceDiscovery(true)
                .setOffline(false)
                .build();
    }

    private boolean isRunning() {
        return application != null;
    }

    private void closeApplication() {
        if (application != null) {
            try {
                // curated application is also closed
                application.close();
            } catch (Exception cause) {
                cause.printStackTrace();
            }
        }

        QuarkusConfigFactory.setConfig(null);
        ClassLoader old = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(getClass().getClassLoader());

        try {
            ConfigProviderResolver cpr = ConfigProviderResolver.instance();
            cpr.releaseConfig(cpr.getConfig());
        } catch (Throwable ignored) {
            // just means no config was installed, which is fine
        } finally {
            Thread.currentThread().setContextClassLoader(old);
        }

        application = null;
        curated = null;
    }
}
