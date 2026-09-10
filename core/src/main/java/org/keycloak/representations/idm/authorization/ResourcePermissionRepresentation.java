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
package org.keycloak.representations.idm.authorization;

import java.util.Set;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class ResourcePermissionRepresentation extends AbstractPolicyRepresentation {

    public static Builder create() {
        return new Builder();
    }

    private String resourceType;

    @Override
    public String getType() {
        return "resource";
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public String getResourceType() {
        return resourceType;
    }

    public static final class Builder {

        private final ResourcePermissionRepresentation rep;

        private Builder() {
            rep = new ResourcePermissionRepresentation();
        }

        public Builder name(String name) {
            rep.setName(name);
            return this;
        }

        public Builder resources(Set<String> ids) {
            rep.setResources(ids);
            return this;
        }

        public Builder policies(Set<String> ids) {
            rep.setPolicies(ids);
            return this;
        }

        public Builder policy(AbstractPolicyRepresentation policy) {
            String id = policy.getId();

            if (id == null) {
                throw new IllegalArgumentException("Policy must have an id");
            }

            rep.addPolicy(id);

            return this;
        }

        public ResourcePermissionRepresentation build() {
            return rep;
        }
    }
}
