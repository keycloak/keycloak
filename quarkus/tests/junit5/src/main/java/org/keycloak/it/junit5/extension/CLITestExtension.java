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

package org.keycloak.it.junit5.extension;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.keycloak.it.utils.KeycloakDistribution;
import org.keycloak.it.utils.RawDistRootPath;
import org.keycloak.it.utils.RawKeycloakDistribution;
import org.keycloak.quarkus.runtime.Environment;
import org.keycloak.quarkus.runtime.cli.command.DryRunMixin;
import org.keycloak.quarkus.runtime.cli.command.Start;
import org.keycloak.quarkus.runtime.cli.command.StartDev;
import org.keycloak.quarkus.runtime.configuration.ConfigArgsConfigSource;
import org.keycloak.quarkus.runtime.configuration.Configuration;
import org.keycloak.quarkus.runtime.configuration.KeycloakPropertiesConfigSource;
import org.keycloak.quarkus.runtime.integration.QuarkusPlatform;

import io.quarkus.deployment.util.FileUtil;
import io.quarkus.runtime.configuration.QuarkusConfigFactory;
import io.quarkus.test.junit.QuarkusMainTestExtension;
import io.quarkus.test.junit.main.Launch;
import io.quarkus.test.junit.main.LaunchResult;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.junit.jupiter.api.extension.ExtensionContext.Store;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ReflectiveInvocationContext;

import static java.lang.System.setProperty;

import static org.keycloak.it.junit5.extension.DistributionTest.ReInstall.BEFORE_ALL;
import static org.keycloak.it.junit5.extension.DistributionType.RAW;
import static org.keycloak.quarkus.runtime.Environment.forceTestLaunchMode;
import static org.keycloak.quarkus.runtime.cli.command.Main.CONFIG_FILE_LONG_NAME;
import static org.keycloak.quarkus.runtime.cli.command.Main.CONFIG_FILE_SHORT_NAME;

public class CLITestExtension extends QuarkusMainTestExtension {

    private static final String SYS_PROPS = "sys-props";
    private KeycloakDistribution dist;
    private DatabaseContainer databaseContainer;
    private InfinispanContainer infinispanContainer;
    private CLIResult result;

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        DistributionTest distConfig = getDistributionConfig(context);
        Launch launch = context.getRequiredTestMethod().getAnnotation(Launch.class);
        getStore(context).put(SYS_PROPS, new HashMap<>(System.getProperties()));

        if (launch != null && distConfig == null) {
            ConfigArgsConfigSource.parseConfigArgs(List.of(launch.value()), (arg, value) -> {
                if (arg.equals(CONFIG_FILE_SHORT_NAME) || arg.equals(CONFIG_FILE_LONG_NAME)) {
                    setProperty(KeycloakPropertiesConfigSource.KEYCLOAK_CONFIG_FILE_PROP, value);
                } else if (arg.startsWith("-D")) {
                    setProperty(arg, value);
                }
            }, arg -> {
                if (arg.startsWith("-D")) {
                    setProperty(arg, "");
                }
            });
        }

        configureDatabase(context);
        infinispanContainer = configureExternalInfinispan(context);

