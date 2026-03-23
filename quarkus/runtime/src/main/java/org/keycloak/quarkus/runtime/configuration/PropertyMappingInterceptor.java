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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import jakarta.annotation.Priority;

import org.keycloak.config.OptionCategory;
import org.keycloak.quarkus.runtime.Environment;
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

        final Set<PropertyMapper<?>> mappersWithoutValues = PropertyMappers.getMappers();

        boolean filterRuntime = isRebuild() || Boolean.getBoolean(Environment.KC_TEST_REBUILD);

        // this will return different iterations when our configuration is initialized vs not.
        // when the configuration is not initialized, we only need to worry about providing
        // bootsrapping level options, so we can make simplistic assumptions about the presence of values

        // we also only do first level discovery - for example if an inferred value exists, we are not
        // checking to see if it has wildcard or connected options

        var baseStream = StreamSupport.stream(iterable.spliterator(), false).flatMap(name -> {
            final PropertyMapper<?> mapper = PropertyMappers.getMapper(name);

            if (mapper == null) {
                return Stream.of(name);
            }
            if (filterRuntime && mapper.getCategory() == OptionCategory.CONFIG) {
                return Stream.of(); // advertising the keystore type causes the keystore to be used early
            }

            final PropertyMapper<?> mappedMapper = mapper.forKey(name);

            mappersWithoutValues.remove(mapper);

            // only include additional mappings if we're on the from side of the mapping as the mapping may not be bi-directional
            if (!name.equals(mappedMapper.getFrom())) {
                return Stream.of(name);
            }

            List<String> allNames = new ArrayList<String>();
            allNames.add(name);
            if (!name.equals(mappedMapper.getTo()) && hasValue(mappedMapper.getTo(), context)) {
                allNames.add(mappedMapper.getTo());
            }

            appendWildcardsMappedFrom(context, name, mapper, allNames);

            if (mapper.hasWildcard()) {
                var wildcardMapper = (WildcardPropertyMapper<?>) mapper;

                var wildcardValue = wildcardMapper.extractWildcardValue(name).orElseThrow();

                if (mapper.hasConnectedOptions()) {
                    wildcardMapper.getConnectedOptions(wildcardValue).stream()
                            .map(option -> Optional.ofNullable(PropertyMappers.getMapper(NS_KEYCLOAK_PREFIX + option)).orElseThrow(() -> new IllegalArgumentException("Cannot find connected options")))
                            .map(m -> m.hasWildcard() ? ((WildcardPropertyMapper<?>) m).getTo(wildcardValue) : m.getTo())
                            .filter(key -> hasValue(key, context)).forEach(allNames::add);
                }
            }

            return allNames.stream();
        });

        // include anything remaining that has a value - we currently only care about the to (typically quarkus) values
        var inferredValueStream = mappersWithoutValues.stream()
                .filter(m -> hasInferredValue(m, context))
                .map(m -> m.getTo());

        return IteratorUtils.chainedIterator(baseStream.iterator(), inferredValueStream.iterator());
    }

    private void appendWildcardsMappedFrom(ConfigSourceInterceptorContext context, String name,
            final PropertyMapper<?> mapper, List<String> names) {
        var wildCards = PropertyMappers.getWildcardsMappedFrom(mapper.getOption());
        if (wildCards.isEmpty()) {
            return;
        }
        ConfigValue value = context.proceed(name);
        if (value == null || value.getValue() == null) {
            return;
        }
        if (mapper.hasWildcard()) {
            var wildcardMapper = (WildcardPropertyMapper<?>) mapper;
            var wildcardValue = wildcardMapper.extractWildcardValue(name).orElseThrow();
            wildCards.stream().map(w -> w.getTo(wildcardValue)).filter(to -> hasValue(to, context)).forEach(names::add);
        } else {
            // this is not a wildcard value, but may map to wildcards
            // the current example is something like log-level=wildcardCat1:level,wildcardCat2:level
            wildCards.stream().flatMap(w -> w.getToFromWildcardTransformer(value.getValue())).forEach(names::add);
        }
    }

    private boolean hasInferredValue(PropertyMapper<?> m, ConfigSourceInterceptorContext context) {
        if (m.getCategory() == OptionCategory.CONFIG // advertising the keystore type causes the keystore to be used early
                || m.hasWildcard()
                || (m.getDefaultValue().isEmpty() && m.getMapFrom() == null)
                || m.getTo().startsWith(NS_KEYCLOAK_PREFIX)) {
            return false;
        }

        if (m.getMapper() == null && m.getDefaultValue().isPresent() && m.getMapFrom() == null) {
            return true; // the default will be used "as-is"
        }

        if (Configuration.isInitialized()) {
            return hasValue(m.getTo(), context);
        }

        return m.getDefaultValue().isPresent(); // just make a simplistic assumption prior to init
    }

    private boolean hasValue(String key, ConfigSourceInterceptorContext context) {
        try {
            return !Configuration.isInitialized()
                    || key.startsWith(NS_KEYCLOAK_PREFIX) // once we remove Scope.getPropertyNames, this check can be inverted like in hasInferredValue
                    || Optional.ofNullable(context.restart(key)).map(ConfigValue::getValue).isPresent();
        } catch (Exception e) {
            return false; // corner case - validation or other failure, we won't report it as having a value
        }
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
