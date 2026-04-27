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

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.keycloak.provider.Provider;

/**
 * Identifies a requirement for a given provider to be present in the session factory.
 * If the provider is not available, the test is skipped.
 *
 * @author hmlnarik
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
@Repeatable(RequireProviders.class)
public @interface RequireProvider {
    Class<? extends Provider> value() default Provider.class;

    /**
     * Specifies provider IDs of mandatory provider. There must be at least one provider available
     * from those in {@code only} array to fulfil this requirement. If this is used together with
     * {@link #exclude()} both rules are applied.
     * <p />
     * For example,
     * When possible providers are: {@code provider1}, {@code provider2}, {@code provider3}
     * and rules: {@code @RequireProvider{value = MyFactory.class, only = [provider1, provider2], exclude = [provider2]}}
     * The test will be running only when {@code provider1} is available on the session factory
     *
     */
    String[] only() default {};

    /**
     * Specifies provider IDs that does not satisfy this requirement. In other words, there must be another provider
     * of type {@code value()} for satisfying this requirement. If this is used together with
     * {@link #only()} both rules are applied.
     * <p />
     * For example,
     * When possible providers are: {@code provider1}, {@code provider2}, {@code provider3}
     * and rules: {@code @RequireProvider{value = MyFactory.class, only = [provider1, provider2], exclude = [provider2]}}
     * The test will be running only when {@code provider1} is available on the session factory
     */
    String[] exclude() default {};

}
