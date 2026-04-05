/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.models;

/**
 * <p>Thrown to indicate that an error is expected as a result of the validations run against a model. Such
 * exceptions are not considered internal server errors but an expected error when validating a model where the client
 * has the opportunity to fix and retry the request.
 *
 * <p>Some validations can only happen during the commit phase and should not be handled as an unknown error by the default exception
 * handling mechanism.
 */
public class ModelValidationException extends ModelException {

    public ModelValidationException(String message) {
        super(message);
    }
}
