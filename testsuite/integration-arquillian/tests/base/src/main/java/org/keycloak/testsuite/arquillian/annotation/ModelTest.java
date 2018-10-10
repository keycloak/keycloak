/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates
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

import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * The annotation on test methods. The annotated method MUST have single parameter - KeycloakSession
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@Documented
@Retention(RUNTIME)
@Target({ElementType.METHOD}) // TODO: Maybe ElementClass.TYPE too? That way it will be possible to add the annotation on the the test class and not need to add on all the test methods inside the class
public @interface ModelTest {
}
