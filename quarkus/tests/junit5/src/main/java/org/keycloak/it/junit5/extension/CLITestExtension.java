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

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.keycloak.Keycloak;
import org.keycloak.it.utils.KeycloakDistribution;
import org.keycloak.it.utils.RawDistRootPath;
import org.keycloak.it.utils.RawKeycloakDistribution;
import org.keycloak.quarkus.runtime.Environment;
import org.keycloak.quarkus.runtime.cli.command.Start;
import org.keycloak.quarkus.runtime.cli.command.StartDev;

import io.quarkus.runtime.configuration.QuarkusConfigFactory;
import io.quarkus.test.common.TestResourceManager;
import io.quarkus.test.junit.QuarkusMainTestExtension;
import io.quarkus.test.junit.main.Launch;
import io.quarkus.test.junit.main.LaunchResult;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ReflectiveInvocationContext;

import static java.lang.System.setProperty;

import static org.keycloak.it.junit5.extension.DistributionType.RAW;
import static org.keycloak.quarkus.runtime.Environment.forceExitAfterStartLaunchMode;

public class CLITestExtension extends QuarkusMainTestExtension {

    private KeycloakRunner runner;
    private DatabaseContainer databaseContainer;
    private InfinispanContainer infinispanContainer;
    private CLIResult result;
    private boolean beforeAll;
    
    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        DistributionTest distConfig = getDistributionConfig(context);
        Launch launch = context.getRequiredTestMethod().getAnnotation(Launch.class);

        if (launch != null && distConfig == null) {
            Stream.of(launch.value()).forEach(arg -> {
                if (arg.startsWith("-D")) {
                    int index = arg.indexOf("=");
                    if (index > 0) {
                        setProperty(arg.substring(2, index), arg.substring(index + 1, arg.length()));
                    } else {
                        setProperty(arg.substring(2), "");
                    }
                }
            });
        }
        
        if (isRaw() && distConfig != null && runner != null) {
            try {
                runner.getDistribution(RawKeycloakDistribution.class).reset(beforeAll);
                beforeAll = false;
            } catch (Exception cause) {
                throw new RuntimeException("Failed to partially reset", cause);
            }
        }

        configureDatabase(context);
        infinispanContainer = configureExternalInfinispan(context);

