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

package org.keycloak.it.junit5.extension;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.keycloak.it.junit5.extension.StopServer.Mode;
import org.keycloak.it.utils.KeycloakDistribution;
import org.keycloak.it.utils.RawKeycloakDistribution;
import org.keycloak.quarkus.runtime.cli.command.DryRunMixin;

import io.restassured.RestAssured;

import static org.keycloak.quarkus.runtime.Environment.LAUNCH_MODE;
import static org.keycloak.quarkus.runtime.Environment.LAUNCH_MODE_EXIT_AFTER_START;
import static org.keycloak.quarkus.runtime.Environment.LAUNCH_MODE_EXIT_BEFORE_BOOTSTRAP;

/**
 * Wraps the distribution to provide a run methods that configure the distribution
 * to match the expectations of the test related annotations.
 */
public class KeycloakRunner {

    private DistributionTest config;
    private KeycloakDistribution delegate;
    private StopServer.Mode stopServer;
    private long startTimeout = TimeUnit.SECONDS.toMillis(Long.getLong("keycloak.distribution.start.timeout", 120L));

    public KeycloakRunner(DistributionTest config,
                                         KeycloakDistribution delegate) {
        this.config = config;
        this.delegate = delegate;
    }
    
    public CLIResult run(String ... rawArgs) {
        return run(List.of(rawArgs));
    }

    public CLIResult run(List<String> rawArgs) {
        delegate.stop();

        List<String> args = new ArrayList<>(rawArgs);
        args.addAll(List.of(config.defaultOptions()));
        if (config.debug() && delegate.supportsDebug()) {
            delegate.setEnvVar("KC_DEBUG", "true");
            delegate.setEnvVar("KC_DEBUG_SUSPEND", "y");
        }
        delegate.setEnvVar("KC_SHUTDOWN_DELAY", "0s");
        if (config.localCache()) {
            delegate.setEnvVar("KC_CACHE", "local");    
        } else {
            args.add("-Djgroups.join_timeout=50");
        }
        if (stopServer == Mode.BEFORE_QUARKUS) {
            delegate.setEnvVar(DryRunMixin.KC_DRY_RUN_ENV, "true");
            delegate.setEnvVar(DryRunMixin.KC_DRY_RUN_BUILD_ENV, "true");
        } else if (stopServer != Mode.MANUAL) {
            args.add("-D" + LAUNCH_MODE + "=" + (stopServer == Mode.BEFORE_BOOTSTRAP ? LAUNCH_MODE_EXIT_BEFORE_BOOTSTRAP : LAUNCH_MODE_EXIT_AFTER_START));            
        }

        if (config.enableTls()) {
            getDistribution(RawKeycloakDistribution.class).copyOrReplaceFileFromClasspath("/server.keystore", Path.of("conf", "server.keystore"));
        }
        try {
            delegate.runKc(args);
            delegate.waitFor(stopServer == Mode.MANUAL, startTimeout);
        } finally {
            if (stopServer != Mode.MANUAL) {
                delegate.stop();
            }
        }            
            
        setRequestPort(config.requestPort());

        return CLIResult.create(delegate.getOutputStream(), delegate.getErrorStream(), delegate.getExitCode());
    }
    
    public void stop() {
        delegate.stop();
    }

    public void setStopServer(Mode mode) {
        this.stopServer = mode;
    }

    public void setRequestPort(int port) {
        RestAssured.port = delegate.getMappedPort(port);
    }

    /**
     * Get the underlying distribution - NOTE directly calling {@link KeycloakDistribution#runKc(List)} 
     * on the unwrapped distribution is not recommended. The {@link #run(List)} methods on class should be used
     * instead as they ensure the arguments and env match the expectations of the test and method annotations
     */
    public <D extends KeycloakDistribution> D getDistribution(Class<D> type) {
        if (!KeycloakDistribution.class.isAssignableFrom(type)) {
            throw new IllegalArgumentException("Not a " + KeycloakDistribution.class + " type");
        }

        if (type.isInstance(delegate)) {
            //noinspection unchecked
            return (D) delegate;
        }

        throw new IllegalArgumentException("Not a " + type + " type");
    }
    
    public KeycloakDistribution getDistribution() {
        return this.delegate;
    }

    public void setEnvVar(String key, String value) {
        this.delegate.setEnvVar(key, value);
    }

}
