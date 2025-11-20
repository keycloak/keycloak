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

package org.keycloak.models.jpa;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaDelete;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.MapJoin;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import org.keycloak.authorization.fgap.AdminPermissionsSchema;
import org.keycloak.client.clienttype.ClientTypeManager;
import org.keycloak.common.Profile;
import org.keycloak.common.util.Time;
import org.keycloak.connections.jpa.util.JpaUtils;
import org.keycloak.migration.MigrationModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientProvider;
import org.keycloak.models.ClientScopeModel;
import org.keycloak.models.ClientScopeProvider;
import org.keycloak.models.DeploymentStateProvider;
import org.keycloak.models.GroupModel;
import org.keycloak.models.GroupModel.GroupCreatedEvent;
import org.keycloak.models.GroupModel.GroupPathChangeEvent;
import org.keycloak.models.GroupModel.GroupUpdatedEvent;
import org.keycloak.models.GroupModel.Type;
import org.keycloak.models.GroupProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelDuplicateException;
import org.keycloak.models.ModelException;
import org.keycloak.models.ModelValidationException;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RealmProvider;
import org.keycloak.models.RoleContainerModel;
import org.keycloak.models.RoleContainerModel.RoleRemovedEvent;
import org.keycloak.models.RoleModel;
import org.keycloak.models.RoleProvider;
import org.keycloak.models.delegate.ClientModelLazyDelegate;
import org.keycloak.models.jpa.entities.ClientAttributeEntity;
import org.keycloak.models.jpa.entities.ClientEntity;
import org.keycloak.models.jpa.entities.ClientScopeClientMappingEntity;
import org.keycloak.models.jpa.entities.ClientScopeEntity;
import org.keycloak.models.jpa.entities.GroupAttributeEntity;
import org.keycloak.models.jpa.entities.GroupEntity;
import org.keycloak.models.jpa.entities.RealmEntity;
import org.keycloak.models.jpa.entities.RealmLocalizationTextsEntity;
import org.keycloak.models.jpa.entities.RoleEntity;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;

import org.hibernate.Session;
import org.jboss.logging.Logger;

import static org.keycloak.common.util.StackUtil.getShortStackTrace;
import static org.keycloak.models.jpa.PaginationUtils.paginateQuery;
import static org.keycloak.utils.StreamsUtil.closing;


