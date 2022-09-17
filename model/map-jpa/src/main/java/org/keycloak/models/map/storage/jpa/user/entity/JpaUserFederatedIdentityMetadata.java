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
package org.keycloak.models.map.storage.jpa.user.entity;

import java.io.Serializable;

import org.keycloak.models.map.common.DeepCloner;
import org.keycloak.models.map.user.MapUserFederatedIdentityEntityImpl;

/**
 * Class that contains all the user federated identity metadata that is written as JSON into the database.
 *
 * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
 */
public class JpaUserFederatedIdentityMetadata extends MapUserFederatedIdentityEntityImpl implements Serializable {

    public JpaUserFederatedIdentityMetadata() {
        super();
    }

    public JpaUserFederatedIdentityMetadata(final DeepCloner cloner) {
        super(cloner);
    }

    private Integer entityVersion;

    public Integer getEntityVersion() {
        return entityVersion;
    }

    public void setEntityVersion(Integer entityVersion) {
        this.entityVersion = entityVersion;
    }
}
