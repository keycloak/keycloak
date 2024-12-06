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
import org.apache.commons.collections4.IteratorUtils;
import org.apache.commons.collections4.iterators.FilterIterator;
import org.keycloak.quarkus.runtime.Environment;
import org.keycloak.quarkus.runtime.configuration.mappers.PropertyMapper;
import org.keycloak.quarkus.runtime.configuration.mappers.PropertyMappers;
import org.keycloak.quarkus.runtime.configuration.mappers.WildcardPropertyMapper;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

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

    private static final ThreadLocal<Boolean> disable = new ThreadLocal<>();
    private static final ThreadLocal<Boolean> disableAdditionalNames = new ThreadLocal<>();

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
        // We need to iterate through names to get wildcard option names.
        // Additionally, wildcardValuesTransformer might also trigger iterateNames.
        // Hence we need to disable this to prevent infinite recursion.
        // But we don't want to disable the whole interceptor, as wildcardValuesTransformer
        // might still need mappers to work.
        List<String> mappedWildcardNames = List.of();
        if (!Boolean.TRUE.equals(disableAdditionalNames.get())) {
            disableAdditionalNames.set(true);
            try {
                mappedWildcardNames = PropertyMappers.getWildcardMappers().stream()
                        .map(WildcardPropertyMapper::getToWithWildcards)
                        .flatMap(Set::stream)
                        .toList();
            } finally {
                disableAdditionalNames.remove();
            }
        }

        // this could be optimized by filtering the wildcard names in the stream above
        return filterRuntime(IteratorUtils.chainedIterator(mappedWildcardNames.iterator(), context.iterateNames()));
    }

    @Override
    public ConfigValue getValue(ConfigSourceInterceptorContext context, String name) {
        if (Boolean.TRUE.equals(disable.get())) {
            return context.proceed(name);
        }
        return PropertyMappers.getValue(context, name);
    }
}
