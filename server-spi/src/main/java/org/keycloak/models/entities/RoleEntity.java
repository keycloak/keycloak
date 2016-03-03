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

package org.keycloak.models.entities;

import java.util.List;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class RoleEntity extends AbstractIdentifiableEntity {

    private String name;
    private String description;
    private boolean scopeParamRequired;

    private List<String> compositeRoleIds;

    private String realmId;
    private String clientId;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isScopeParamRequired() {
        return scopeParamRequired;
    }

    public void setScopeParamRequired(boolean scopeParamRequired) {
        this.scopeParamRequired = scopeParamRequired;
    }

    public List<String> getCompositeRoleIds() {
        return compositeRoleIds;
    }

    public void setCompositeRoleIds(List<String> compositeRoleIds) {
        this.compositeRoleIds = compositeRoleIds;
    }

    public String getRealmId() {
        return realmId;
    }

    public void setRealmId(String realmId) {
        this.realmId = realmId;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

}
