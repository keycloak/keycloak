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
package org.keycloak.protocol.oidc.rar.model;

import java.util.Objects;

import org.keycloak.models.ClientScopeModel;

/**
 * @author <a href="mailto:dgozalob@redhat.com">Daniel Gozalo</a>
 */
public class IntermediaryScopeRepresentation {
    final private ClientScopeModel scope;
    final private String requestedScopeString;
    final private String parameter;
    final private boolean isDynamic;

    public IntermediaryScopeRepresentation(ClientScopeModel scope, String parameter, String requestedScopeString) {
        this.scope = scope;
        this.parameter = parameter;
        this.isDynamic = scope.isDynamicScope();
        this.requestedScopeString = requestedScopeString;
    }

    public IntermediaryScopeRepresentation(ClientScopeModel scope) {
        this.scope = scope;
        this.isDynamic = false;
        this.parameter = null;
        this.requestedScopeString = scope.getName();
    }

    public ClientScopeModel getScope() {
        return scope;
    }

    public String getParameter() {
        return parameter;
    }

    public boolean isDynamic() {
        return isDynamic;
    }

    public String getRequestedScopeString() {
        return requestedScopeString;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        IntermediaryScopeRepresentation that = (IntermediaryScopeRepresentation) o;
        return isDynamic == that.isDynamic && Objects.equals(scope.getName(), that.scope.getName()) && Objects.equals(parameter, that.parameter);
    }

    @Override
    public int hashCode() {
        return Objects.hash(scope.getName(), parameter, isDynamic);
    }
}
