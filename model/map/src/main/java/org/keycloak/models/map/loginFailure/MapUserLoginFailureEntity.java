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
package org.keycloak.models.map.loginFailure;

import org.keycloak.models.map.annotations.GenerateEntityImplementations;
import org.keycloak.models.map.annotations.IgnoreForEntityImplementationGenerator;
import org.keycloak.models.map.common.AbstractEntity;

import org.keycloak.models.map.common.DeepCloner;
import org.keycloak.models.map.common.UpdatableEntity;

/**
 * @author <a href="mailto:mkanis@redhat.com">Martin Kanis</a>
 */
@GenerateEntityImplementations(
        inherits = "org.keycloak.models.map.loginFailure.MapUserLoginFailureEntity.AbstractUserLoginFailureEntity"
)
@DeepCloner.Root
public interface MapUserLoginFailureEntity extends AbstractEntity, UpdatableEntity {

    public abstract class AbstractUserLoginFailureEntity extends UpdatableEntity.Impl implements MapUserLoginFailureEntity {

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

        @Override
        public void clearFailures() {
            this.updated |= getFailedLoginNotBefore() != null || getNumFailures() != null || getLastFailure() != null || getLastIPFailure() != null;
            setFailedLoginNotBefore(null);
            setNumFailures(null);
            setLastFailure(null);
            setLastIPFailure(null);
        }
    }

    String getRealmId();
    void setRealmId(String realmId);

    String getUserId();
    void setUserId(String userId);

    Long getFailedLoginNotBefore();
    void setFailedLoginNotBefore(Long failedLoginNotBefore);

    Integer getNumFailures();
    void setNumFailures(Integer numFailures);

    Long getLastFailure();
    void setLastFailure(Long lastFailure);

    String getLastIPFailure();
    void setLastIPFailure(String lastIPFailure);

    @IgnoreForEntityImplementationGenerator
    void clearFailures();
}
