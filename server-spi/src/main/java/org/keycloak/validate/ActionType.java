/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.validate;

/**
 * Helps to differentiate a Validation variant. An example us the validation during User creation or update.
 * <p>
 * This is an interface instead of an {@code enum}, to allow users to provide their own {@link ActionType} implementations.
 */
public interface ActionType {

    /**
     * The name of the action.
     *
     * @return
     */
    String name();

    /**
     * Common {@link ActionType ActionType's}.
     */
    enum Common implements ActionType {
        CREATE,
        UPDATE,
    }
}