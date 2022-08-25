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
package org.keycloak.models.map.storage.jpa.singleUseObject.entity;

import java.io.Serializable;

import org.keycloak.models.map.common.DeepCloner;
import org.keycloak.models.map.singleUseObject.MapSingleUseObjectEntityImpl;

/**
 * Class that contains all the single-use object metadata that is written as JSON into the database.
 *
 * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
 */
public class JpaSingleUseObjectMetadata extends MapSingleUseObjectEntityImpl implements Serializable {

    public JpaSingleUseObjectMetadata() {
        super();
    }

    public JpaSingleUseObjectMetadata(final DeepCloner cloner) {
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