        if (distConfig != null) {
            if (runner == null) {
                runner = createDistribution(distConfig);
            }

            copyTestProvider(context.getRequiredTestClass().getAnnotation(TestProvider.class));
            copyTestProvider(context.getRequiredTestMethod().getAnnotation(TestProvider.class));
            onBeforeStartDistribution(context.getRequiredTestClass().getAnnotation(BeforeStartDistribution.class));
            onBeforeStartDistribution(context.getRequiredTestMethod().getAnnotation(BeforeStartDistribution.class));

            configureEnvVars(context.getRequiredTestClass().getAnnotation(WithEnvVars.class));
            configureEnvVars(context.getRequiredTestMethod().getAnnotation(WithEnvVars.class));
            
            var stopServer = Optional.ofNullable(context.getRequiredTestMethod().getAnnotation(StopServer.class)).map(StopServer::value).orElse(distConfig.stopServer());
            
            runner.setStopServer(stopServer);

            if (launch != null) {
                result = runner.run(List.of(launch.value()));
            }
        } else {
            if (!Keycloak.initSys(launch == null ? new String[] {} : launch.value())) {
                return;
            }
            configureProfile(context);
            super.beforeEach(context);
        }
    }

    private void copyTestProvider(TestProvider provider) {
        if (provider == null) {
            return;
        }

        if (isRaw()) {
            try {
                runner.getDistribution(RawKeycloakDistribution.class).copyProvider(provider.value().getDeclaredConstructor().newInstance());
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
        if (runner == null) {
            super.interceptTestMethod(invocation, invocationContext, extensionContext);
        } else {
            invocation.proceed();
        }
    }

    private void onBeforeStartDistribution(BeforeStartDistribution annotation) {
        if (annotation != null) {
            try {
                annotation.value().getDeclaredConstructor().newInstance().accept(runner.getDistribution(RawKeycloakDistribution.class));
            } catch (Exception cause) {
                throw new RuntimeException("Error when invoking " + annotation.value() + " instance before starting distribution", cause);
            }
        }
    }

    @Override
    public void afterEach(ExtensionContext context) throws Exception {
        DistributionTest distConfig = getDistributionConfig(context);

        if (runner != null) {
            runner.stop();
            runner.getDistribution().clearEnv();
        }

        super.afterEach(context);
        reset(distConfig, context);
    }

    private void reset(DistributionTest distConfig, ExtensionContext context) {
        QuarkusConfigFactory.setConfig(null);
        if (databaseContainer != null && databaseContainer.isRunning()) {
            databaseContainer.stop();
            databaseContainer = null;
        }
        if (infinispanContainer != null) {
            infinispanContainer.stop();
        }
        result = null;
    }

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        // taken from QuarkusUnitTest - QuarkusMainTestExtension does not do resource management
        ExtensionContext.Store store = context.getRoot().getStore(ExtensionContext.Namespace.GLOBAL);
        TestResourceManager testResourceManager = (TestResourceManager) store.get(TestResourceManager.class.getName());
        if (testResourceManager == null) {
            testResourceManager = new TestResourceManager(context.getRequiredTestClass());
            testResourceManager.init(null);
            testResourceManager.start();
            TestResourceManager tm = testResourceManager;
            store.put(TestResourceManager.class.getName(), testResourceManager);
            store.put(TestResourceManager.CLOSEABLE_NAME, new AutoCloseable() {

                @Override
                public void close() throws Exception {
                    tm.close();
                }
            });
        }
        
        beforeAll = true;
        
        DistributionTest distConfig = getDistributionConfig(context);

        if (distConfig != null) {
            runner = createDistribution(distConfig);
        } else {
            forceExitAfterStartLaunchMode();
        }

        super.beforeAll(context);
    }

    @Override
    public void afterAll(ExtensionContext context) throws Exception {
        if (runner != null) {
            // just to make sure the server is stopped after all tests
            runner.stop();
        }
        super.afterAll(context);
    }

    private KeycloakRunner createDistribution(DistributionTest config) {
        return new KeycloakRunner(config, DistributionType.getCurrent().orElse(RAW).newInstance(config));
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
        
        if (type.equals(KeycloakRunner.class)) {
            return this.runner;
        }

        if (KeycloakDistribution.class.isAssignableFrom(type)) {
            if (context.getTestClass().orElse(Object.class).getDeclaredAnnotation(DistributionTest.class) == null) {
                throw new RuntimeException("Only tests annotated with " + DistributionTest.class + " can inject a distribution instance");
            }
            return runner.getDistribution((Class<? extends KeycloakDistribution>) type);
        }

        // for now, no support for manual launching using QuarkusMainLauncher
        throw new RuntimeException("Parameter type [" + type + "] not supported");
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
            throws ParameterResolutionException {
        Class<?> type = parameterContext.getParameter().getType();
        return type == LaunchResult.class || type == CLIResult.class || type == RawDistRootPath.class || type == KeycloakDistribution.class || type == KeycloakRunner.class;
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
            if (runner == null) {
                configureDevServices();
                setProperty("kc.db", database.alias());
                setProperty("kc.db-password", DatabaseContainer.DEFAULT_PASSWORD);
            } else {
                databaseContainer = new DatabaseContainer(database.alias());

                databaseContainer.start();

                RawKeycloakDistribution rawDist = runner.getDistribution(RawKeycloakDistribution.class);
                if (database.buildOptions().length == 0) {
                    rawDist.setProperty("db", database.alias());
                } else {
                    for (String option : database.buildOptions()) {
                        rawDist.setProperty(option.substring(0, option.indexOf('=')), option.substring(option.indexOf('=') + 1));
                    }
                }

                databaseContainer.configureDistribution(rawDist);

                runner.run("build");
            }
        } else if (runner == null) {
            // This is for re-creating the H2 database instead of using the default in home
            setProperty("kc.db-url-path", Keycloak.initTempDirectory("h2-home").toFile().getAbsolutePath());
        }
    }

    private static InfinispanContainer configureExternalInfinispan(ExtensionContext context) {
        if (getAnnotationFromTestContext(context, WithExternalInfinispan.class) != null) {
            InfinispanContainer infinispanContainer = new InfinispanContainer();
            try {
                infinispanContainer.start();
            }  catch (RuntimeException e) {
                infinispanContainer.stop();
                throw e;
            }
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
            runner.getDistribution().setEnvVar(envVars.value()[i], envVars.value()[i+1]);
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
        return new RawDistRootPath(runner.getDistribution(RawKeycloakDistribution.class).getDistPath());
    }
}
