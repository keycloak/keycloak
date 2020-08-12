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
package org.keycloak.validation;

/**
 * {@link ConditionalValidation} allows to delegate to a given validation while customizing the checks for
 * enablement and context-specific support.
 */
public class ConditionalValidation implements Validation {

    private final Validation delegate;

    private final ValidationCondition condition;

    public ConditionalValidation(Validation delegate) {
        this(delegate, ValidationCondition.ALWAYS);
    }

    public ConditionalValidation(Validation delegate, ValidationCondition condition) {
        this.delegate = delegate;
        this.condition = condition;
    }

    @Override
    public boolean validate(ValidationKey key, Object value, NestedValidationContext context) {
        return this.delegate.validate(key, value, context);
    }

    public boolean isApplicable(ValidationKey key, Object value, ValidationContext context) {
        return this.condition.test(key, value, context);
    }

    public Validation getDelegate() {
        return delegate;
    }

    public ValidationCondition getCondition() {
        return condition;
    }
}
