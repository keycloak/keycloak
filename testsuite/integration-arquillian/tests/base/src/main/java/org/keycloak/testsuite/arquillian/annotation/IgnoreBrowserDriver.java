/*
 * Copyright 2022 Red Hat, Inc. and/or its affiliates
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

import org.openqa.selenium.WebDriver;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Annotation for marking test method/class which should be ignored
 *
 * @author <a href="mailto:mabartos@redhat.com">Martin Bartos</a>
 */
@Documented
@Retention(RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
@Repeatable(IgnoreBrowserDrivers.class)
public @interface IgnoreBrowserDriver {

    /**
     * Define for which WebDriver the test method/class should be ignored
     */
    Class<? extends WebDriver> value();

    /**
     * Define whether the value should be negated
     *
     * Usable in cases when we want to execute test method/class with all WebDrivers except the one specified in value()
     */
    boolean negate() default false;

}