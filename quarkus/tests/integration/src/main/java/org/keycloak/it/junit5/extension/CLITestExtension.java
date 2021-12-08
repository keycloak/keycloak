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

import static org.keycloak.it.junit5.extension.DistributionTest.ReInstall.BEFORE_ALL;
import static org.keycloak.it.junit5.extension.DistributionType.RAW;
import static org.keycloak.quarkus.runtime.Environment.forceTestLaunchMode;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.keycloak.it.utils.KeycloakDistribution;
import org.keycloak.quarkus.runtime.Environment;
import org.keycloak.quarkus.runtime.cli.command.Start;
import org.keycloak.quarkus.runtime.cli.command.StartDev;

import io.quarkus.test.junit.QuarkusMainTestExtension;
import io.quarkus.test.junit.main.Launch;
import io.quarkus.test.junit.main.LaunchResult;

public class CLITestExtension extends QuarkusMainTestExtension {

    private KeycloakDistribution dist;

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        DistributionTest distConfig = getDistributionConfig(context);

        if (distConfig != null) {
            Launch launch = context.getRequiredTestMethod().getAnnotation(Launch.class);

            if (launch != null) {
                if (dist == null) {
                    dist = createDistribution(distConfig);
                }
                dist.start(Arrays.asList(launch.value()));
            }
        } else {
            configureProfile(context);
            super.beforeEach(context);
        }
    }

    @Override
    public void afterEach(ExtensionContext context) throws Exception {
        DistributionTest distConfig = getDistributionConfig(context);

        if (distConfig != null) {
            if (distConfig.keepAlive()) {
                dist.stop();
            }
        }

        super.afterEach(context);
    }

    @Override
    public void afterAll(ExtensionContext context) throws Exception {
        if (dist != null) {
            // just to make sure the server is stopped after all tests
            dist.stop();
        }
        super.afterAll(context);
    }

    private KeycloakDistribution createDistribution(DistributionTest config) {
        return DistributionType.getCurrent().orElse(RAW).newInstance(config);
    }

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        DistributionTest distConfig = getDistributionConfig(context);

        if (distConfig != null) {
            if (BEFORE_ALL.equals(distConfig.reInstall())) {
                dist = createDistribution(distConfig);
            }
        } else {
            forceTestLaunchMode();
        }

        super.beforeAll(context);
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext context)
            throws ParameterResolutionException {
        Class<?> type = parameterContext.getParameter().getType();

        if (type == LaunchResult.class) {
            List<String> outputStream;
            List<String> errStream;
            int exitCode;

            boolean isDistribution = getDistributionConfig(context) != null;

            if (isDistribution) {
                outputStream = dist.getOutputStream();
                errStream = dist.getErrorStream();
                exitCode = dist.getExitCode();
            } else {
                LaunchResult result = (LaunchResult) super.resolveParameter(parameterContext, context);
                outputStream = result.getOutputStream();
                errStream = result.getErrorStream();
                exitCode = result.exitCode();
            }

            return CLIResult.create(outputStream, errStream, exitCode, isDistribution);
        }

        // for now, not support for manual launching using QuarkusMainLauncher
        throw new RuntimeException("Parameter type [" + type + "] not supported");
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
            throws ParameterResolutionException {
        Class<?> type = parameterContext.getParameter().getType();
        return type == LaunchResult.class;
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
}
