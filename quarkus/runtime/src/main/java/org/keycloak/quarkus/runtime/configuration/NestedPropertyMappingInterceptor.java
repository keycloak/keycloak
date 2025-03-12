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

import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.function.Function;

import org.keycloak.quarkus.runtime.configuration.mappers.PropertyMappers;

import io.smallrye.config.ConfigSourceInterceptor;
import io.smallrye.config.ConfigSourceInterceptorContext;
import io.smallrye.config.ConfigValue;
import io.smallrye.config.Priorities;
import jakarta.annotation.Priority;

/**
 * Some resolution of values that come from PropertyMappers
 * happens at the ExpressionConfigSourceInterceptor, which is after
 * property mapping. This interceptor appears just after the expression
 * interceptor and will restart the context for anything not actively recursing.
 * This is needed in case the expression contains something that requires property mapping.
 */
@Priority(Priorities.LIBRARY + 299)
public class NestedPropertyMappingInterceptor implements ConfigSourceInterceptor {

    static final ThreadLocal<LinkedHashSet<String>> recursions = new ThreadLocal<>();

    @Override
    public ConfigValue getValue(ConfigSourceInterceptorContext context, String name) {
        return resolve(context::restart, context::proceed, name);
    }

    static <T> T resolve(Function<String, T> resolver, Function<String, T> nonRecursiveResolver, String name) {
        LinkedHashSet<String> recursing = recursions.get();
        if (recursing == null) {
            recursing = new LinkedHashSet<String>();
            recursions.set(recursing);
        }
        boolean added = recursing.add(name);
        if (added) {
            try {
                return resolver.apply(name);
            } finally {
                recursing.remove(name);
            }
        }
        return nonRecursiveResolver.apply(name);
    }

    public static Optional<String> getResolvingRoot() {
        return Optional.ofNullable(recursions.get()).map(s -> s.iterator().next());
    }

}