        if (distConfig != null) {
            if (dist == null) {
                dist = createDistribution(distConfig, getStoreConfig(context), getDatabaseConfig(context));
            }

            onKeepServerAlive(context.getRequiredTestMethod().getAnnotation(KeepServerAlive.class), true);

            copyTestProvider(context.getRequiredTestClass().getAnnotation(TestProvider.class));
            copyTestProvider(context.getRequiredTestMethod().getAnnotation(TestProvider.class));
            onBeforeStartDistribution(context.getRequiredTestClass().getAnnotation(BeforeStartDistribution.class));
            onBeforeStartDistribution(context.getRequiredTestMethod().getAnnotation(BeforeStartDistribution.class));

            configureEnvVars(context.getRequiredTestClass().getAnnotation(WithEnvVars.class));
            configureEnvVars(context.getRequiredTestMethod().getAnnotation(WithEnvVars.class));
            boolean dryRun = context.getRequiredTestClass().getAnnotation(DryRun.class) != null
                    || context.getRequiredTestMethod().getAnnotation(DryRun.class) != null;
            if (dryRun && isRaw()) {
                dist.setEnvVar(DryRunMixin.KC_DRY_RUN_ENV, "true");
                dist.setEnvVar(DryRunMixin.KC_DRY_RUN_BUILD_ENV, "true");
            }

            if (launch != null) {
                result = dist.run(Stream.concat(List.of(launch.value()).stream(), List.of(distConfig.defaultOptions()).stream()).collect(Collectors.toList()));
            }
        } else {
            ConfigArgsConfigSource.setCliArgs(launch == null ? new String[] {} : launch.value());
            configureProfile(context);
            super.beforeEach(context);
        }
    }

    private Store getStore(ExtensionContext context) {
        return context.getStore(Namespace.create(context.getRequiredTestClass(), context.getRequiredTestMethod()));
    }

    private static Storage getStoreConfig(ExtensionContext context) {
        return context.getTestClass().get().getDeclaredAnnotation(Storage.class);
    }

    private void copyTestProvider(TestProvider provider) {
        if (provider == null) {
            return;
        }

        if (isRaw()) {
            try {
                dist.unwrap(RawKeycloakDistribution.class).copyProvider(provider.value().getDeclaredConstructor().newInstance());
            } catch (Exception cause) {
                throw new RuntimeException("Failed to instantiate test provider: " + provider.getClass(), cause);
            }
        }
    }

    private boolean isRaw() {
        return RAW.equals(DistributionType.getCurrent().orElse(RAW));
    }

    @Override
    public void interceptTestMethod(Invocation<Void> invocation,
            ReflectiveInvocationContext<Method> invocationContext, ExtensionContext extensionContext) throws Throwable {
        if (dist == null) {
            super.interceptTestMethod(invocation, invocationContext, extensionContext);
        } else {
            invocation.proceed();
        }
    }

    private void onBeforeStartDistribution(BeforeStartDistribution annotation) {
        if (annotation != null) {
            try {
                annotation.value().getDeclaredConstructor().newInstance().accept(dist);
            } catch (Exception cause) {
                throw new RuntimeException("Error when invoking " + annotation.value() + " instance before starting distribution", cause);
            }
        }
    }

    private void onKeepServerAlive(KeepServerAlive annotation, boolean setting) {
        if(annotation != null && dist != null) {
            try {
                dist.setManualStop(setting);
            } catch (Exception cause) {
                throw new RuntimeException("Error when invoking " + annotation, cause);
            }
        }
    }

    @Override
    public void afterEach(ExtensionContext context) throws Exception {
        DistributionTest distConfig = getDistributionConfig(context);

        if (dist != null) {
            onKeepServerAlive(context.getRequiredTestMethod().getAnnotation(KeepServerAlive.class), false);
            dist.stop();
            dist.clearEnv();

            if (distConfig != null && DistributionTest.ReInstall.BEFORE_TEST.equals(distConfig.reInstall())) {
                dist = null;
            }
        }

        super.afterEach(context);
        reset(distConfig, context);
    }

    private void reset(DistributionTest distConfig, ExtensionContext context) {
        QuarkusConfigFactory.setConfig(null);
        HashMap props = getStore(context).remove(SYS_PROPS, HashMap.class);
        System.getProperties().clear();
        System.getProperties().putAll(props);
        // TODO: for in-vm tests this is not all that it takes to reset static state
        // may want to call AbstractConfigurationTest.resetConfiguration
        Configuration.resetConfig();
        if (databaseContainer != null && databaseContainer.isRunning()) {
            databaseContainer.stop();
            databaseContainer = null;
        }
        if (infinispanContainer != null) {
            infinispanContainer.stop();
        }
        result = null;
        if (isRaw()) {
            if (distConfig != null && !DistributionTest.ReInstall.NEVER.equals(distConfig.reInstall()) && dist != null) {
                try {
                    FileUtil.deleteDirectory(getDistPath().getDistRootPath().resolve("conf"));
                    getDistPath().getDistRootPath().resolve("conf").toFile().mkdirs();
                    FileUtil.deleteDirectory(getDistPath().getDistRootPath().resolve("providers"));
                    getDistPath().getDistRootPath().resolve("providers").toFile().mkdirs();
                    FileUtil.deleteDirectory(getDistPath().getDistRootPath().resolve("data"));
                    getDistPath().getDistRootPath().resolve("data").toFile().mkdirs();
                } catch (IOException e) {
                    throw new RuntimeException("Failed to delete conf directory");
                }
            }
        }
    }

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        DistributionTest distConfig = getDistributionConfig(context);

        if (distConfig != null) {
            if (BEFORE_ALL.equals(distConfig.reInstall())) {
                dist = createDistribution(distConfig, getStoreConfig(context), getDatabaseConfig(context));
            }
        } else {
            forceTestLaunchMode();
        }

        super.beforeAll(context);
    }

    @Override
    public void afterAll(ExtensionContext context) throws Exception {
        if (dist != null) {
            // just to make sure the server is stopped after all tests
            dist.stop();
        }
        super.afterAll(context);
    }

    private KeycloakDistribution createDistribution(DistributionTest config, Storage storeConfig, WithDatabase databaseConfig) {
        return new KeycloakDistributionDecorator(storeConfig, databaseConfig, config, DistributionType.getCurrent().orElse(RAW).newInstance(config));
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext context)
            throws ParameterResolutionException {
        Class<?> type = parameterContext.getParameter().getType();

        if (type == LaunchResult.class || type == CLIResult.class) {
            boolean isDistribution = getDistributionConfig(context) != null;

            if (isDistribution) {
                return result;
            }

            LaunchResult result = (LaunchResult) super.resolveParameter(parameterContext, context);
            List<String> outputStream = result.getOutputStream();
            List<String> errStream = result.getErrorStream();
            int exitCode = result.exitCode();

            return CLIResult.create(outputStream, errStream, exitCode);
        }

        if (type.equals(RawDistRootPath.class)) {
            //assuming the path to the distribution directory
            return getDistPath();
        }

        if (type.equals(KeycloakDistribution.class)) {
            if (context.getTestClass().orElse(Object.class).getDeclaredAnnotation(DistributionTest.class) == null) {
                throw new RuntimeException("Only tests annotated with " + DistributionTest.class + " can inject a distribution instance");
            }
            return dist;
        }

        // for now, no support for manual launching using QuarkusMainLauncher
        throw new RuntimeException("Parameter type [" + type + "] not supported");
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
            throws ParameterResolutionException {
        Class<?> type = parameterContext.getParameter().getType();
        return type == LaunchResult.class || type == CLIResult.class || type == RawDistRootPath.class || type == KeycloakDistribution.class;
    }

    private void configureProfile(ExtensionContext context) {
        List<String> cliArgs = getCliArgs(context);

        // when running tests, build steps happen before executing our CLI so that profiles are not set and not taken
        // into account when executing the build steps
        // this is basically reproducing the behavior when using kc.sh
        if (cliArgs.contains(Start.NAME)) {
            Environment.setProfile("prod");
        } else if (cliArgs.contains(StartDev.NAME)) {
            Environment.forceDevProfile();
        }
    }

    private void configureDatabase(ExtensionContext context) {
        WithDatabase database = getDatabaseConfig(context);

        if (database != null) {
            if (dist == null) {
                configureDevServices();
                setProperty("kc.db", database.alias());
                setProperty("kc.db-password", DatabaseContainer.DEFAULT_PASSWORD);
            } else {
                databaseContainer = new DatabaseContainer(database.alias());

                databaseContainer.start();

                if (database.buildOptions().length == 0) {
                    dist.setProperty("db", database.alias());
                } else {
                    for (String option : database.buildOptions()) {
                        dist.setProperty(option.substring(0, option.indexOf('=')), option.substring(option.indexOf('=') + 1));
                    }
                }

                databaseContainer.configureDistribution(dist);

                dist.run("build");
            }
        } else {
            // This is for re-creating the H2 database instead of using the default in home
            setProperty("kc.db-url-path", new QuarkusPlatform().getTmpDirectory().getAbsolutePath());
        }
    }

    private static InfinispanContainer configureExternalInfinispan(ExtensionContext context) {
        if (getAnnotationFromTestContext(context, WithExternalInfinispan.class) != null) {
            InfinispanContainer infinispanContainer = new InfinispanContainer();
            infinispanContainer.start();
            return infinispanContainer;
        }

        return null;
    }

    private static WithDatabase getDatabaseConfig(ExtensionContext context) {
        return getAnnotationFromTestContext(context, WithDatabase.class);
    }

    private static <T extends Annotation> T getAnnotationFromTestContext(ExtensionContext context, Class<T> annotationClass) {
        return context.getTestClass().map(c -> c.getDeclaredAnnotation(annotationClass))
                .or(() -> context.getTestMethod().map(m -> m.getAnnotation(annotationClass)))
                .orElse(null);
    }

    private void configureDevServices() {
        setProperty("quarkus.vault.devservices.enabled", Boolean.FALSE.toString());
        setProperty("quarkus.datasource.devservices.enabled", Boolean.TRUE.toString());
        setProperty("quarkus.devservices.enabled", Boolean.TRUE.toString());
    }

    private void configureEnvVars(WithEnvVars envVars) {
        if (envVars == null) {
            return;
        }

        for (int i=0; i<envVars.value().length; i=i+2) {
            dist.setEnvVar(envVars.value()[i], envVars.value()[i+1]);
        }
    }

    private List<String> getCliArgs(ExtensionContext context) {
        Launch annotation = context.getRequiredTestMethod().getAnnotation(Launch.class);

        if (annotation != null) {
            return Arrays.asList(annotation.value());
        }

        return Collections.emptyList();
    }

    private DistributionTest getDistributionConfig(ExtensionContext context) {
        return context.getTestClass().get().getDeclaredAnnotation(DistributionTest.class);
    }

    private RawDistRootPath getDistPath(){
        return new RawDistRootPath(dist.unwrap(RawKeycloakDistribution.class).getDistPath());
    }
}
