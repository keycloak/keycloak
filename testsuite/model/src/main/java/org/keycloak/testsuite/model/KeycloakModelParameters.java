/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.testsuite.model;

import org.keycloak.provider.ProviderFactory;
import org.keycloak.provider.Spi;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

/**
 *
 * @author hmlnarik
 */
public class KeycloakModelParameters {

    private final Set<Class<? extends Spi>> allowedSpis;
    private final Set<Class<? extends ProviderFactory>> allowedFactories;

    public KeycloakModelParameters(Set<Class<? extends Spi>> allowedSpis, Set<Class<? extends ProviderFactory>> allowedFactories) {
        this.allowedSpis = allowedSpis;
        this.allowedFactories = allowedFactories;
    }

    boolean isSpiAllowed(Spi s) {
        return allowedSpis.contains(s.getClass());
    }

    boolean isFactoryAllowed(ProviderFactory factory) {
        return allowedFactories.stream().anyMatch((c) -> c.isAssignableFrom(factory.getClass()));
    }

    /**
     * Returns stream of parameters of the given type, or an empty stream if no parameters of the given type are supplied
     * by this clazz.
     * @param <T>
     * @param clazz
     * @return
     */
    public <T> Stream<T> getParameters(Class<T> clazz) {
        return Stream.empty();
    }

    public void updateConfig(Config cf) {
    }

    public Statement classRule(Statement base, Description description) {
        return base;
    }

    public Statement instanceRule(Statement base, Description description) {
        return base;
    }

}
