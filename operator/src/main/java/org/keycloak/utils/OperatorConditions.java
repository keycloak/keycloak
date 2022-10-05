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

package org.keycloak.utils;

import java.util.NoSuchElementException;
import java.util.function.Supplier;

/**
 * Null-safe condition checks
 *
 * @author <a href="mailto:mabartos@redhat.com">Martin Bartos</a>
 */
public class OperatorConditions {

    /**
     * Check whether the supplied object is NULL
     *
     * @param object supplied object
     */
    public static boolean isNull(Supplier<Object> object) {
        try {
            return object.get() == null;
        } catch (NullPointerException | NoSuchElementException ignore) {
            return true;
        }
    }

    /**
     * Check whether the supplied object is NOT NULL
     *
     * @param object supplied object
     */
    public static boolean notNull(Supplier<Object> object) {
        return !isNull(object);
    }

    /**
     * Null-safe check whether the supplied condition is met
     *
     * @param condition supplied condition
     */
    public static boolean checkCondition(Supplier<Boolean> condition) {
        return !isNull(condition::get) && condition.get();
    }
}
