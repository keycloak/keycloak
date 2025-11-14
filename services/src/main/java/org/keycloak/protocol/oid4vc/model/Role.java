/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.protocol.oid4vc.model;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;

import org.keycloak.protocol.oid4vc.issuance.mappers.OID4VCTargetRoleMapper;

/**
 * Pojo representation of a role to be added by the {@link OID4VCTargetRoleMapper}
 *
 * @author <a href="https://github.com/wistefan">Stefan Wiedemann</a>
 */
public class Role {

    private Set<String> names;
    private String target;

    public Role() {
    }

    public Role(Set<String> names, String target) {
        this.names = Collections.unmodifiableSet(names);
        this.target = target;
    }

    public Set<String> getNames() {
        return names;
    }

    public void setNames(Set<String> names) {
        this.names = names;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Role role = (Role) o;
        return Objects.equals(names, role.names) && Objects.equals(target, role.target);
    }

    @Override
    public int hashCode() {
        return Objects.hash(names, target);
    }
}