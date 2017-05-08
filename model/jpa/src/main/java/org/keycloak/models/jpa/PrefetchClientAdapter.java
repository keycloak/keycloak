/* 
 * This file is copyright 2002-2017
 *
 * Halogen Software Inc. All rights reserved.
 * http://www.halogensoftware.com
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
