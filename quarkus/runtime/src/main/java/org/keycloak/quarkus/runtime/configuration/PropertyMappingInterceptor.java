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
package org.keycloak.quarkus.runtime.configuration;

import io.smallrye.config.ConfigSourceInterceptor;
import io.smallrye.config.ConfigSourceInterceptorContext;
import io.smallrye.config.ConfigValue;

import io.smallrye.config.Priorities;
import jakarta.annotation.Priority;
import org.apache.commons.collections4.iterators.FilterIterator;
import org.keycloak.common.util.StringPropertyReplacer;
import org.keycloak.quarkus.runtime.Environment;
import org.keycloak.quarkus.runtime.configuration.mappers.PropertyMapper;
import org.keycloak.quarkus.runtime.configuration.mappers.PropertyMappers;

import java.util.Iterator;

import static org.keycloak.quarkus.runtime.Environment.isRebuild;

/**
 * <p>This interceptor is responsible for mapping Keycloak properties to their corresponding properties in Quarkus.
 *
 * <p>A single property in Keycloak may span a single or multiple properties on Quarkus and for each property we want to map
 * from Quarkus we should configure a {@link PropertyMapper}.
 *
 * <p>The {@link PropertyMapper} can either perform a 1:1 mapping where the value of a property from
 * Keycloak (e.g.: https.port) is mapped to a single properties in Quarkus, or perform a 1:N mapping where the value of a property
 * from Keycloak (e.g.: database) is mapped to multiple properties in Quarkus.
 *
 * <p>This interceptor must execute after the {@link io.smallrye.config.ExpressionConfigSourceInterceptor} so that expressions
 * are properly resolved before executing this interceptor.
 * <p>
 * The reason for the used priority is to always execute the interceptor before default Application Config Source interceptors
 */
@Priority(Priorities.APPLICATION - 10)
public class PropertyMappingInterceptor implements ConfigSourceInterceptor {

    private static ThreadLocal<Boolean> disable = new ThreadLocal<>();

    public static void disable() {
        disable.set(true);
    }

    public static void enable() {
        disable.remove();
    }

    static Iterator<String> filterRuntime(Iterator<String> iter) {
        if (!isRebuild() && !Environment.isRebuildCheck()) {
            return iter;
        }
        return new FilterIterator<>(iter, item -> !isRuntime(item));
    }

    static boolean isRuntime(String name) {
        PropertyMapper<?> mapper = PropertyMappers.getMapper(name);
        return mapper != null && mapper.isRunTime();
    }

    @Override
    public Iterator<String> iterateNames(ConfigSourceInterceptorContext context) {
        return filterRuntime(context.iterateNames());
    }

    @Override
    public ConfigValue getValue(ConfigSourceInterceptorContext context, String name) {
        if (Boolean.TRUE.equals(disable.get())) {
            return context.proceed(name);
        }
        ConfigValue value = PropertyMappers.getValue(context, name);

        if (value == null || value.getValue() == null) {
            return null;
        }

        if (!value.getValue().contains("${")) {
            return value;
        }

        // Our mappers might have returned a value containing an expression ${...}.
        // However, ExpressionConfigSourceInterceptor was already executed before (to expand e.g. env vars in config file).
        // Hence, we need to manually resolve these expressions here. Not ideal, but there's no other way (at least I haven't found one).
        return value.withValue(
                StringPropertyReplacer.replaceProperties(value.getValue(),
                        property -> {
                            ConfigValue prop = context.proceed(property);

                            if (prop == null) {
                                return null;
                            }

                            return prop.getValue();
                        }));
    }
}
