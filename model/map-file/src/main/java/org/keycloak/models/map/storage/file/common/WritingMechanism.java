/*
 * Copyright 2023 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.models.map.storage.file.common;

/**
 * Class implementing this interface defines mechanism for writing basic structures: primitive types,
 * sequences and maps.
 */
public interface WritingMechanism {

    /**
     * Writes a value of a primitive type ({@code null}, boolean, number, String).
     * @param value
     * @return
     */
    WritingMechanism writeObject(Object value);

    /**
     * Writes a sequence, items of which are written using this mechanism in the {@code task}.
     * @param task
     * @return
     */
    WritingMechanism writeSequence(Runnable task);

    /**
     * Writes a mapping, items of which are written using this mechanism in the {@code task}.
     * @param task
     * @return
     */
    WritingMechanism writeMapping(Runnable task);

    /**
     * Writes a mapping key/value pair, items of which are written using this mechanism in the {@code task}.
     * @param valueTask
     * @return
     */
    WritingMechanism writePair(String key, Runnable valueTask);


}
