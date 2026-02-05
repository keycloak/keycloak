/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package org.keycloak.scim.resource.schema.attribute;

import java.util.function.BiConsumer;

import org.keycloak.common.util.TriConsumer;
import org.keycloak.models.Model;
import org.keycloak.scim.resource.ResourceTypeRepresentation;

/**
 * <p>An attribute mapper defines how to set an attribute to a {@link Model} and its corresponding {@link ResourceTypeRepresentation}.
 *
 * @see Attribute
 */
public class AttributeMapper<M extends Model, R extends ResourceTypeRepresentation> {

    private final TriConsumer<M, String, String> modelSetter;
    private final BiConsumer<R, String> representationSetter;

    public AttributeMapper(TriConsumer<M, String, String> modelSetter, BiConsumer<R, String> representationSetter) {
        this.modelSetter = modelSetter;
        this.representationSetter = representationSetter;
    }

    public AttributeMapper(BiConsumer<M, String> modelSetter, BiConsumer<R, String> representationSetter) {
        this((model, name, value) -> modelSetter.accept(model, value), representationSetter);
    }

    public void setValue(R representation, String value) {
        representationSetter.accept(representation, value);
    }

    public void setValue(M model, String name, String value) {
        modelSetter.accept(model, name, value);
    }
}
