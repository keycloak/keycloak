/*
 *  Copyright 2016 Red Hat, Inc. and/or its affiliates
 *  and other contributors as indicated by the @author tags.
 *
 * Halogen Software Inc. All rights reserved.
 * http://www.halogensoftware.com
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * is not permitted absent prior written approval from Halogen Software Inc.
 */
package org.keycloak.models.jpa;

import java.util.Set;

import javax.persistence.EntityManager;

import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.jpa.entities.ClientEntity;

public class PrefetchClientAdapter extends ClientAdapter implements ClientModel {

    private Set<RoleModel> scopeMappings;

    public PrefetchClientAdapter(RealmModel realm, EntityManager em, KeycloakSession session,
            ClientEntity entity, Set<RoleModel> scopeMappings) {
        super(realm, em, session, entity);
        this.scopeMappings = scopeMappings;
    }

    @Override
    public Set<RoleModel> getScopeMappings() {
        return scopeMappings;
    }

    @Override
    public void addScopeMapping(RoleModel role) {
        super.addScopeMapping(role);
        this.scopeMappings.add(role);
    }
}
