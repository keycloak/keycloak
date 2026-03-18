/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.quarkus.runtime.configuration;

import java.util.Iterator;

import jakarta.annotation.Priority;

import org.keycloak.quarkus.runtime.configuration.mappers.PropertyMappers;

import io.smallrye.config.ConfigSourceInterceptor;
import io.smallrye.config.ConfigSourceInterceptorContext;
import io.smallrye.config.ConfigValue;
import io.smallrye.config.Priorities;
import org.apache.commons.collections4.iterators.FilterIterator;

import static org.keycloak.quarkus.runtime.configuration.MicroProfileConfigProvider.NS_KEYCLOAK_PREFIX;

/**
 * <p>This interceptor is responsible for ignoring disabled Keycloak properties
 *
 * <p>This interceptor should execute before the {@link PropertyMappingInterceptor} so that disabled properties
 * are not mapped to the Quarkus properties.
 * <p>
 * The reason for the used priority is to always execute the interceptor before default Application Config Source interceptors
 * and before the {@link PropertyMappingInterceptor}
 */
@Priority(Priorities.APPLICATION - 20)
public class DisabledMappersInterceptor implements ConfigSourceInterceptor {

    private static final ThreadLocal<Boolean> ENABLED = ThreadLocal.withInitial(() -> false);

    public static void enable() {
        enable(true);
    }

    public static void disable() {
        enable(false);
    }

    public static void enable(boolean enable) {
        ENABLED.set(enable);
    }

    private <T> boolean isDisabledMapper(String property) {
        return property.startsWith(NS_KEYCLOAK_PREFIX) && PropertyMappers.isDisabledMapper(property);
    }

    Iterator<String> filterDisabledMappers(Iterator<String> iter) {
        return new FilterIterator<>(iter, item -> !isDisabledMapper(item));
    }

    @Override
    public Iterator<String> iterateNames(ConfigSourceInterceptorContext context) {
        return filterDisabledMappers(context.iterateNames());
    }

    @Override
    public ConfigValue getValue(ConfigSourceInterceptorContext context, String name) {
        if (isEnabled() && isDisabledMapper(name)) {
            return null;
        }
        return context.proceed(name);
    }

    public static boolean isEnabled() {
        return Boolean.TRUE.equals(ENABLED.get());
    }

    public static void runWithDisabled(Runnable execution) {
        if (!isEnabled()) {
            execution.run();
            return;
        }
        try {
            disable();
            execution.run();
        } finally {
            enable();
        }
    }
}
