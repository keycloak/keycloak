/*
 * Copyright 2022. Red Hat, Inc. and/or its affiliates
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

package org.keycloak.models.map.storage.ldap.user.entity;

import org.keycloak.models.map.common.delegate.EntityFieldDelegate;
import org.keycloak.models.map.credential.MapSubjectCredentialManagerEntity;
import org.keycloak.models.map.user.MapUserEntity;
import org.keycloak.models.map.user.MapUserEntityFieldDelegate;
import org.keycloak.models.map.storage.ldap.model.LdapMapObject;

public class LdapMapUserEntityFieldDelegate extends MapUserEntityFieldDelegate {

    public LdapMapUserEntityFieldDelegate(EntityFieldDelegate<MapUserEntity> entityFieldDelegate) {
        super(entityFieldDelegate);
    }

    @Override
    public LdapUserEntity getEntityFieldDelegate() {
        return (LdapUserEntity) super.getEntityFieldDelegate();
    }

    @Override
    public boolean isUpdated() {
        // TODO: EntityFieldDelegate.isUpdated is broken, as it is never updated
        return getEntityFieldDelegate().isUpdated();
    }

    @Override
    public MapSubjectCredentialManagerEntity credentialManager() {
        return getEntityFieldDelegate().getUserCredentialManager();
    }

    public LdapMapObject getLdapMapObject() {
        return getEntityFieldDelegate().getLdapMapObject();
    }

}
