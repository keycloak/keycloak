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
package org.keycloak.models;

import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Thrown when data can't be retrieved for the model.
 *
 * This occurs when an entity has been removed or updated in the meantime. This might wrap an optimistic lock exception
 * depending on the store.
 *
 * Callers might use this exception to filter out entities that are in an illegal state, see
 * <code>org.keycloak.models.utils.ModelToRepresentation#toRepresentation(Stream, Function)</code>
 *
 * @author <a href="mailto:aschwart@redhat.com">Alexander Schwartz</a>
 */
public class ModelIllegalStateException extends ModelException {

    public ModelIllegalStateException() {
    }

    public ModelIllegalStateException(String message) {
        super(message);
    }

    public ModelIllegalStateException(String message, Throwable cause) {
        super(message, cause);
    }

    public ModelIllegalStateException(Throwable cause) {
        super(cause);
    }
}
