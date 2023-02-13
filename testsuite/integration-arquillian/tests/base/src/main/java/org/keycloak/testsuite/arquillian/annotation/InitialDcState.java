/*
 * Copyright 201 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.testsuite.arquillian.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.keycloak.testsuite.crossdc.ServerSetup;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Specifies initial state of auth-server and cache-server nodes before start of each test
 * in multinode setup like in cross-DC tests.
 * When a test class is annotated, this annotation is applied to every test method in the class
 * but can be overridden on method level.
 * 
 * @author vramik
 * @author hmlnarik
 */
@Documented
@Retention(RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface InitialDcState {
    ServerSetup cacheServers() default ServerSetup.FIRST_NODE_IN_EVERY_DC;
    ServerSetup authServers() default ServerSetup.FIRST_NODE_IN_EVERY_DC;
}
