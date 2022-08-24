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

package org.keycloak.models.map.singleUseObject;

import org.keycloak.models.map.annotations.GenerateEntityImplementations;
import org.keycloak.models.map.common.AbstractEntity;
import org.keycloak.models.map.common.DeepCloner;
import org.keycloak.models.map.common.ExpirableEntity;
import org.keycloak.models.map.common.UpdatableEntity;

import java.util.Map;

/**
 * @author <a href="mailto:mkanis@redhat.com">Martin Kanis</a>
 */
@GenerateEntityImplementations(
        inherits = "org.keycloak.models.map.singleUseObject.MapSingleUseObjectEntity.AbstractSingleUseObjectEntity"
)
@DeepCloner.Root
public interface MapSingleUseObjectEntity extends AbstractEntity, UpdatableEntity, ExpirableEntity {

    public abstract class AbstractSingleUseObjectEntity extends UpdatableEntity.Impl implements MapSingleUseObjectEntity {

        private String id;

        @Override
        public String getId() {
            return this.id;
        }

        @Override
        public void setId(String id) {
            if (this.id != null) throw new IllegalStateException("Id cannot be changed");
            this.id = id;
            this.updated |= id != null;
        }
    }

    String getUserId();
    void setUserId(String userId);

    String getObjectKey();
    void setObjectKey(String objectKey);

    String getActionId();
    void setActionId(String actionId);

    String getActionVerificationNonce();
    void setActionVerificationNonce(String actionVerificationNonce);

    Map<String, String> getNotes();
    void setNotes(Map<String, String> notes);
    String getNote(String name);
    void setNote(String key, String value);
}
