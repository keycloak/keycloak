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
package org.keycloak.operator.v2alpha1.crds;

import java.util.List;
import java.util.Objects;

/**
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public class KeycloakStatus {
    private List<KeycloakStatusCondition> conditions;

    public List<KeycloakStatusCondition> getConditions() {
        return conditions;
    }

    public void setConditions(List<KeycloakStatusCondition> conditions) {
        this.conditions = conditions;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        KeycloakStatus status = (KeycloakStatus) o;
        return Objects.equals(getConditions(), status.getConditions());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getConditions());
    }
}
