/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.representations.admin.v2.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE_USE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Validation constraint for URLs that may contain parameterized placeholders.
 * <p>
 * This validator supports URLs with curly brace placeholders like {@code http://{host}/callback}.
 * The placeholders are URL-encoded before validation, allowing parameterized URLs to pass validation.
 * <p>
 * Curly brackets must be balanced (each '{' must have a matching '}' in correct order).
 *
 * @see ParameterizedURLValidator
 */
@Target({FIELD, METHOD, PARAMETER, ANNOTATION_TYPE, CONSTRUCTOR, TYPE_USE})
@Retention(RUNTIME)
@Documented
@Constraint(validatedBy = ParameterizedURLValidator.class)
public @interface ParameterizedURL {

    String message() default "must be a valid URL";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
