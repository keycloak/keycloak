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

package org.keycloak.models;

import java.io.Serializable;

/**
 * Model implementation of an organization internet domain.
 *
 * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
 */
public class OrganizationDomainModel implements Serializable {

    /**
     * Value used to link an identity provider with all domains from the organization. If the user's email domain matches
     * any of the organization domains, automatic redirection to the identity will be performed.
     */
    public static final String ANY_DOMAIN = "ANY";

    private final String name;
    private final boolean verified;

    public OrganizationDomainModel(String name) {
        this(name, false);
    }

    public OrganizationDomainModel(String name, boolean verified) {
        this.name = name == null ? null : name.trim().toLowerCase();
        this.verified = verified;
    }

    public String getName() {
        return this.name;
    }

    public boolean isVerified() {
        return this.verified;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        if (!(o instanceof OrganizationDomainModel)) return false;

        OrganizationDomainModel that = (OrganizationDomainModel) o;
        return name != null && name.equals(that.getName());
    }

    @Override
    public int hashCode() {
        if (name == null) {
            return super.hashCode();
        }
        return name.hashCode();
    }
}
