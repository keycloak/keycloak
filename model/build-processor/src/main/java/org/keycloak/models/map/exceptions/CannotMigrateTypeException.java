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

package org.keycloak.models.map.exceptions;

import javax.lang.model.type.TypeMirror;
import java.util.Arrays;
import java.util.stream.Collectors;

public class CannotMigrateTypeException extends RuntimeException {
    private final TypeMirror toType;
    private final TypeMirror[] fromType;

    public CannotMigrateTypeException(TypeMirror toType, TypeMirror[] fromType) {
        this.toType = toType;
        this.fromType = fromType;
    }

    public String getFormattedMessage() {
        return "Cannot migrate [" + Arrays.stream(fromType).map(TypeMirror::toString).collect(Collectors.joining(", ")) + "] to " + toType.toString();
    }

}
