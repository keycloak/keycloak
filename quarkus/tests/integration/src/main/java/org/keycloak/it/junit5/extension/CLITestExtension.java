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
import static org.keycloak.quarkus.runtime.Environment.forceTestLaunchMode;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.keycloak.it.junit5.extension.DistributionTest.ReInstall;
import org.keycloak.it.utils.KeycloakDistribution;
import org.keycloak.quarkus.runtime.Environment;
import org.keycloak.quarkus.runtime.cli.command.Start;
import org.keycloak.quarkus.runtime.cli.command.StartDev;

import io.quarkus.test.junit.QuarkusMainTestExtension;
import io.quarkus.test.junit.main.Launch;
import io.quarkus.test.junit.main.LaunchResult;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.OutputFrame;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.ImageFromDockerfile;

public class CLITestExtension extends QuarkusMainTestExtension {

    private GenericContainer keycloakContainer = new GenericContainer(
            new ImageFromDockerfile()
                    .withFileFromFile("keycloak.x-16.0.0-SNAPSHOT.tar.gz", new File("../../../distribution/server-x-dist/target/keycloak.x-16.0.0-SNAPSHOT.tar.gz"))
                    .withFileFromFile("Dockerfile", new File("./Dockerfile"))
                    .withBuildArg("KEYCLOAK_DIST", "keycloak.x-16.0.0-SNAPSHOT.tar.gz")
    ).withExposedPorts(8080).waitingFor(Wait.forHttp("/").forStatusCode(200));

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {

        List<String> additionalProperties = List.of(
                "-Dhttp.enabled=true",
                "-Dcluster=local",
                "-Dhostname.strict=false",
                "-Dhostname.strict-https=false"
        );

        List<String> cmd = getCliArgs(context);
        Stream<String> fullCmd = Stream.concat(cmd.stream(), additionalProperties.stream());

        keycloakContainer.withCommand(fullCmd.collect(Collectors.toList()).toArray(new String[0])).start();

        super.beforeEach(context);
    }

    @Override
    public void afterEach(ExtensionContext context) throws Exception {
        if (keycloakContainer != null) {
            keycloakContainer.stop();
        }

        super.afterEach(context);
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext context)
            throws ParameterResolutionException {
        Class<?> type = parameterContext.getParameter().getType();

        if (type == LaunchResult.class) {
            List<String> outputStream;
            List<String> errStream;
            int exitCode;

            boolean isDistribution = keycloakContainer != null;

            if (isDistribution) {
                keycloakContainer.getLogs(OutputFrame.OutputType.END);
                outputStream = List.of(keycloakContainer.getLogs(OutputFrame.OutputType.STDOUT));
                errStream = List.of(keycloakContainer.getLogs(OutputFrame.OutputType.STDERR));
                exitCode = keycloakContainer.isRunning() ? 0 : 1;
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

    private List<String> getCliArgs(ExtensionContext context) {
        Launch annotation = context.getRequiredTestMethod().getAnnotation(Launch.class);

        if (annotation != null) {
            return Arrays.asList(annotation.value());
        }

        return Collections.emptyList();
    }

}
