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

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import javax.annotation.Priority;

import io.smallrye.config.ConfigSourceInterceptor;
import io.smallrye.config.ConfigSourceInterceptorContext;
import io.smallrye.config.ConfigValue;
import io.smallrye.config.Priorities;

import org.keycloak.quarkus.runtime.configuration.mappers.PropertyMapper;
import org.keycloak.quarkus.runtime.configuration.mappers.PropertyMappers;

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
 * <p>This interceptor should execute between the {@link io.smallrye.config.ExpressionConfigSourceInterceptor} and the {@link io.smallrye.config.ProfileConfigSourceInterceptor}
 * so that expressions are properly resolved after executing this interceptor while still able to resolve properties based on the current
 * profile.
 */
@Priority(Priorities.LIBRARY + 201)
public class PropertyMappingInterceptor implements ConfigSourceInterceptor {

    @Override
    public ConfigValue getValue(ConfigSourceInterceptorContext context, String name) {
        return PropertyMappers.getValue(context, name);
    }
}
