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

import org.keycloak.it.utils.KeycloakDistribution;

public class KeycloakDistributionDecorator implements KeycloakDistribution {

    private Storage storageConfig;
    private WithDatabase databaseConfig;
    private DistributionTest config;
    private KeycloakDistribution delegate;

    public KeycloakDistributionDecorator(Storage storageConfig, WithDatabase databaseConfig, DistributionTest config,
                                         KeycloakDistribution delegate) {
        this.storageConfig = storageConfig;
        this.databaseConfig = databaseConfig;
        this.config = config;
        this.delegate = delegate;
    }

    @Override
    public CLIResult run(List<String> rawArgs) {
        List<String> args = new ArrayList<>(rawArgs);

        args.addAll(List.of(config.defaultOptions()));

        return delegate.run(new ServerOptions(storageConfig, databaseConfig, args));
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

    @Override
    public boolean isDebug() {
        return delegate.isDebug();
    }

    @Override
    public boolean isManualStop() {
        return delegate.isManualStop();
    }

    @Override
    public String[] getCliArgs(List<String> arguments) {
        return delegate.getCliArgs(arguments);
    }

    @Override
    public void setManualStop(boolean manualStop) {
        delegate.setManualStop(manualStop);
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

    @Override
    public void setRequestPort() {
        delegate.setRequestPort();
    }

    @Override
    public void setRequestPort(int port) {
        delegate.setRequestPort(port);
    }

    @Override
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
