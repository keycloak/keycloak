/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.organization.jpa;

import java.time.LocalDateTime;
import java.util.stream.Stream;

import jakarta.persistence.EntityManager;

import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.OrganizationModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.organization.OrganizationInvitationModel;
import org.keycloak.organization.OrganizationInvitationProvider;

/**
 * JPA implementation of OrganizationInvitationProvider.
 */
public class JpaOrganizationInvitationProvider implements OrganizationInvitationProvider {

    private final KeycloakSession session;
    private final EntityManager em;

    public JpaOrganizationInvitationProvider(KeycloakSession session) {
        this.session = session;
        this.em = session.getProvider(JpaConnectionProvider.class).getEntityManager();
    }

    @Override
    public OrganizationInvitationModel createInvitation(OrganizationModel organization, String email, 
                                                       String firstName, String lastName) {
        LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(
            session.getContext().getRealm().getActionTokenGeneratedByAdminLifespan()
        );
        return createInvitation(organization, email, firstName, lastName, expiresAt);
    }

    @Override
    public OrganizationInvitationModel createInvitation(OrganizationModel organization, String email, 
                                                       String firstName, String lastName, 
                                                       LocalDateTime expiresAt) {
        String id = KeycloakModelUtils.generateId();
        OrganizationInvitationEntity entity = new OrganizationInvitationEntity(
            id, organization.getId(), email.trim(), 
            firstName != null ? firstName.trim() : null, 
            lastName != null ? lastName.trim() : null
        );
        entity.setExpiresAt(expiresAt);
        
        em.persist(entity);
        em.flush();
        
        return entity;
    }

    @Override
    public OrganizationInvitationModel getById(String id, OrganizationModel organization) {
        return em.createQuery(
                "SELECT i FROM OrganizationInvitationEntity i WHERE i.id = :id AND i.organizationId = :orgId", 
                OrganizationInvitationEntity.class)
                .setParameter("id", id)
                .setParameter("orgId", organization.getId())
                .getResultStream()
                .findFirst()
                .orElse(null);
    }

    @Override
    public OrganizationInvitationModel getByEmailAndOrganization(String email, OrganizationModel organization) {
        return em.createQuery(
                "SELECT i FROM OrganizationInvitationEntity i WHERE i.email = :email AND i.organizationId = :orgId", 
                OrganizationInvitationEntity.class)
                .setParameter("email", email)
                .setParameter("orgId", organization.getId())
                .getResultStream()
                .findFirst()
                .orElse(null);
    }

    @Override
    public Stream<OrganizationInvitationModel> getAllInvitations(OrganizationModel organization) {
        return em.createQuery(
                "SELECT i FROM OrganizationInvitationEntity i WHERE i.organizationId = :orgId ORDER BY i.createdAt DESC", 
                OrganizationInvitationEntity.class)
                .setParameter("orgId", organization.getId())
                .getResultStream()
                .map(OrganizationInvitationModel.class::cast);
    }

    @Override
    public boolean deleteInvitation(OrganizationModel organization, String invitationId) {
        OrganizationInvitationEntity invitation = (OrganizationInvitationEntity) getById(invitationId, organization);
        
        if (invitation == null) {
            return false;
        }
        
        em.remove(invitation);
        em.flush();
        return true;
    }

    @Override
    public void close() {
    }
}
