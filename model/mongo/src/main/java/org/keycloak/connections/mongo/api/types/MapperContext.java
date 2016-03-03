/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.connections.mongo.api.types;

import java.lang.reflect.Type;
import java.util.List;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class MapperContext<T, S> {

    // object to convert
    private final T objectToConvert;

    // expected return type, which could be useful information in some mappers, so they are able to dynamically instantiate types
    private final Class<? extends S> expectedReturnType;

    // in case that expected return type is generic type (like "List<String>"), then genericTypes could contain list of expected generic arguments
    private final List<Type> genericTypes;

    public MapperContext(T objectToConvert, Class<? extends S> expectedReturnType, List<Type> genericTypes) {
        this.objectToConvert = objectToConvert;
        this.expectedReturnType = expectedReturnType;
        this.genericTypes = genericTypes;
    }

    public T getObjectToConvert() {
        return objectToConvert;
    }

    public Class<? extends S> getExpectedReturnType() {
        return expectedReturnType;
    }

    public List<Type> getGenericTypes() {
        return genericTypes;
    }
}
