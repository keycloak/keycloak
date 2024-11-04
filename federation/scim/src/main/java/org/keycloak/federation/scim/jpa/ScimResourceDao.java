package org.keycloak.federation.scim.jpa;

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.TypedQuery;
import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.federation.scim.core.service.EntityOnRemoteScimId;
import org.keycloak.federation.scim.core.service.KeycloakId;
import org.keycloak.federation.scim.core.service.ScimResourceType;

import java.util.Optional;

public class ScimResourceDao {

    private final String realmId;

    private final String componentId;

    private final EntityManager entityManager;

    private ScimResourceDao(String realmId, String componentId, EntityManager entityManager) {
        this.realmId = realmId;
        this.componentId = componentId;
        this.entityManager = entityManager;
    }

    public static ScimResourceDao newInstance(KeycloakSession keycloakSession, String componentId) {
        String realmId = keycloakSession.getContext().getRealm().getId();
        EntityManager entityManager = keycloakSession.getProvider(JpaConnectionProvider.class).getEntityManager();
        return new ScimResourceDao(realmId, componentId, entityManager);
    }

    private EntityManager getEntityManager() {
        return entityManager;
    }

    private String getRealmId() {
        return realmId;
    }

    private String getComponentId() {
        return componentId;
    }

    public void create(KeycloakId id, EntityOnRemoteScimId externalId, ScimResourceType type) {
        ScimResourceMapping entity = new ScimResourceMapping();
        entity.setType(type.name());
        entity.setExternalId(externalId.asString());
        entity.setComponentId(componentId);
        entity.setRealmId(realmId);
        entity.setId(id.asString());
        entityManager.persist(entity);
    }

    private TypedQuery<ScimResourceMapping> getScimResourceTypedQuery(String queryName, String id, ScimResourceType type) {
        return getEntityManager().createNamedQuery(queryName, ScimResourceMapping.class).setParameter("type", type.name())
                .setParameter("realmId", getRealmId()).setParameter("componentId", getComponentId()).setParameter("id", id);
    }

    public Optional<ScimResourceMapping> findByExternalId(EntityOnRemoteScimId externalId, ScimResourceType type) {
        try {
            return Optional.of(getScimResourceTypedQuery("findByExternalId", externalId.asString(), type).getSingleResult());
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }

    public Optional<ScimResourceMapping> findById(KeycloakId keycloakId, ScimResourceType type) {
        try {
            return Optional.of(getScimResourceTypedQuery("findById", keycloakId.asString(), type).getSingleResult());
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }

    public Optional<ScimResourceMapping> findUserById(KeycloakId id) {
        return findById(id, ScimResourceType.USER);
    }

    public Optional<ScimResourceMapping> findUserByExternalId(EntityOnRemoteScimId externalId) {
        return findByExternalId(externalId, ScimResourceType.USER);
    }

    public void delete(ScimResourceMapping resource) {
        entityManager.remove(resource);
    }
}
