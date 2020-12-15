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
package org.keycloak.models.map.storage;

import org.keycloak.models.map.storage.ModelCriteriaBuilder.Operator;
import org.keycloak.storage.SearchableModelField;

/**
 *
 * @author hmlnarik
 */
public class CriterionNotSupportedException extends RuntimeException {
    private final SearchableModelField field;
    private final Operator op;

    public CriterionNotSupportedException(SearchableModelField field, Operator op) {
        super("Criterion not supported: operator: " + op + ", field: " + field);
        this.field = field;
        this.op = op;
    }

    public CriterionNotSupportedException(SearchableModelField field, Operator op, String message) {
        super(message);
        this.field = field;
        this.op = op;
    }

    public CriterionNotSupportedException(SearchableModelField field, Operator op, String message, Throwable cause) {
        super(message, cause);
        this.field = field;
        this.op = op;
    }

    public SearchableModelField getField() {
        return field;
    }

    public Operator getOp() {
        return op;
    }
}
