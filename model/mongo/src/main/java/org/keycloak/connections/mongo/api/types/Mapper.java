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

/**
 * SPI object to convert object from application type to database type and vice versa. Shouldn't be directly used by application.
 * Various mappers should be registered in MapperRegistry, which is main entry point to be used by application
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public interface Mapper<T, S> {

    /**
     * Convert object from one type to expected type
     *
     * @param mapperContext Encapsulates reference to converted object and other things, which might be helpful in conversion
     * @return converted object
     */
    S convertObject(MapperContext<T, S> mapperContext);

    Class<? extends T> getTypeOfObjectToConvert();

    Class<S> getExpectedReturnType();
}
