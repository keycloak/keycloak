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

import static org.keycloak.quarkus.runtime.Environment.isRebuild;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.apache.commons.collections4.IteratorUtils;
import org.keycloak.config.OptionCategory;
import org.keycloak.quarkus.runtime.Environment;
import org.keycloak.quarkus.runtime.configuration.mappers.PropertyMapper;
import org.keycloak.quarkus.runtime.configuration.mappers.PropertyMappers;

import io.smallrye.config.ConfigSourceInterceptor;
import io.smallrye.config.ConfigSourceInterceptorContext;
import io.smallrye.config.ConfigValue;
import io.smallrye.config.Priorities;
import jakarta.annotation.Priority;

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

    public static void disable() {
        disable.set(true);
    }

    public static void enable() {
        disable.remove();
    }

    @Override
    public Iterator<String> iterateNames(ConfigSourceInterceptorContext context) {
        Iterable<String> iterable = () -> context.iterateNames();

        final Set<PropertyMapper<?>> allMappers = new HashSet<>(PropertyMappers.getMappers());

        //TODO: this is still not a complete list - things like quarkus.log.console.enabled
        // come from kc.log - but via a map from, not to.
        // so we'd need additional logic like the getWildcardMappedFrom case for that

        boolean filterRuntime = isRebuild() || Environment.isRebuildCheck();

        var baseStream = StreamSupport.stream(iterable.spliterator(), false).flatMap(name -> {
            PropertyMapper<?> mapper = PropertyMappers.getMapper(name);

            if (mapper == null) {
                return Stream.of(name);
            }
            if (filterRuntime && mapper.getCategory() == OptionCategory.CONFIG) {
                return Stream.of(); // advertising the keystore type causes the keystore to be used early
            }
            allMappers.remove(mapper);

            if (!mapper.hasWildcard()) {
                var wildCard = PropertyMappers.getWildcardMappedFrom(mapper.getOption());
                if (wildCard != null) {
                    ConfigValue value = context.proceed(name);
                    if (value != null && value.getValue() != null) {
                        return Stream.concat(Stream.of(name), wildCard.getToFromWildcardTransformer(value.getValue()));
                    }
                }
            }

            mapper = mapper.forKey(name);

            // there is a corner case here -1 for the reload period has no 'to' value.
            // if that becomes an issue we could use more metadata to perform a full mapping
            return toDistinctStream(name, mapper.getTo());
        });

        var defaultStream = allMappers.stream()
                .filter(m -> !m.getDefaultValue().isEmpty() && !m.hasWildcard()
                        && m.getCategory() != OptionCategory.CONFIG) // advertising the keystore type causes the keystore to be used early
                .flatMap(m -> toDistinctStream(m.getFrom(), m.getTo()));

        return IteratorUtils.chainedIterator(baseStream.iterator(), defaultStream.iterator());
    }

    private static Stream<String> toDistinctStream(String... values) {
        return Stream.of(values).filter(Objects::nonNull).distinct();
    }

    @Override
    public ConfigValue getValue(ConfigSourceInterceptorContext context, String name) {
        if (Boolean.TRUE.equals(disable.get())) {
            return context.proceed(name);
        }
        Function<String, ConfigValue> resolver = (n) -> PropertyMappers.getValue(context, n);
        return NestedPropertyMappingInterceptor.resolve(resolver, resolver, name);
    }
}
