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

import java.util.Iterator;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import jakarta.annotation.Priority;

import org.keycloak.config.OptionCategory;
import org.keycloak.quarkus.runtime.configuration.mappers.PropertyMapper;
import org.keycloak.quarkus.runtime.configuration.mappers.PropertyMappers;
import org.keycloak.quarkus.runtime.configuration.mappers.WildcardPropertyMapper;

import io.smallrye.config.ConfigSourceInterceptor;
import io.smallrye.config.ConfigSourceInterceptorContext;
import io.smallrye.config.ConfigValue;
import io.smallrye.config.Priorities;
import org.apache.commons.collections4.IteratorUtils;

import static org.keycloak.quarkus.runtime.Environment.isRebuild;
import static org.keycloak.quarkus.runtime.configuration.MicroProfileConfigProvider.NS_KEYCLOAK_PREFIX;

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
 *
 * <p>The {@link NestedPropertyMappingInterceptor} catches property mappings that need to be performed within expressions.
 *
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

    /**
     * Provides a curated iteration of names based upon the mapping logic.
     * Quarkus logic, such as config mapping, is dependent upon seeing the quarkus
     * form of the key. We want to expose that here, rather than in the config sources
     * because we lack a simple way to do name mapping for some sources, such as the
     * keystore config source.
     * <p>
     * We currently expose:
     * <li>anything based upon a property mapper that has a map to a quarkus property - including
     * our kc. properties that have defaults.
     * <li>wildcard key names for wildcard keys that map from a keycloak property (e.g. kc.log-level)
     *
     * We selectively exclude:
     * <li>Config keystore properties at build time
     */
    @Override
    public Iterator<String> iterateNames(ConfigSourceInterceptorContext context) {
        Iterable<String> iterable = context::iterateNames;

        final Set<PropertyMapper<?>> allMappers = PropertyMappers.getMappers();

        //TODO: this is still not a complete list - things like quarkus.log.console.enabled
        // come from kc.log - but via a map from, not to.
        // so we'd need additional logic like the getWildcardMappedFrom case for that

        boolean filterRuntime = isRebuild();

        var baseStream = StreamSupport.stream(iterable.spliterator(), false).flatMap(name -> {
            final PropertyMapper<?> mapper = PropertyMappers.getMapper(name);

            if (mapper == null) {
                return Stream.of(name);
            }
            if (filterRuntime && mapper.getCategory() == OptionCategory.CONFIG) {
                return Stream.of(); // advertising the keystore type causes the keystore to be used early
            }

            final PropertyMapper<?> mappedMapper = mapper.forKey(name);

            // only include additional mappings if we're on the from side of the mapping as the mapping may not be bi-directional
            if (!name.equals(mappedMapper.getFrom())) {
                return Stream.of(name);
            }

            allMappers.remove(mapper);

            if (mapper.hasWildcard()) {
                // non-wildcard connected options should be already advertised by the logic in iterateNames()
                if (mapper.hasConnectedOptions()) {
                    var wildcardMapper = (WildcardPropertyMapper<?>) mapper;
                    var wildcardValue = wildcardMapper.extractWildcardValue(name).orElseThrow();
                    var connectedTo = wildcardMapper.getConnectedOptions(wildcardValue).stream()
                            .map(option -> Optional.ofNullable(PropertyMappers.getMapper(NS_KEYCLOAK_PREFIX + option)).orElseThrow(() -> new IllegalArgumentException("Cannot find connected options")))
                            .map(m -> m.hasWildcard() ? ((WildcardPropertyMapper<?>) m).getTo(wildcardValue) : m.getTo());

                    return Stream.concat(toDistinctStream(name, wildcardMapper.getTo(wildcardValue)), connectedTo);
                }
            } else {
                // this is not a wildcard value, but may map to wildcards
                // the current example is something like log-level=wildcardCat1:level,wildcardCat2:level
                var wildCard = PropertyMappers.getWildcardMappedFrom(mapper.getOption());
                if (wildCard != null) {
                    ConfigValue value = context.proceed(name);
                    if (value != null && value.getValue() != null) {
                        return Stream.concat(Stream.of(name), wildCard.getToFromWildcardTransformer(value.getValue()));
                    }
                }
            }

            // there is a corner case here: -1 for the reload period has no 'to' value.
            // if that becomes an issue we could use more metadata to perform a full mapping
            return toDistinctStream(name, mappedMapper.getTo());
        });

        // include anything remaining that has a default value
        var defaultStream = allMappers.stream()
                .filter(m -> m.getDefaultValue().isPresent() && !m.hasWildcard()
                        && m.getCategory() != OptionCategory.CONFIG) // advertising the keystore type causes the keystore to be used early
                .flatMap(m -> toDistinctStream(m.getTo()));

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

        // Call through NestedPropertyMappingInterceptor to track what we are currently getting the value for
        return NestedPropertyMappingInterceptor.getValueFromPropertyMappers(context, name);
    }
}