/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class JpaRealmProvider implements RealmProvider, ClientProvider, ClientScopeProvider, GroupProvider, RoleProvider, DeploymentStateProvider {
    protected static final Logger logger = Logger.getLogger(JpaRealmProvider.class);
    private final KeycloakSession session;
    protected EntityManager em;
    private Set<String> clientSearchableAttributes;
    private Set<String> groupSearchableAttributes;

    public JpaRealmProvider(KeycloakSession session, EntityManager em, Set<String> clientSearchableAttributes, Set<String> groupSearchableAttributes) {
        this.session = session;
        this.em = em;
        this.clientSearchableAttributes = clientSearchableAttributes;
        this.groupSearchableAttributes = groupSearchableAttributes;
    }

    @Override
    public MigrationModel getMigrationModel() {
        return new MigrationModelAdapter(em);
    }

    @Override
    public RealmModel createRealm(String name) {
        return createRealm(KeycloakModelUtils.generateId(), name);
    }

    @Override
    public RealmModel createRealm(String id, String name) {
        RealmEntity realm = new RealmEntity();
        realm.setName(name);
        realm.setId(id);
        em.persist(realm);
        em.flush();
        final RealmModel adapter = new RealmAdapter(session, em, realm);
        session.getKeycloakSessionFactory().publish(new RealmModel.RealmCreationEvent() {
            @Override
            public RealmModel getCreatedRealm() {
                return adapter;
            }
            @Override
            public KeycloakSession getKeycloakSession() {
            	return session;
            }
        });
        return adapter;
    }

    @Override
    public RealmModel getRealm(String id) {
        RealmEntity realm = em.find(RealmEntity.class, id);
        if (realm == null) return null;
        RealmAdapter adapter = new RealmAdapter(session, em, realm);
        return adapter;
    }

    @Override
    public Stream<RealmModel> getRealmsWithProviderTypeStream(Class<?> providerType) {
        TypedQuery<String> query = em.createNamedQuery("getRealmIdsWithProviderType", String.class);
        query.setParameter("providerType", providerType.getName());
        return getRealms(query);
    }

    @Override
    public Stream<RealmModel> getRealmsStream() {
        TypedQuery<String> query = em.createNamedQuery("getAllRealmIds", String.class);
        return getRealms(query);
    }

    @Override
    public Stream<RealmModel> getRealmsStream(String search) {
        if (search.trim().isEmpty()) {
            return getRealmsStream();
        }
        TypedQuery<String> query = em.createNamedQuery("getRealmIdsWithNameContaining", String.class);
        query.setParameter("search", search);
        return getRealms(query);
    }

    private Stream<RealmModel> getRealms(TypedQuery<String> query) {
        return closing(query.getResultStream().map(session.realms()::getRealm).filter(Objects::nonNull));
    }

    @Override
    public RealmModel getRealmByName(String name) {
        TypedQuery<String> query = em.createNamedQuery("getRealmIdByName", String.class);
        query.setParameter("name", name);
        List<String> entities = query.getResultList();
        if (entities.isEmpty()) return null;
        if (entities.size() > 1) throw new IllegalStateException("Should not be more than one realm with same name");
        String id = query.getResultList().get(0);

        return session.realms().getRealm(id);
    }

    @Override
    public boolean removeRealm(String id) {
        RealmEntity realm = em.find(RealmEntity.class, id, LockModeType.PESSIMISTIC_WRITE);
        if (realm == null) {
            return false;
        }
        final RealmAdapter adapter = new RealmAdapter(session, em, realm);
        session.users().preRemove(adapter);

        realm.getDefaultGroupIds().clear();
        em.flush();

        session.clients().removeClients(adapter);

        em.createNamedQuery("deleteDefaultClientScopeRealmMappingByRealm")
                .setParameter("realm", realm).executeUpdate();

        session.clientScopes().removeClientScopes(adapter);
        session.roles().removeRoles(adapter);

        em.createNamedQuery("deleteOrganizationDomainsByRealm")
                .setParameter("realmId", realm.getId()).executeUpdate();
        em.createNamedQuery("deleteOrganizationsByRealm")
                .setParameter("realmId", realm.getId()).executeUpdate();
        session.groups().preRemove(adapter);

        session.identityProviders().removeAll();
        session.identityProviders().removeAllMappers();

        em.createNamedQuery("removeClientInitialAccessByRealm")
                .setParameter("realm", realm).executeUpdate();

        em.remove(realm);

        em.flush();
        em.clear();

        session.getKeycloakSessionFactory().publish(new RealmModel.RealmRemovedEvent() {
            @Override
            public RealmModel getRealm() {
                return adapter;
            }

            @Override
            public KeycloakSession getKeycloakSession() {
                return session;
            }
        });

        return true;
    }

    @Override
    public void close() {
    }

    @Override
    public RoleModel addRealmRole(RealmModel realm, String name) {
       return addRealmRole(realm, KeycloakModelUtils.generateId(), name);

    }
    @Override
    public RoleModel addRealmRole(RealmModel realm, String id, String name) {
        if (getRealmRole(realm, name) != null) {
            throw new ModelDuplicateException();
        }
        RoleEntity entity = new RoleEntity();
        entity.setId(id);
        entity.setName(name);
        entity.setRealmId(realm.getId());
        em.persist(entity);
        em.flush();
        RoleAdapter adapter = new RoleAdapter(session, realm, em, entity);
        return adapter;

    }

    @Override
    public RoleModel getRealmRole(RealmModel realm, String name) {
        TypedQuery<String> query = em.createNamedQuery("getRealmRoleIdByName", String.class);
        query.setParameter("name", name);
        query.setParameter("realm", realm.getId());
        List<String> roles = query.getResultList();
        if (roles.isEmpty()) return null;
        return session.roles().getRoleById(realm, roles.get(0));
    }

    @Override
    public RoleModel addClientRole(ClientModel client, String name) {
        return addClientRole(client, KeycloakModelUtils.generateId(), name);
    }

    @Override
    public RoleModel addClientRole(ClientModel client, String id, String name) {
        if (getClientRole(client, name) != null) {
            throw new ModelDuplicateException();
        }
        RoleEntity roleEntity = new RoleEntity();
        roleEntity.setId(id);
        roleEntity.setName(name);
        roleEntity.setRealmId(client.getRealm().getId());
        roleEntity.setClientId(client.getId());
        roleEntity.setClientRole(true);
        em.persist(roleEntity);
        RoleAdapter adapter = new RoleAdapter(session, client.getRealm(), em, roleEntity);
        return adapter;
    }

    @Override
    public Stream<RoleModel> getRealmRolesStream(RealmModel realm) {
        TypedQuery<String> query = em.createNamedQuery("getRealmRoleIds", String.class);
        query.setParameter("realm", realm.getId());
        Stream<String> roles = query.getResultStream();

        return closing(roles.map(realm::getRoleById).filter(Objects::nonNull));
    }

    @Override
    public RoleModel getClientRole(ClientModel client, String name) {
        TypedQuery<String> query = em.createNamedQuery("getClientRoleIdByName", String.class);
        query.setParameter("name", name);
        query.setParameter("client", client.getId());
        List<String> roles = query.getResultList();
        if (roles.isEmpty()) return null;
        return session.roles().getRoleById(client.getRealm(), roles.get(0));
    }

    @Override
    public Map<ClientModel, Set<String>> getAllRedirectUrisOfEnabledClients(RealmModel realm) {
        TypedQuery<Map> query = em.createNamedQuery("getAllRedirectUrisOfEnabledClients", Map.class);
        query.setParameter("realm", realm.getId());
        return closing(query.getResultStream()
                .filter(s -> s.get("client") != null))
                .collect(
                        Collectors.groupingBy(
                                s -> toClientModel(realm, (ClientEntity) s.get("client")),
                                Collectors.mapping(s -> (String) s.get("redirectUri"), Collectors.toSet())
                        )
                );

    }

    @Override
    public Stream<RoleModel> getRealmRolesStream(RealmModel realm, Integer first, Integer max) {
        TypedQuery<RoleEntity> query = em.createNamedQuery("getRealmRoles", RoleEntity.class);
        query.setParameter("realm", realm.getId());

        return getRolesStream(query, realm, first, max);
    }

    @Override
    public Stream<RoleModel> getRolesStream(RealmModel realm, Stream<String> ids, String search, Integer first, Integer max) {
        if (ids == null) return Stream.empty();

        TypedQuery<String> query;

        if (search == null) {
            query = em.createNamedQuery("getRoleIdsFromIdList", String.class);
        } else {
            query = em.createNamedQuery("getRoleIdsByNameContainingFromIdList", String.class)
                    .setParameter("search", search);
        }

        query.setParameter("realm", realm.getId())
                .setParameter("ids", ids.collect(Collectors.toList()));

        return closing(paginateQuery(query, first, max).getResultStream())
                .map(g -> session.roles().getRoleById(realm, g)).filter(Objects::nonNull);
    }

    @Override
    public Stream<RoleModel> searchForClientRolesStream(RealmModel realm, Stream<String> ids, String search, Integer first, Integer max) {
        return searchForClientRolesStream(realm, ids, search, first, max, false);
    }
    @Override
    public Stream<RoleModel> searchForClientRolesStream(RealmModel realm, String search, Stream<String> excludedIds, Integer first, Integer max) {
        return searchForClientRolesStream(realm, excludedIds, search, first, max, true);
    }

    private Stream<RoleModel> searchForClientRolesStream(RealmModel realm, Stream<String> ids, String search, Integer first, Integer max, boolean negateIds) {
        List<String> idList = null;
        if(ids != null) {
            idList = ids.collect(Collectors.toList());
            if(idList.isEmpty() && !negateIds)
                return Stream.empty();
        }
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<RoleEntity> query = cb.createQuery(RoleEntity.class);

        Root<RoleEntity> roleRoot = query.from(RoleEntity.class);
        Root<ClientEntity> clientRoot = query.from(ClientEntity.class);

        List<Predicate> predicates = new ArrayList<>();
        predicates.add(cb.equal(roleRoot.get("realmId"), realm.getId()));
        predicates.add(cb.isTrue(roleRoot.get("clientRole")));
        predicates.add(cb.equal(roleRoot.get("clientId"),clientRoot.get("id")));
        if(search != null && !search.isEmpty()) {
            search = "%" + search.trim().toLowerCase() + "%";
            predicates.add(cb.or(
                    cb.like(cb.lower(roleRoot.get("name")), search),
                    cb.like(cb.lower(clientRoot.get("clientId")), search)
            ));
        }
        if(idList != null && !idList.isEmpty()) {
            Predicate idFilter = roleRoot.get("id").in(idList);
            if(negateIds) idFilter = cb.not(idFilter);
            predicates.add(idFilter);
        }
        query.select(roleRoot).where(predicates.toArray(new Predicate[0]))
                .orderBy(
                        cb.asc(clientRoot.get("clientId")),
                        cb.asc(roleRoot.get("name")));
        return closing(paginateQuery(em.createQuery(query),first,max).getResultStream())
                .map(roleEntity -> new RoleAdapter(session, realm, em, roleEntity));
    }


    @Override
    public Stream<RoleModel> getClientRolesStream(ClientModel client, Integer first, Integer max) {
        TypedQuery<RoleEntity> query = em.createNamedQuery("getClientRoles", RoleEntity.class);
        query.setParameter("client", client.getId());

        return getRolesStream(query, client.getRealm(), first, max);
    }

    protected Stream<RoleModel> getRolesStream(TypedQuery<RoleEntity> query, RealmModel realm, Integer first, Integer max) {
        Stream<RoleEntity> results = paginateQuery(query, first, max).getResultStream();

        return closing(results.map(role -> new RoleAdapter(session, realm, em, role)));
    }

    @Override
    public Stream<RoleModel> searchForClientRolesStream(ClientModel client, String search, Integer first, Integer max) {
        TypedQuery<RoleEntity> query = em.createNamedQuery("searchForClientRoles", RoleEntity.class);
        query.setParameter("client", client.getId());
        return searchForRoles(query, client.getRealm(), search, first, max);
    }

    @Override
    public Stream<RoleModel> searchForRolesStream(RealmModel realm, String search, Integer first, Integer max) {
        TypedQuery<RoleEntity> query = em.createNamedQuery("searchForRealmRoles", RoleEntity.class);
        query.setParameter("realm", realm.getId());

        return searchForRoles(query, realm, search, first, max);
    }

    protected Stream<RoleModel> searchForRoles(TypedQuery<RoleEntity> query, RealmModel realm, String search, Integer first, Integer max) {
        query.setParameter("search", "%" + search.trim().toLowerCase() + "%");
        Stream<RoleEntity> results = paginateQuery(query, first, max).getResultStream();

        return closing(results.map(role -> new RoleAdapter(session, realm, em, role)));
    }

    @Override
    public boolean removeRole(RoleModel role) {
        RealmModel realm;
        if (role.getContainer() instanceof RealmModel) {
            realm = (RealmModel) role.getContainer();
        } else if (role.getContainer() instanceof ClientModel) {
            realm = ((ClientModel)role.getContainer()).getRealm();
        } else {
            throw new IllegalStateException("RoleModel's container isn not instance of either RealmModel or ClientModel");
        }
        session.users().preRemove(realm, role);
        RoleEntity roleEntity = em.getReference(RoleEntity.class, role.getId());
        if (roleEntity == null || !roleEntity.getRealmId().equals(realm.getId())) {
            // Throw model exception to ensure transaction rollback and revert previous operations (removing default roles) as well
            throw new ModelException("Role not found or trying to remove role from incorrect realm");
        }

        // Can't use a native query to delete the composite roles mappings because it causes TransientObjectException.
        // At the same time, can't use the persist cascade type on the compositeRoles field because in that case
        // we could not still use a native query as a different problem would arise - it may happen that a parent role that
        // has this role as a composite is present in the persistence context. In that case it, the role would be re-created
        // again after deletion through persist cascade type.
        // So in any case, native query is not an option. This is not optimal as it executes additional queries but
        // the alternative of clearing the persistence context is not either as we don't know if something currently present
        // in the context is not needed later.

        roleEntity.getCompositeRoles().forEach(childRole -> childRole.getParentRoles().remove(roleEntity));
        roleEntity.getParentRoles().forEach(parentRole -> parentRole.getCompositeRoles().remove(roleEntity));

        em.createNamedQuery("deleteClientScopeRoleMappingByRole").setParameter("role", roleEntity).executeUpdate();

        em.remove(roleEntity);

        session.getKeycloakSessionFactory().publish(roleRemovedEvent(role));

        return true;

    }

    public RoleRemovedEvent roleRemovedEvent(RoleModel role) {
        return new RoleContainerModel.RoleRemovedEvent() {
            @Override
            public RoleModel getRole() {
                return role;
            }

            @Override
            public KeycloakSession getKeycloakSession() {
                return session;
            }
        };
    }

    @Override
    public void removeRoles(RealmModel realm) {
        // No need to go through cache. Roles were already invalidated
        realm.getRolesStream().forEach(this::removeRole);
    }

    @Override
    public void removeRoles(ClientModel client) {
        // No need to go through cache. Roles were already invalidated
        client.getRolesStream().forEach(this::removeRole);
    }

    @Override
    public RoleModel getRoleById(RealmModel realm, String id) {
        RoleEntity entity = em.find(RoleEntity.class, id);
        if (entity == null) return null;
        if (!realm.getId().equals(entity.getRealmId())) return null;
        RoleAdapter adapter = new RoleAdapter(session, realm, em, entity);
        return adapter;
    }

    @Override
    public GroupModel getGroupById(RealmModel realm, String id) {
        GroupEntity groupEntity = em.find(GroupEntity.class, id);
        if (groupEntity == null) return null;
        if (!groupEntity.getRealm().equals(realm.getId())) return null;
        GroupAdapter adapter =  new GroupAdapter(session, realm, em, groupEntity);
        return adapter;
    }

    @Override
    public GroupModel getGroupByName(RealmModel realm, GroupModel parent, String name) {
        CriteriaBuilder builder = em.getCriteriaBuilder();
        CriteriaQuery<String> queryBuilder = builder.createQuery(String.class);
        Root<GroupEntity> root = queryBuilder.from(GroupEntity.class);

        queryBuilder.select(root.get("id"));

        List<Predicate> predicates = new ArrayList<>();

        predicates.add(builder.equal(root.get("realm"), realm.getId()));
        predicates.add(builder.equal(root.get("type"), Type.REALM.intValue()));
        predicates.add(builder.equal(root.get("parentId"), parent != null ? parent.getId() : GroupEntity.TOP_PARENT_ID));
        predicates.add(builder.equal(root.get("name"), name));
        predicates.addAll(AdminPermissionsSchema.SCHEMA.applyAuthorizationFilters(session, AdminPermissionsSchema.GROUPS, null, realm, builder, queryBuilder, root));

        queryBuilder.where(predicates.toArray(new Predicate[0]));
        queryBuilder.orderBy(builder.asc(root.get("name")));

        List<String> groups = em.createQuery(queryBuilder).getResultList();

        if (groups.isEmpty()) return null;
        if (groups.size() > 1) throw new IllegalStateException("Should not be more than one Group with same name");

        return session.groups().getGroupById(realm, groups.get(0));
    }

    @Override
    public void moveGroup(RealmModel realm, GroupModel group, GroupModel toParent) {
        if (toParent != null && group.getId().equals(toParent.getId())) {
            return;
        }

        GroupModel previousParent = group.getParent();

        if (group.getParentId() != null) {
            group.getParent().removeChild(group);
        }
        group.setParent(toParent);
        if (toParent != null) toParent.addChild(group);
        else session.groups().addTopLevelGroup(realm, group);

        // TODO: Remove em.flush(), currently this needs to be there to translate ConstraintViolationException to
        //  DuplicateModelException {@link PersistenceExceptionConverter} is not called if the
        //  ConstraintViolationException is not thrown in method called directly from EntityManager
        em.flush();

        String newPath = KeycloakModelUtils.buildGroupPath(group);
        String previousPath = KeycloakModelUtils.buildGroupPath(group, previousParent);

        GroupPathChangeEvent.fire(group, newPath, previousPath, session);
        fireGroupUpdatedEvent(group);
    }

    @Override
    public Stream<GroupModel> getGroupsStream(RealmModel realm) {
        CriteriaBuilder builder = em.getCriteriaBuilder();
        CriteriaQuery<String> queryBuilder = builder.createQuery(String.class);
        Root<GroupEntity> root = queryBuilder.from(GroupEntity.class);

        queryBuilder.select(root.get("id"));

        List<Predicate> predicates = new ArrayList<>();

        predicates.add(builder.equal(root.get("realm"), realm.getId()));
        predicates.add(builder.equal(root.get("type"), Type.REALM.intValue()));
        predicates.addAll(AdminPermissionsSchema.SCHEMA.applyAuthorizationFilters(session, AdminPermissionsSchema.GROUPS, null, realm, builder, queryBuilder, root));

        queryBuilder.where(predicates.toArray(new Predicate[0]));
        queryBuilder.orderBy(builder.asc(root.get("name")));

        return closing(em.createQuery(queryBuilder).getResultStream())
                .map(g -> session.groups().getGroupById(realm, g))
                .filter(Objects::nonNull);
    }

    @Override
    public Stream<GroupModel> getGroupsStream(RealmModel realm, Stream<String> ids, String search, Integer first, Integer max) {
        if (search == null || search.isEmpty()) return getGroupsStream(realm, ids, first, max);

        List<String> idsList = ids.collect(Collectors.toList());
        if (idsList.isEmpty()) {
            return Stream.empty();
        }

        CriteriaBuilder builder = em.getCriteriaBuilder();
        CriteriaQuery<String> queryBuilder = builder.createQuery(String.class);
        Root<GroupEntity> root = queryBuilder.from(GroupEntity.class);

        queryBuilder.select(root.get("id"));

        List<Predicate> predicates = new ArrayList<>();

        predicates.add(builder.equal(root.get("realm"), realm.getId()));
        predicates.add(builder.equal(root.get("type"), Type.REALM.intValue()));
        predicates.add(builder.like(builder.lower(root.get("name")), builder.lower(builder.literal("%" + search + "%"))));
        predicates.add(root.get("id").in(idsList));
        predicates.addAll(AdminPermissionsSchema.SCHEMA.applyAuthorizationFilters(session, AdminPermissionsSchema.GROUPS, realm, builder, queryBuilder, root));

        queryBuilder.where(predicates.toArray(new Predicate[0]));
        queryBuilder.orderBy(builder.asc(root.get("name")));

        return closing(paginateQuery(em.createQuery(queryBuilder), first, max).getResultStream())
                .map(g -> session.groups().getGroupById(realm, g))
                .filter(Objects::nonNull);
    }

    @Override
    public Stream<GroupModel> getGroupsStream(RealmModel realm, Stream<String> ids, Integer first, Integer max) {
        if (first == null && max == null) {
            return getGroupsStream(realm, ids);
        }

        List<String> idsList = ids.toList();
        if (idsList.isEmpty()) {
            return Stream.empty();
        }

        CriteriaBuilder builder = em.getCriteriaBuilder();
        CriteriaQuery<String> queryBuilder = builder.createQuery(String.class);
        Root<GroupEntity> root = queryBuilder.from(GroupEntity.class);

        queryBuilder.select(root.get("id"));

        List<Predicate> predicates = new ArrayList<>();

        predicates.add(builder.equal(root.get("realm"), realm.getId()));
        predicates.add(builder.equal(root.get("type"), Type.REALM.intValue()));
        predicates.add(root.get("id").in(idsList));
        predicates.addAll(AdminPermissionsSchema.SCHEMA.applyAuthorizationFilters(session, AdminPermissionsSchema.GROUPS, realm, builder, queryBuilder, root));

        queryBuilder.where(predicates.toArray(new Predicate[0]));
        queryBuilder.orderBy(builder.asc(root.get("name")));

        return closing(em.createQuery(queryBuilder).getResultStream())
                .map(g -> session.groups().getGroupById(realm, g))
                .filter(Objects::nonNull);
    }

    @Override
    public Stream<GroupModel> getGroupsStream(RealmModel realm, Stream<String> ids) {
        return ids.map(id -> session.groups().getGroupById(realm, id)).filter(Objects::nonNull).sorted(GroupModel.COMPARE_BY_NAME);
    }

    @Override
    public Long getGroupsCount(RealmModel realm, Stream<String> ids, String search) {
        CriteriaBuilder builder = em.getCriteriaBuilder();
        CriteriaQuery<Long> queryBuilder = builder.createQuery(Long.class);
        Root<GroupEntity> root = queryBuilder.from(GroupEntity.class);

        queryBuilder.select(builder.count(root.get("id")));

        List<Predicate> predicates = new ArrayList<>();

        predicates.add(builder.equal(root.get("realm"), realm.getId()));
        predicates.add(builder.equal(root.get("type"), Type.REALM.intValue()));

        if (search != null && !search.isEmpty()) {
            predicates.add(builder.like(builder.lower(root.get("name")), builder.lower(builder.literal("%" + search + "%"))));
        }

        predicates.add(root.get("id").in(ids.toList()));
        predicates.addAll(AdminPermissionsSchema.SCHEMA.applyAuthorizationFilters(session, AdminPermissionsSchema.GROUPS, realm, builder, queryBuilder, root));

        queryBuilder.where(predicates.toArray(new Predicate[0]));
        queryBuilder.orderBy(builder.asc(root.get("name")));

        return em.createQuery(queryBuilder).getSingleResult();
    }

    @Override
    public Long getGroupsCount(RealmModel realm, Boolean onlyTopGroups) {
        CriteriaBuilder builder = em.getCriteriaBuilder();
        CriteriaQuery<Long> queryBuilder = builder.createQuery(Long.class);
        Root<GroupEntity> root = queryBuilder.from(GroupEntity.class);

        queryBuilder.select(builder.count(root.get("id")));

        List<Predicate> predicates = new ArrayList<>();

        predicates.add(builder.equal(root.get("realm"), realm.getId()));
        predicates.add(builder.equal(root.get("type"), Type.REALM.intValue()));

        if (Objects.equals(onlyTopGroups, Boolean.TRUE)) {
            predicates.add(builder.equal(root.get("parentId"), GroupEntity.TOP_PARENT_ID));
        }

        predicates.addAll(AdminPermissionsSchema.SCHEMA.applyAuthorizationFilters(session, AdminPermissionsSchema.GROUPS, realm, builder, queryBuilder, root));

        queryBuilder.where(predicates.toArray(new Predicate[0]));

        return em.createQuery(queryBuilder).getSingleResult();
    }

    @Override
    public long getClientsCount(RealmModel realm) {
        CriteriaBuilder builder = em.getCriteriaBuilder();
        CriteriaQuery<Long> queryBuilder = builder.createQuery(Long.class);
        Root<ClientEntity> root = queryBuilder.from(ClientEntity.class);

        queryBuilder.select(builder.count(root.get("id")));

        List<Predicate> predicates = new ArrayList<>();

        predicates.add(builder.equal(root.get("realmId"), realm.getId()));

        predicates.addAll(AdminPermissionsSchema.SCHEMA.applyAuthorizationFilters(session, AdminPermissionsSchema.CLIENTS, realm, builder, queryBuilder, root));

        queryBuilder.where(predicates.toArray(new Predicate[0]));

        return em.createQuery(queryBuilder).getSingleResult();
    }

    @Override
    public Long getGroupsCountByNameContaining(RealmModel realm, String search) {
        return searchForGroupByNameStream(realm, search, false, null, null).count();
    }

    @Override
    public Stream<GroupModel> getGroupsByRoleStream(RealmModel realm, RoleModel role, Integer firstResult, Integer maxResults) {
        TypedQuery<GroupEntity> query = em.createNamedQuery("groupsInRole", GroupEntity.class);
        query.setParameter("roleId", role.getId());

        Stream<GroupEntity> results = paginateQuery(query, firstResult, maxResults).getResultStream();

        return closing(results
                .map(g -> (GroupModel) new GroupAdapter(session, realm, em, g))
                .sorted(GroupModel.COMPARE_BY_NAME));
    }

    @Override
    public Stream<GroupModel> getTopLevelGroupsStream(RealmModel realm, String search, Boolean exact, Integer firstResult, Integer maxResults) {
        CriteriaBuilder builder = em.getCriteriaBuilder();
        CriteriaQuery<String> queryBuilder = builder.createQuery(String.class);
        Root<GroupEntity> root = queryBuilder.from(GroupEntity.class);

        queryBuilder.select(root.get("id"));

        List<Predicate> predicates = new ArrayList<>();

        predicates.add(builder.equal(root.get("realm"), realm.getId()));
        predicates.add(builder.equal(root.get("type"), Type.REALM.intValue()));
        predicates.add(builder.equal(root.get("parentId"), GroupEntity.TOP_PARENT_ID));

        if (Boolean.TRUE.equals(exact)) {
            predicates.add(builder.like(root.get("name"), search));
        } else {
            predicates.add(builder.like(builder.lower(root.get("name")), builder.lower(builder.literal("%" + search + "%"))));
        }

        predicates.addAll(AdminPermissionsSchema.SCHEMA.applyAuthorizationFilters(session, AdminPermissionsSchema.GROUPS, realm, builder, queryBuilder, root));

        queryBuilder.where(predicates.toArray(new Predicate[0]));
        queryBuilder.orderBy(builder.asc(root.get("name")));

        return closing(paginateQuery(em.createQuery(queryBuilder), firstResult, maxResults).getResultStream()
            .map(realm::getGroupById)
            // In concurrent tests, the group might be deleted in another thread, therefore, skip those null values.
            .filter(Objects::nonNull)
            .sorted(GroupModel.COMPARE_BY_NAME)
        );
    }

    @Override
    public boolean removeGroup(RealmModel realm, GroupModel group) {
        if (group == null) {
            return false;
        }

        GroupModel.GroupRemovedEvent event = new GroupModel.GroupRemovedEvent() {
            @Override
            public RealmModel getRealm() {
                return realm;
            }

            @Override
            public GroupModel getGroup() {
                return group;
            }

            @Override
            public KeycloakSession getKeycloakSession() {
                return session;
            }
        };
        session.getKeycloakSessionFactory().publish(event);

        session.users().preRemove(realm, group);

        realm.removeDefaultGroup(group);

        group.getSubGroupsStream().forEach(realm::removeGroup);

        GroupEntity groupEntity = em.find(GroupEntity.class, group.getId(), LockModeType.PESSIMISTIC_WRITE);
        if ((groupEntity == null) || (!groupEntity.getRealm().equals(realm.getId()))) {
            return false;
        }
        em.createNamedQuery("deleteGroupRoleMappingsByGroup").setParameter("group", groupEntity).executeUpdate();

        em.remove(groupEntity);
        return true;


    }

    @Override
    public GroupModel createGroup(RealmModel realm, String id, Type type, String name, GroupModel toParent) {
        if (id == null) {
            id = KeycloakModelUtils.generateId();
        } else if (GroupEntity.TOP_PARENT_ID.equals(id)) {
            // maybe it's impossible but better ensure this doesn't happen
            throw new ModelException("The ID of the new group is equals to the tag used for top level groups");
        }
        GroupEntity groupEntity = new GroupEntity();
        groupEntity.setId(id);
        groupEntity.setName(name);
        groupEntity.setRealm(realm.getId());
        groupEntity.setParentId(toParent == null? GroupEntity.TOP_PARENT_ID : toParent.getId());
        groupEntity.setType(type == null ? Type.REALM.intValue() : type.intValue());
        em.persist(groupEntity);
        em.flush();

        GroupAdapter group = new GroupAdapter(session, realm, em, groupEntity);

        fireGroupCreatedEvent(group);

        return group;
    }

    @Override
    public void addTopLevelGroup(RealmModel realm, GroupModel subGroup) {
        subGroup.setParent(null);
        fireGroupUpdatedEvent(subGroup);
    }

    public void preRemove(RealmModel realm, RoleModel role) {
        // GroupProvider method implementation starts here
        em.createNamedQuery("deleteGroupRoleMappingsByRole").setParameter("roleId", role.getId()).executeUpdate();
        // GroupProvider method implementation ends here

        // ClientProvider implementation
        String clientScopeMapping = JpaUtils.getTableNameForNativeQuery("SCOPE_MAPPING", em);
        em.createNativeQuery("delete from " + clientScopeMapping + " where ROLE_ID = :role").setParameter("role", role.getId()).executeUpdate();
    }

    @Override
    public void preRemove(RealmModel realm) {
        em.createNamedQuery("deleteGroupRoleMappingsByRealm")
                .setParameter("realm", realm.getId()).executeUpdate();
        em.createNamedQuery("deleteGroupAttributesByRealm")
                .setParameter("realm", realm.getId()).executeUpdate();
        em.createNamedQuery("deleteGroupsByRealm")
                .setParameter("realm", realm.getId()).executeUpdate();
    }

    @Override
    public ClientModel addClient(RealmModel realm, String clientId) {
        return addClient(realm, KeycloakModelUtils.generateId(), clientId);
    }

    @Override
    public ClientModel addClient(RealmModel realm, String id, String clientId) {
        ClientModel resource;

        if (id == null) {
            id = KeycloakModelUtils.generateId();
        } else if (id.length() > ClientEntity.ID_MAX_LENGTH){
            throw new ModelValidationException("Client ID must not exceed 36 characters");
        }

        if (clientId == null) {
            clientId = id;
        }

        logger.tracef("addClient(%s, %s, %s)%s", realm, id, clientId, getShortStackTrace());

        ClientEntity entity = new ClientEntity();
        entity.setId(id);
        entity.setClientId(clientId);
        entity.setEnabled(true);
        entity.setStandardFlowEnabled(true);
        entity.setRealmId(realm.getId());
        em.persist(entity);

        resource = toClientModel(realm, entity);

        session.getKeycloakSessionFactory().publish((ClientModel.ClientCreationEvent) () -> resource);
        return resource;
    }

    @Override
    public Stream<ClientModel> getClientsStream(RealmModel realm) {
        return getClientsStream(realm, null, null);
    }

    @Override
    public Stream<ClientModel> getClientsStream(RealmModel realm, Integer firstResult, Integer maxResults) {
        return closing(getClientIdsStream(realm, firstResult, maxResults).map(id -> new ClientModelLazyDelegate.WithId(session, realm, id)));
    }

    private Stream<String> getClientIdsStream(RealmModel realm, Integer firstResult, Integer maxResults) {
        CriteriaBuilder builder = em.getCriteriaBuilder();
        CriteriaQuery<String> queryBuilder = builder.createQuery(String.class);
        Root<ClientEntity> root = queryBuilder.from(ClientEntity.class);
        queryBuilder.select(root.get("id"));

        List<Predicate> predicates = new ArrayList<>();

        predicates.add(builder.equal(root.get("realmId"), realm.getId()));
        predicates.addAll(AdminPermissionsSchema.SCHEMA.applyAuthorizationFilters(session, AdminPermissionsSchema.CLIENTS, realm, builder, queryBuilder, root));

        Predicate finalPredicate = builder.and(predicates.toArray(new Predicate[0]));
        queryBuilder.where(finalPredicate).orderBy(builder.asc(root.get("clientId")));

        return paginateQuery(em.createQuery(queryBuilder), firstResult, maxResults).getResultStream();
    }

    @Override
    public Stream<ClientModel> getAlwaysDisplayInConsoleClientsStream(RealmModel realm) {
        TypedQuery<String> query = em.createNamedQuery("getAlwaysDisplayInConsoleClients", String.class);
        query.setParameter("realm", realm.getId());
        Stream<String> clientStream = query.getResultStream();

        return closing(clientStream.map(c -> session.clients().getClientById(realm, c)).filter(Objects::nonNull));
    }

    @Override
    public ClientModel getClientById(RealmModel realm, String id) {
        logger.tracef("getClientById(%s, %s)%s", realm, id, getShortStackTrace());

        ClientEntity client = em.find(ClientEntity.class, id);
        // Check if client belongs to this realm
        if (client == null || !realm.getId().equals(client.getRealmId())) return null;
        return toClientModel(realm, client);
    }

    private ClientModel toClientModel(RealmModel realm, ClientEntity client) {
        ClientAdapter adapter = new ClientAdapter(realm, em, session, client);

        if (Profile.isFeatureEnabled(Profile.Feature.CLIENT_TYPES)) {
            ClientTypeManager mgr = session.getProvider(ClientTypeManager.class);
            return mgr.augmentClient(adapter);
        } else {
            return adapter;
        }
    }

    @Override
    public ClientModel getClientByClientId(RealmModel realm, String clientId) {
        logger.tracef("getClientByClientId(%s, %s)%s", realm, clientId, getShortStackTrace());

        TypedQuery<String> query = em.createNamedQuery("findClientIdByClientId", String.class);
        query.setParameter("clientId", clientId);
        query.setParameter("realm", realm.getId());
        List<String> results = query.getResultList();
        if (results.isEmpty()) return null;
        String id = results.get(0);
        return session.clients().getClientById(realm, id);
    }

    @Override
    public Stream<ClientModel> searchClientsByClientIdStream(RealmModel realm, String clientId, Integer firstResult, Integer maxResults) {
        CriteriaBuilder builder = em.getCriteriaBuilder();
        CriteriaQuery<String> queryBuilder = builder.createQuery(String.class);
        Root<ClientEntity> root = queryBuilder.from(ClientEntity.class);
        queryBuilder.select(root.get("id"));

        List<Predicate> predicates = new ArrayList<>();

        predicates.add(builder.equal(root.get("realmId"), realm.getId()));
        predicates.add(builder.like(builder.lower(root.get("clientId")), builder.lower(builder.literal("%" + clientId + "%"))));
        predicates.addAll(AdminPermissionsSchema.SCHEMA.applyAuthorizationFilters(session, AdminPermissionsSchema.CLIENTS, realm, builder, queryBuilder, root));

        Predicate finalPredicate = builder.and(predicates.toArray(new Predicate[0]));
        queryBuilder.where(finalPredicate).orderBy(builder.asc(root.get("clientId")));

        Stream<String> results = paginateQuery(em.createQuery(queryBuilder), firstResult, maxResults).getResultStream();
        return closing(results.map(id -> new ClientModelLazyDelegate.WithId(session, realm, id)));
    }

    @Override
    public Stream<ClientModel> searchClientsByAttributes(RealmModel realm, Map<String, String> attributes, Integer firstResult, Integer maxResults) {
        Map<String, String> filteredAttributes = attributes;
        if (clientSearchableAttributes != null) {
            Set<String> notAllowed = attributes.keySet().stream().filter(attr -> !clientSearchableAttributes.contains(attr)).collect(Collectors.toSet());
            if (!notAllowed.isEmpty()) {
                throw new ModelException("Attributes [" + String.join(", ", notAllowed) + "] not allowed for search");
            }
            filteredAttributes = attributes.entrySet().stream().filter(e -> clientSearchableAttributes.contains(e.getKey())).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        }

        CriteriaBuilder builder = em.getCriteriaBuilder();
        CriteriaQuery<String> queryBuilder = builder.createQuery(String.class);
        Root<ClientEntity> root = queryBuilder.from(ClientEntity.class);
        queryBuilder.select(root.get("id"));

        List<Predicate> predicates = new ArrayList<>();

        predicates.add(builder.equal(root.get("realmId"), realm.getId()));

        //noinspection resource
        String dbProductName = em.unwrap(Session.class).doReturningWork(connection -> connection.getMetaData().getDatabaseProductName());

        for (Map.Entry<String, String> entry : filteredAttributes.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();

            Join<ClientEntity, ClientAttributeEntity> attributeJoin = root.join("attributes");

            Predicate attrNamePredicate = builder.equal(attributeJoin.get("name"), key);

            if (dbProductName.equals("Oracle")) {
                // SELECT * FROM client_attributes WHERE ... DBMS_LOB.COMPARE(value, '0') = 0 ...;
                // Oracle is not able to compare a CLOB with a VARCHAR unless it being converted with TO_CHAR
                // But for this all values in the table need to be smaller than 4K, otherwise the cast will fail with
                // "ORA-22835: Buffer too small for CLOB to CHAR" (even if it is in another row).
                // This leaves DBMS_LOB.COMPARE as the option to compare the CLOB with the value.
                Predicate attrValuePredicate = builder.equal(builder.function("DBMS_LOB.COMPARE", Integer.class, attributeJoin.get("value"), builder.literal(value)), 0);
                predicates.add(builder.and(attrNamePredicate, attrValuePredicate));
            } else if (dbProductName.equals("PostgreSQL")) {
                // use the substr comparison and the full comparison in postgresql
                Predicate attrValuePredicate1 = builder.equal(
                        builder.function("substr", Integer.class, attributeJoin.get("value"), builder.literal(1), builder.literal(255)),
                        builder.function("substr", Integer.class, builder.literal(value), builder.literal(1), builder.literal(255)));
                Predicate attrValuePredicate2 =  builder.equal(attributeJoin.get("value"), value);
                predicates.add(builder.and(attrNamePredicate, attrValuePredicate1, attrValuePredicate2));
            } else {
                Predicate attrValuePredicate = builder.equal(attributeJoin.get("value"), value);
                predicates.add(builder.and(attrNamePredicate, attrValuePredicate));
            }
        }

        predicates.addAll(AdminPermissionsSchema.SCHEMA.applyAuthorizationFilters(session, AdminPermissionsSchema.CLIENTS, realm, builder, queryBuilder, root));

        Predicate finalPredicate = builder.and(predicates.toArray(new Predicate[0]));
        queryBuilder.where(finalPredicate).orderBy(builder.asc(root.get("clientId")));

        TypedQuery<String> query = em.createQuery(queryBuilder);
        return closing(paginateQuery(query, firstResult, maxResults).getResultStream())
                .map(id -> session.clients().getClientById(realm, id)).filter(Objects::nonNull);
    }

    @Override
    public Stream<ClientModel> searchClientsByAuthenticationFlowBindingOverrides(RealmModel realm, Map<String, String> overrides, Integer firstResult, Integer maxResults) {
        CriteriaBuilder builder = em.getCriteriaBuilder();
        CriteriaQuery<String> queryBuilder = builder.createQuery(String.class);
        Root<ClientEntity> root = queryBuilder.from(ClientEntity.class);
        queryBuilder.select(root.get("id"));

        List<Predicate> predicates = new ArrayList<>();

        predicates.add(builder.equal(root.get("realmId"), realm.getId()));

        //noinspection resource
        String dbProductName = em.unwrap(Session.class).doReturningWork(connection -> connection.getMetaData().getDatabaseProductName());

        for (Map.Entry<String, String> entry : overrides.entrySet()) {
            String bindingName = entry.getKey();
            String authenticationFlowId = entry.getValue();

            MapJoin<ClientEntity, String, String> authFlowBindings = root.joinMap("authFlowBindings", JoinType.LEFT);

            Predicate attrNamePredicate = builder.equal(authFlowBindings.key(), bindingName);

            Predicate attrValuePredicate;
            if (dbProductName.equals("Oracle")) {
                // SELECT * FROM client_attributes WHERE ... DBMS_LOB.COMPARE(value, '0') = 0 ...;
                // Oracle is not able to compare a CLOB with a VARCHAR unless it being converted with TO_CHAR
                // But for this all values in the table need to be smaller than 4K, otherwise the cast will fail with
                // "ORA-22835: Buffer too small for CLOB to CHAR" (even if it is in another row).
                // This leaves DBMS_LOB.COMPARE as the option to compare the CLOB with the value.
                attrValuePredicate = builder.equal(builder.function("DBMS_LOB.COMPARE", Integer.class, authFlowBindings.value(), builder.literal(authenticationFlowId)), 0);
            } else {
                attrValuePredicate = builder.equal(authFlowBindings.value(), authenticationFlowId);
            }

            predicates.add(builder.and(attrNamePredicate, attrValuePredicate));
        }

        Predicate finalPredicate = builder.and(predicates.toArray(new Predicate[0]));
        queryBuilder.where(finalPredicate).orderBy(builder.asc(root.get("clientId")));

        TypedQuery<String> query = em.createQuery(queryBuilder);
        return closing(paginateQuery(query, firstResult, maxResults).getResultStream())
                .map(id -> session.clients().getClientById(realm, id)).filter(Objects::nonNull);
    }

    @Override
    public void removeClients(RealmModel realm) {
        closing(getClientIdsStream(realm, -1, -1)).forEach((id) -> removeClient(realm, id));
    }

    @Override
    public boolean removeClient(RealmModel realm, String id) {

        logger.tracef("removeClient(%s, %s)%s", realm, id, getShortStackTrace());

        final ClientModel client = getClientById(realm, id);
        if (client == null) return false;

        session.users().preRemove(realm, client);
        session.roles().removeRoles(client);

        ClientEntity clientEntity = em.find(ClientEntity.class, id, LockModeType.PESSIMISTIC_WRITE);

        session.getKeycloakSessionFactory().publish(new ClientModel.ClientRemovedEvent() {
            @Override
            public ClientModel getClient() {
                return client;
            }

            @Override
            public KeycloakSession getKeycloakSession() {
                return session;
            }
        });

        int countRemoved = em.createNamedQuery("deleteClientScopeClientMappingByClient")
                .setParameter("clientId", clientEntity.getId())
                .executeUpdate();
        em.remove(clientEntity);  // i have no idea why, but this needs to come before deleteScopeMapping

        try {
            em.flush();
        } catch (RuntimeException e) {
            logger.errorv("Unable to delete client entity: {0} from realm {1}", client.getClientId(), realm.getName());
            throw e;
        }

        return true;
    }

    @Override
    public ClientScopeModel getClientScopeById(RealmModel realm, String id) {
        ClientScopeEntity clientScope = em.find(ClientScopeEntity.class, id);

        // Check if client scope belongs to this realm
        if (clientScope == null || !realm.getId().equals(clientScope.getRealmId())) return null;
        ClientScopeAdapter adapter = new ClientScopeAdapter(realm, em, session, clientScope);
        return adapter;
    }

    @Override
    public Stream<ClientScopeModel> getClientScopesStream(RealmModel realm) {
        TypedQuery<String> query = em.createNamedQuery("getClientScopeIds", String.class);
        query.setParameter("realm", realm.getId());
        Stream<String> scopes = query.getResultStream();

        return closing(scopes.map(realm::getClientScopeById).filter(Objects::nonNull));
    }

    @Override
    public ClientScopeModel addClientScope(RealmModel realm, String id, String name) {
        if (id == null) {
            id = KeycloakModelUtils.generateId();
        }
        ClientScopeEntity entity = new ClientScopeEntity();
        entity.setId(id);
        name = KeycloakModelUtils.convertClientScopeName(name);
        entity.setName(name);
        entity.setRealmId(realm.getId());
        em.persist(entity);

        ClientScopeModel clientScope = new ClientScopeAdapter(realm, em, session, entity);
        session.getKeycloakSessionFactory().publish(new ClientScopeModel.ClientScopeCreatedEvent() {

            @Override
            public KeycloakSession getKeycloakSession() {
                return session;
            }

            @Override
            public ClientScopeModel getClientScope() {
                return clientScope;
            }
        });

        em.flush();
        return clientScope;
    }

    @Override
    public boolean removeClientScope(RealmModel realm, String id) {
        if (id == null) return false;
        ClientScopeModel clientScope = getClientScopeById(realm, id);
        if (clientScope == null) return false;

        session.users().preRemove(clientScope);
        realm.removeDefaultClientScope(clientScope);
        ClientScopeEntity clientScopeEntity = em.find(ClientScopeEntity.class, id, LockModeType.PESSIMISTIC_WRITE);

        em.createNamedQuery("deleteClientScopeClientMappingByClientScope").setParameter("clientScopeId", clientScope.getId()).executeUpdate();
        em.createNamedQuery("deleteClientScopeRoleMappingByClientScope").setParameter("clientScope", clientScopeEntity).executeUpdate();
        em.remove(clientScopeEntity);

        session.getKeycloakSessionFactory().publish(new ClientScopeModel.ClientScopeRemovedEvent() {

            @Override
            public KeycloakSession getKeycloakSession() {
                return session;
            }

            @Override
            public ClientScopeModel getClientScope() {
                return clientScope;
            }
        });

        em.flush();
        return true;
    }

    @Override
    public void removeClientScopes(RealmModel realm) {
        // No need to go through cache. Client scopes were already invalidated
        realm.getClientScopesStream().map(ClientScopeModel::getId).forEach(id -> this.removeClientScope(realm, id));
    }

    @Override
    public Stream<ClientScopeModel> getClientScopesByProtocol(RealmModel realm, String protocol)
    {
        TypedQuery<ClientScopeEntity> query = em.createNamedQuery("getClientScopesByProtocol",
                                                                  ClientScopeEntity.class)
                                                .setParameter("realm", realm.getId())
                                                .setParameter("protocol", protocol);
        return query.getResultStream()
                    .map(entity -> new ClientScopeAdapter(realm, em, session, entity));
    }

    /**
     * This method filters clientScopes by specific attributes. To do this, it will generate the sql-statement
     * dynamically based on the given search-parameters.<br />
     * This method prevents SQL-Injections by adding dynamic parameters into the SQL-statement and resolves them
     * later by using the JPA query function {@code query.setParameter(dynamicParamName, actualValue)}.<br/>
     * Here is an example of a generated statement:
     * <pre>
     *     {@code
     *     SELECT distinct C FROM ClientScopeEntity C
     *     inner join ClientScopeAttributeEntity CA0 on C.id = CA0.clientScope.id
     *                                              and CA0.name = :a3e8d01932c104f0ab79441d34884bada
     *     WHERE C.realmId = :realmId
     *     and CA0.value = :acedd0bedc7264a2fb524a37814f7aaa1
     *     }
     * </pre>
     *
     * @param realm     Realm.
     * @param searchMap a key-value map that holds the attribute names and values to search for.
     * @param useOr     If the search-params should be combined with or-expressions or and-expressions
     * @return a stream of clientScopes matching the given criteria
     */
    @Override
    public Stream<ClientScopeModel> getClientScopesByAttributes(RealmModel realm, Map<String, String> searchMap,
                                                                boolean useOr) {
        // we build this specific query dynamically, but we enter the parameters as keys to avoid SQL injections.
        StringBuilder jpql = new StringBuilder("SELECT distinct C FROM ClientScopeEntity C");
        List<String> keys = new ArrayList<>(searchMap.keySet());
        Map<String, String> dynamicParameterNameMap = new HashMap<>();
        Map<String, String> dynamicParameterValueMap = new HashMap<>();
        StringBuilder whereClauseExtension = new StringBuilder();
        // I am using an indexed for-loop because I need the index for dynamic jpql references
        for (int i = 0; i < keys.size(); i++) {
            String key = keys.get(i);
            String value = searchMap.get(key);
            Supplier<String> generateDynamicParameterName = () -> {
                return "a" /* dynamic params must start with a letter */
                        + UUID.randomUUID().toString().replaceAll("-","");
            };
            final String dynamicParameterName = generateDynamicParameterName.get();
            final String dynamicParameterValue = generateDynamicParameterName.get();
            dynamicParameterNameMap.put(dynamicParameterName, key);
            dynamicParameterValueMap.put(dynamicParameterValue, value);
            jpql.append('\n')
                .append("""
                                inner join ClientScopeAttributeEntity CA%1$s on C.id = CA%1$s.clientScope.id
                                                                             and CA%1$s.name = :%2$s
                                """.stripIndent().strip().formatted(i, dynamicParameterName));
            whereClauseExtension.append('\n');
            if (useOr) {
                whereClauseExtension.append("or");
            }else {
                whereClauseExtension.append("and");
            }
            whereClauseExtension.append(" CA%1$s.value = :%2$s".formatted(i, dynamicParameterValue));
        }

        jpql.append('\n').append(" WHERE C.realmId = :realmId").append(whereClauseExtension);
        logger.debugf("Filter for clientScopes with query:\n%s", jpql);
        TypedQuery<ClientScopeEntity> query = em.createQuery(jpql.toString(), ClientScopeEntity.class);
        dynamicParameterNameMap.forEach(query::setParameter);
        dynamicParameterValueMap.forEach(query::setParameter);
        return query.setParameter("realmId", realm.getId())
                    .getResultStream().map(scope -> new ClientScopeAdapter(realm, em, session, scope));
    }

    @Override
    public void addClientScopes(RealmModel realm, ClientModel client, Set<ClientScopeModel> clientScopes, boolean defaultScope) {
        List<String> acceptedClientProtocols = KeycloakModelUtils.getAcceptedClientScopeProtocols(client);

        Map<String, ClientScopeModel> existingClientScopes = getClientScopes(realm, client, true);
        existingClientScopes.putAll(getClientScopes(realm, client, false));

        Set<ClientScopeClientMappingEntity> clientScopeEntities = clientScopes.stream()
                .filter(clientScope -> !existingClientScopes.containsKey(clientScope.getName()))
                .filter(clientScope -> {
                    if (clientScope.getProtocol() == null) {
                        // set default protocol if not set. Otherwise, we will get a NullPointer
                        clientScope.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
                    }
                    return acceptedClientProtocols.contains(clientScope.getProtocol());
                })
                .map(clientScope -> {
                    ClientScopeClientMappingEntity entity = new ClientScopeClientMappingEntity();
                    entity.setClientScopeId(clientScope.getId());
                    entity.setClientId(client.getId());
                    entity.setDefaultScope(defaultScope);
                    em.persist(entity);
                    return entity;
                }).collect(Collectors.toSet());
        if (!clientScopeEntities.isEmpty()) {
            em.flush();
            clientScopeEntities.forEach(entity -> em.detach(entity));
        }
    }

    @Override
    public void removeClientScope(RealmModel realm, ClientModel client, ClientScopeModel clientScope) {
        em.createNamedQuery("deleteClientScopeClientMapping")
                .setParameter("clientScopeId", clientScope.getId())
                .setParameter("clientId", client.getId())
                .executeUpdate();
        em.flush();
    }

    @Override
    public void addClientScopeToAllClients(RealmModel realm, ClientScopeModel clientScope, boolean defaultClientScope) {
        if (realm.equals(clientScope.getRealm())) {
            em.createNamedQuery("addClientScopeToAllClients")
                    .setParameter("realmId", realm.getId())
                    .setParameter("clientScopeId", clientScope.getId())
                    .setParameter("clientProtocol", clientScope.getProtocol())
                    .setParameter("defaultScope", defaultClientScope)
                    .executeUpdate();
        }
    }

    @Override
    public Map<String, ClientScopeModel> getClientScopes(RealmModel realm, ClientModel client, boolean defaultScope) {
        List<String> acceptedClientProtocols = KeycloakModelUtils.getAcceptedClientScopeProtocols(client);

        TypedQuery<String> query = em.createNamedQuery("clientScopeClientMappingIdsByClient", String.class);
        query.setParameter("clientId", client.getId());
        query.setParameter("defaultScope", defaultScope);

        return closing(query.getResultStream())
                .map(clientScopeId -> session.clientScopes().getClientScopeById(realm, clientScopeId))
                .filter(Objects::nonNull)
                .filter(clientScope -> acceptedClientProtocols.contains(clientScope.getProtocol()))
                .collect(Collectors.toMap(ClientScopeModel::getName, Function.identity()));
    }
    @Override
    public Stream<GroupModel> searchForGroupByNameStream(RealmModel realm, String search, Boolean exact, Integer first, Integer max) {
        CriteriaBuilder builder = em.getCriteriaBuilder();
        CriteriaQuery<String> queryBuilder = builder.createQuery(String.class);
        Root<GroupEntity> root = queryBuilder.from(GroupEntity.class);

        queryBuilder.select(root.get("id"));

        List<Predicate> predicates = new ArrayList<>();

        predicates.add(builder.equal(root.get("realm"), realm.getId()));
        predicates.add(builder.equal(root.get("type"), Type.REALM.intValue()));

        if (Boolean.TRUE.equals(exact)) {
            predicates.add(builder.equal(root.get("name"), search));
        } else {
            predicates.add(builder.like(builder.lower(root.get("name")), builder.lower(builder.literal("%" + search + "%"))));
        }

        predicates.addAll(AdminPermissionsSchema.SCHEMA.applyAuthorizationFilters(session, AdminPermissionsSchema.GROUPS, realm, builder, queryBuilder, root));

        queryBuilder.where(predicates.toArray(new Predicate[0]));
        queryBuilder.orderBy(builder.asc(root.get("name")));

        return closing(paginateQuery(em.createQuery(queryBuilder), first, max).getResultStream()
                .map(id -> session.groups().getGroupById(realm, id))
                .filter(Objects::nonNull)
                .sorted(GroupModel.COMPARE_BY_NAME)
                .distinct());
    }

    @Override
    public Stream<GroupModel> searchGroupsByAttributes(RealmModel realm, Map<String, String> attributes, Integer firstResult, Integer maxResults) {
        Map<String, String> filteredAttributes = groupSearchableAttributes == null || groupSearchableAttributes.isEmpty()
                ? attributes
                : attributes.entrySet().stream().filter(m -> groupSearchableAttributes.contains(m.getKey()))
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        CriteriaBuilder builder = em.getCriteriaBuilder();
        CriteriaQuery<GroupEntity> queryBuilder = builder.createQuery(GroupEntity.class);
        Root<GroupEntity> root = queryBuilder.from(GroupEntity.class);

        List<Predicate> predicates = new ArrayList<>();

        predicates.add(builder.equal(root.get("realm"), realm.getId()));
        predicates.add(builder.equal(root.get("type"), Type.REALM.intValue()));

        for (Map.Entry<String, String> entry : filteredAttributes.entrySet()) {
            String key = entry.getKey();
            if (key == null || key.isEmpty()) {
                continue;
            }
            String value = entry.getValue();

            Join<GroupEntity, GroupAttributeEntity> attributeJoin = root.join("attributes");

            Predicate attrNamePredicate = builder.equal(attributeJoin.get("name"), key);
            Predicate attrValuePredicate = builder.equal(attributeJoin.get("value"), value);
            predicates.add(builder.and(attrNamePredicate, attrValuePredicate));
        }

        predicates.addAll(AdminPermissionsSchema.SCHEMA.applyAuthorizationFilters(session, AdminPermissionsSchema.GROUPS, realm, builder, queryBuilder, root));

        Predicate finalPredicate = builder.and(predicates.toArray(new Predicate[0]));
        queryBuilder.where(finalPredicate).orderBy(builder.asc(root.get("name")));

        TypedQuery<GroupEntity> query = em.createQuery(queryBuilder);
        return closing(paginateQuery(query, firstResult, maxResults).getResultStream())
                .map(g -> new GroupAdapter(session, realm, em, g));
    }

    @Override
    public void removeExpiredClientInitialAccess() {
        int currentTime = Time.currentTime();

        em.createNamedQuery("removeExpiredClientInitialAccess")
                .setParameter("currentTime", currentTime)
                .executeUpdate();
    }

    private RealmLocalizationTextsEntity getRealmLocalizationTextsEntity(String locale, String realmId) {
        RealmLocalizationTextsEntity.RealmLocalizationTextEntityKey key = new RealmLocalizationTextsEntity.RealmLocalizationTextEntityKey();
        key.setRealm(em.getReference(RealmEntity.class, realmId));
        key.setLocale(locale);
        return em.find(RealmLocalizationTextsEntity.class, key);
    }

    @Override
    public boolean updateLocalizationText(RealmModel realm, String locale, String key, String text) {
        RealmLocalizationTextsEntity entity = getRealmLocalizationTextsEntity(locale, realm.getId());
        if (entity != null && entity.getTexts() != null && entity.getTexts().containsKey(key)) {
            entity.getTexts().put(key, text);

            em.persist(entity);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void saveLocalizationText(RealmModel realm, String locale, String key, String text) {
        RealmLocalizationTextsEntity entity = getRealmLocalizationTextsEntity(locale, realm.getId());
        if(entity == null) {
            entity = new RealmLocalizationTextsEntity();
            entity.setRealm(em.getReference(RealmEntity.class, realm.getId()));
            entity.setLocale(locale);
            entity.setTexts(new HashMap<>());
        }
        entity.getTexts().put(key, text);
        em.persist(entity);
    }

    @Override
    public void saveLocalizationTexts(RealmModel realm, String locale, Map<String, String> localizationTexts) {
        RealmLocalizationTextsEntity entity = new RealmLocalizationTextsEntity();
        entity.setTexts(localizationTexts);
        entity.setLocale(locale);
        entity.setRealm(em.getReference(RealmEntity.class, realm.getId()));
        em.merge(entity);
    }

    @Override
    public boolean deleteLocalizationTextsByLocale(RealmModel realm, String locale) {
        CriteriaBuilder builder = em.getCriteriaBuilder();
        CriteriaDelete<RealmLocalizationTextsEntity> criteriaDelete =
                builder.createCriteriaDelete(RealmLocalizationTextsEntity.class);
        Root<RealmLocalizationTextsEntity> root = criteriaDelete.from(RealmLocalizationTextsEntity.class);

        criteriaDelete.where(builder.and(
                builder.equal(root.get("realmId"), realm.getId()),
                builder.equal(root.get("locale"), locale)));
        int linesUpdated = em.createQuery(criteriaDelete).executeUpdate();
        return linesUpdated == 1?true:false;
    }

    @Override
    public String getLocalizationTextsById(RealmModel realm, String locale, String key) {
        RealmLocalizationTextsEntity entity = getRealmLocalizationTextsEntity(locale, realm.getId());
        if (entity != null && entity.getTexts() != null && entity.getTexts().containsKey(key)) {
            return entity.getTexts().get(key);
        }
        return null;
    }

    @Override
    public boolean deleteLocalizationText(RealmModel realm, String locale, String key) {
        RealmLocalizationTextsEntity entity = getRealmLocalizationTextsEntity(locale, realm.getId());
        if (entity != null && entity.getTexts() != null && entity.getTexts().containsKey(key)) {
            entity.getTexts().remove(key);

            em.persist(entity);
            return true;
        } else {
            return false;
        }
    }

    public Set<String> getClientSearchableAttributes() {
        return clientSearchableAttributes;
    }

    private void fireGroupUpdatedEvent(GroupModel group) {
        GroupUpdatedEvent.fire(group, session);
    }

    private void fireGroupCreatedEvent(GroupAdapter group) {
        GroupCreatedEvent.fire(group, session);
    }
}
