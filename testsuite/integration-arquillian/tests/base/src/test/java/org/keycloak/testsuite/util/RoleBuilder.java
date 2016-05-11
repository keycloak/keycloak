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

package org.keycloak.testsuite.util;

import org.keycloak.representations.idm.RoleRepresentation;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class RoleBuilder {

    private RoleRepresentation rep = new RoleRepresentation();

    public static RoleBuilder create() {
        return new RoleBuilder();
    }

    private RoleBuilder() {
    }

    public RoleBuilder id(String id) {
        rep.setId(id);
        return this;
    }

    public RoleBuilder name(String name) {
        rep.setName(name);
        return this;
    }

    public RoleBuilder description(String description) {
        rep.setDescription(description);
        return this;
    }

    public RoleBuilder scopeParamRequired(Boolean required) {
        rep.setScopeParamRequired(required);
        return this;
    }

    public RoleRepresentation build() {
        return rep;
    }

}
