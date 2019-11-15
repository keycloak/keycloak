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

package org.keycloak.models.cache.infinispan.entities;

import org.keycloak.models.ClientScopeModel;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class CachedClientScope extends AbstractRevisioned implements InRealm {

    private String name;
    private String description;
    private String realm;
    private String protocol;
    private Set<String> scope = new HashSet<>();
    private Set<ProtocolMapperModel> protocolMappers = new HashSet<>();
    private Map<String, String> attributes = new HashMap<>();

    public CachedClientScope(Long revision, RealmModel realm, ClientScopeModel model) {
        super(revision, model.getId());
        name = model.getName();
        description = model.getDescription();
        this.realm = realm.getId();
        protocol = model.getProtocol();
        for (ProtocolMapperModel mapper : model.getProtocolMappers()) {
            this.protocolMappers.add(mapper);
        }
        for (RoleModel role : model.getScopeMappings())  {
            scope.add(role.getId());
        }
        attributes.putAll(model.getAttributes());
    }

    public String getName() {
        return name;
    }

    public String getDescription() { return description; }

    public void setDescription(String description) { this.description = description; }

    public String getRealm() {
        return realm;
    }
    public Set<ProtocolMapperModel> getProtocolMappers() {
        return protocolMappers;
    }

    public String getProtocol() {
        return protocol;
    }

    public Set<String> getScope() {
        return scope;
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }
}
