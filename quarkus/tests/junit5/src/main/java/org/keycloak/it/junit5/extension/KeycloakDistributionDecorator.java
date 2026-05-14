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
import org.keycloak.quarkus.runtime.cli.command.DryRunMixin;

import io.restassured.RestAssured;

import static org.keycloak.quarkus.runtime.Environment.LAUNCH_MODE;
import static org.keycloak.quarkus.runtime.Environment.LAUNCH_MODE_EXIT_AFTER_START;
import static org.keycloak.quarkus.runtime.Environment.LAUNCH_MODE_EXIT_BEFORE_BOOTSTRAP;

public class KeycloakDistributionDecorator implements KeycloakDistribution {

    private DistributionTest config;
    private KeycloakDistribution delegate;
    private StopServer.Mode stopServer;
    private long startTimeout = TimeUnit.SECONDS.toMillis(Long.getLong("keycloak.distribution.start.timeout", 120L));

    public KeycloakDistributionDecorator(DistributionTest config,
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
            setEnvVar("KC_DEBUG", "true");
            setEnvVar("KC_DEBUG_SUSPEND", "y");
        }
        setEnvVar("KC_SHUTDOWN_DELAY", "0s");
        if (config.localCache()) {
            setEnvVar("KC_CACHE", "local");    
        } else {
            args.add("-Djgroups.join_timeout=50");
        }
        if (stopServer == Mode.BEFORE_QUARKUS) {
            setEnvVar(DryRunMixin.KC_DRY_RUN_ENV, "true");
            setEnvVar(DryRunMixin.KC_DRY_RUN_BUILD_ENV, "true");
        } else if (stopServer != Mode.MANUAL) {
            args.add("-D" + LAUNCH_MODE + "=" + (stopServer == Mode.BEFORE_BOOTSTRAP ? LAUNCH_MODE_EXIT_BEFORE_BOOTSTRAP : LAUNCH_MODE_EXIT_AFTER_START));            
        }

        if (config.enableTls()) {
            copyOrReplaceFileFromClasspath("/server.keystore", Path.of("conf", "server.keystore"));
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

        return CLIResult.create(getOutputStream(), getErrorStream(), getExitCode());
    }
    
    @Override
    public void runKc(List<String> arguments) {
        delegate.runKc(arguments);
    }
    
    @Override
    public int getMappedPort(int port) {
        return delegate.getMappedPort(port);
    }
    
    @Override
    public void waitFor(boolean ready, long timeoutMillis) {
        delegate.waitFor(ready, timeoutMillis);
    }
    
    @Override
    public boolean supportsDebug() {
        return delegate.supportsDebug();
    }

    @Override
    public void stop() {
        delegate.stop();
    }

    @Override
    public List<String> getOutputStream() {
        return delegate.getOutputStream();
    }

    @Override
    public List<String> getErrorStream() {
        return delegate.getErrorStream();
    }

    @Override
    public int getExitCode() {
        return delegate.getExitCode();
    }

    public void setStopServer(Mode mode) {
        this.stopServer = mode;
    }

    @Override
    public void setQuarkusProperty(String key, String value) {
        delegate.setQuarkusProperty(key, value);
    }

    @Override
    public void setProperty(String key, String value) {
        delegate.setProperty(key, value);
    }

    @Override
    public void deleteQuarkusProperties() {
        delegate.deleteQuarkusProperties();
    }

    @Override
    public void copyOrReplaceFileFromClasspath(String file, Path distDir) {
        delegate.copyOrReplaceFileFromClasspath(file, distDir);
    }

    @Override
    public void removeProperty(String name) {
        delegate.removeProperty(name);
    }

    @Override
    public void setEnvVar(String name, String value) {
        delegate.setEnvVar(name, value);
    }

    @Override
    public void copyOrReplaceFile(Path file, Path targetFile) {
        delegate.copyOrReplaceFile(file, targetFile);
    }

    public void setRequestPort(int port) {
        RestAssured.port = delegate.getMappedPort(port);
    }

    /**
     * Get the underlying distribution - NOTE directly calling {@link KeycloakDistribution#runKc(List)} 
     * on the unwrapped distribution is not recommended. The {@link #run(List)} methods on class should be used
     * instead as they ensure the arguments and env match the expectations of the test and method annotations
     */
    public  <D extends KeycloakDistribution> D unwrap(Class<D> type) {
        if (!KeycloakDistribution.class.isAssignableFrom(type)) {
            throw new IllegalArgumentException("Not a " + KeycloakDistribution.class + " type");
        }

        if (type.isInstance(delegate)) {
            //noinspection unchecked
            return (D) delegate;
        }

        throw new IllegalArgumentException("Not a " + type + " type");
    }

    @Override
    public void clearEnv() {
        delegate.clearEnv();
    }

}
