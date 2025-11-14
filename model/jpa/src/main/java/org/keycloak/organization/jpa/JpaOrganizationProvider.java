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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.From;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.models.GroupModel;
import org.keycloak.models.GroupModel.Type;
import org.keycloak.models.GroupProvider;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.MembershipMetadata;
import org.keycloak.models.ModelDuplicateException;
import org.keycloak.models.ModelException;
import org.keycloak.models.ModelValidationException;
import org.keycloak.models.OrganizationModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserProvider;
import org.keycloak.models.jpa.entities.GroupAttributeEntity;
import org.keycloak.models.jpa.entities.GroupEntity;
import org.keycloak.models.jpa.entities.OrganizationDomainEntity;
import org.keycloak.models.jpa.entities.OrganizationEntity;
import org.keycloak.models.jpa.entities.UserEntity;
import org.keycloak.models.jpa.entities.UserGroupMembershipEntity;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.models.utils.ReadOnlyUserModelDelegate;
import org.keycloak.organization.OrganizationProvider;
import org.keycloak.organization.utils.Organizations;
import org.keycloak.representations.idm.MembershipType;
import org.keycloak.storage.StorageId;
import org.keycloak.utils.ReservedCharValidator;
import org.keycloak.utils.StringUtil;

import static org.keycloak.models.OrganizationModel.ORGANIZATION_DOMAIN_ATTRIBUTE;
import static org.keycloak.models.UserModel.EMAIL;
import static org.keycloak.models.UserModel.FIRST_NAME;
import static org.keycloak.models.UserModel.LAST_NAME;
import static org.keycloak.models.UserModel.USERNAME;
import static org.keycloak.models.jpa.PaginationUtils.paginateQuery;
import static org.keycloak.organization.utils.Organizations.isReadOnlyOrganizationMember;
import static org.keycloak.utils.StreamsUtil.closing;

public class JpaOrganizationProvider implements OrganizationProvider {

    private final EntityManager em;
    private final GroupProvider groupProvider;
    private final UserProvider userProvider;
    private final KeycloakSession session;

    public JpaOrganizationProvider(KeycloakSession session) {
        this.session = session;
        em = session.getProvider(JpaConnectionProvider.class).getEntityManager();
        groupProvider = session.groups();
        userProvider = session.users();
    }

    @Override
    public OrganizationModel create(String id, String name, String alias) {
        if (StringUtil.isBlank(name)) {
            throw new ModelValidationException("Name can not be null");
        }

        if (StringUtil.isBlank(alias)) {
            try {
                ReservedCharValidator.validateNoSpace(name);
            } catch (ReservedCharValidator.ReservedCharException e) {
                throw new ModelValidationException("Name contains a reserved character and cannot be used as alias");
            }
            alias = name;
        }

        if (getByName(name) != null) {
            throw new ModelDuplicateException("A organization with the same name already exists.");
        }

        if (getAllStream(Map.of(OrganizationModel.ALIAS, alias), -1, -1).findAny().isPresent()) {
            throw new ModelDuplicateException("A organization with the same alias already exists");
        }

        OrganizationEntity entity = new OrganizationEntity();
        entity.setId(id != null ? id : KeycloakModelUtils.generateId());
        entity.setRealmId(getRealm().getId());
        OrganizationAdapter adapter = new OrganizationAdapter(session, getRealm(), entity, this);

        try {
            session.getContext().setOrganization(adapter);
            GroupModel group = createOrganizationGroup(adapter.getId());

            adapter.setGroupId(group.getId());
            adapter.setName(name);
            adapter.setAlias(alias);
            adapter.setEnabled(true);

            em.persist(adapter.getEntity());
        } finally {
            session.getContext().setOrganization(null);
        }

        return adapter;
    }

    @Override
    public boolean remove(OrganizationModel organization) {
        OrganizationEntity entity = getEntity(organization.getId());

        try {
            session.getContext().setOrganization(organization);
            RealmModel realm = session.realms().getRealm(getRealm().getId());

            // check if the realm is being removed so that we don't need to remove manually remove any other data but the org
            if (realm != null) {
                GroupModel group = getOrganizationGroup(entity);

                if (group != null) {
                    OrganizationProvider provider = session.getProvider(OrganizationProvider.class);
                    //TODO: won't scale, requires a better mechanism for bulk deleting users
                    userProvider.getGroupMembersStream(realm, group).forEach(userModel -> provider.removeMember(organization, userModel));
                    groupProvider.removeGroup(realm, group);
                }

                organization.getIdentityProviders().forEach((model) -> removeIdentityProvider(organization, model));
            }

            em.remove(entity);
        } finally {
            session.getContext().setOrganization(null);
        }

        return true;
    }

    @Override
    public void removeAll() {
        //TODO: won't scale, requires a better mechanism for bulk deleting organizations within a realm
        getAllStream().forEach(this::remove);
    }

    @Override
    public boolean addManagedMember(OrganizationModel organization, UserModel user) {
        return addMember(organization, user, new MembershipMetadata(MembershipType.MANAGED));
    }

    @Override
    public boolean addMember(OrganizationModel organization, UserModel user) {
        return addMember(organization, user, new MembershipMetadata(MembershipType.UNMANAGED));
    }

    private boolean addMember(OrganizationModel organization, UserModel user, MembershipMetadata metadata) {
        throwExceptionIfObjectIsNull(organization, "Organization");
        throwExceptionIfObjectIsNull(user, "User");

        OrganizationEntity entity = getEntity(organization.getId());
        OrganizationModel current = Organizations.resolveOrganization(session);

        // check the user and the organization belongs to the same realm
        if (session.users().getUserById(session.realms().getRealm(entity.getRealmId()), user.getId()) == null) {
            return false;
        }

        if (current == null) {
            session.getContext().setOrganization(organization);
        }

        try {
            GroupModel group = getOrganizationGroup(entity);

            if (user.isMemberOf(group)) {
                return false;
            }

            user.joinGroup(group, metadata);
            OrganizationModel.OrganizationMemberJoinEvent.fire(organization, user, session);
        } finally {
            if (current == null) {
                session.getContext().setOrganization(null);
            }
        }

        return true;
    }

    @Override
    public OrganizationModel getById(String id) {
        OrganizationEntity entity = getEntity(id, false);
        return entity == null ? null : new OrganizationAdapter(session, getRealm(), entity, this);
    }

    @Override
    public OrganizationModel getByDomainName(String domain) {
        TypedQuery<OrganizationEntity> query = em.createNamedQuery("getByDomainName", OrganizationEntity.class);
        RealmModel realm = getRealm();
        query.setParameter("realmId", realm.getId());
        query.setParameter("name", domain.toLowerCase());
        try {
            OrganizationEntity entity = query.getSingleResult();
            return new OrganizationAdapter(session, realm, entity, this);
        } catch (NoResultException nre) {
            return null;
        }
    }

    @Override
    public Stream<OrganizationModel> getAllStream(String search, Boolean exact, Integer first, Integer max) {
        CriteriaBuilder builder = em.getCriteriaBuilder();
        CriteriaQuery<OrganizationEntity> query = builder.createQuery(OrganizationEntity.class);
        Root<OrganizationEntity> org = query.from(OrganizationEntity.class);

        Predicate predicate = buildStringSearchPredicate(builder, query, org, search, exact);

        TypedQuery<OrganizationEntity> typedQuery = buildSearchQuery(builder, query, org, predicate);

        return closing(paginateQuery(typedQuery, first, max).getResultStream()
                .map(entity -> new OrganizationAdapter(session, getRealm(), entity, this)));
    }

    @Override
    public Stream<OrganizationModel> getAllStream(Map<String, String> attributes, Integer first, Integer max) {
        CriteriaBuilder builder = em.getCriteriaBuilder();
        CriteriaQuery<OrganizationEntity> query = builder.createQuery(OrganizationEntity.class);
        Root<OrganizationEntity> org = query.from(OrganizationEntity.class);

        Predicate predicate = buildAttributeSearchPredicate(builder, query, org, attributes);


        TypedQuery<OrganizationEntity> typedQuery = buildSearchQuery(builder, query, org, predicate);
        return closing(paginateQuery(typedQuery, first, max).getResultStream())
                .map(entity -> new OrganizationAdapter(session, getRealm(), entity, this));
    }

    @Override
    public long count(String search, Boolean exact) {
        CriteriaBuilder builder = em.getCriteriaBuilder();
        CriteriaQuery<Long> query = builder.createQuery(Long.class);
        Root<OrganizationEntity> org = query.from(OrganizationEntity.class);

        Predicate predicate = buildStringSearchPredicate(builder, query, org, search, exact);

        TypedQuery<Long> typedQuery = buildCountQuery(builder, query, org, predicate);

        return typedQuery.getSingleResult();
    }

    @Override
    public long count(Map<String, String> attributes) {
        CriteriaBuilder builder = em.getCriteriaBuilder();
        CriteriaQuery<Long> query = builder.createQuery(Long.class);
        Root<OrganizationEntity> org = query.from(OrganizationEntity.class);

        Predicate predicate = buildAttributeSearchPredicate(builder, query, org, attributes);


        TypedQuery<Long> typedQuery = buildCountQuery(builder, query, org, predicate);

        return typedQuery.getSingleResult();
    }

    private TypedQuery<OrganizationEntity> buildSearchQuery(CriteriaBuilder builder,
                                                            CriteriaQuery<OrganizationEntity> query,
                                                            Root<OrganizationEntity> org,
                                                            Predicate predicate) {
        return em.createQuery(
                query.select(org).distinct(true).where(predicate).orderBy(builder.asc(org.get("name"))));
    }

    private TypedQuery<Long> buildCountQuery(CriteriaBuilder builder, CriteriaQuery<Long> query,
                                             Root<OrganizationEntity> org, Predicate predicate) {
        return em.createQuery(query.select(builder.countDistinct(org)).where(predicate));
    }

    private Predicate buildStringSearchPredicate(CriteriaBuilder builder, CriteriaQuery<?> query, Root<OrganizationEntity> org, String search,
                                                 Boolean exact) {
        List<Predicate> predicates = new ArrayList<>();
        RealmModel realm = getRealm();
        Predicate realmPredicate = builder.equal(org.get("realmId"), realm.getId());

        if (StringUtil.isBlank(search)) {
            return realmPredicate;
        }

        predicates.add(realmPredicate);

        Join<OrganizationEntity, OrganizationDomainEntity> domain = org.join("domains", JoinType.LEFT);
        Predicate namePredicate;
        Predicate domainPredicate;

        if (Boolean.TRUE.equals(exact)) {
            namePredicate = builder.equal(org.get("name"), search);
            domainPredicate = builder.equal(domain.get("name"), search);
        } else {
            namePredicate = builder.like(builder.lower(org.get("name")), "%" + search.toLowerCase() + "%");
            domainPredicate = builder.like(domain.get("name"), "%" + search.toLowerCase() + "%");
        }

        predicates.add(builder.or(namePredicate, domainPredicate));

        return builder.and(predicates.toArray(new Predicate[0]));
    }

    private Predicate buildAttributeSearchPredicate(CriteriaBuilder builder, CriteriaQuery<?> query,
                                                    Root<OrganizationEntity> org, Map<String, String> attributes) {
        Root<GroupEntity> group = query.from(GroupEntity.class);

        List<Predicate> predicates = new ArrayList<>();
        RealmModel realm = getRealm();
        predicates.add(builder.equal(org.get("realmId"), realm.getId()));
        predicates.add(builder.equal(org.get("groupId"), group.get("id")));

        for (Map.Entry<String, String> entry : attributes.entrySet()) {
            if (StringUtil.isBlank(entry.getKey())) {
                continue;
            }

            if (OrganizationModel.ALIAS.equals(entry.getKey())) {
                predicates.add(builder.equal(org.get("alias"), entry.getValue()));
            } else {
                Join<GroupEntity, GroupAttributeEntity> groupJoin = group.join("attributes");
                Predicate attrNamePredicate = builder.equal(groupJoin.get("name"), entry.getKey());
                Predicate attrValuePredicate = builder.equal(groupJoin.get("value"), entry.getValue());
                predicates.add(builder.and(attrNamePredicate, attrValuePredicate));
            }
        }

        builder.count(builder.and(predicates.toArray(new Predicate[0])));
        return builder.and(predicates.toArray(new Predicate[0]));
    }

    @Override
    public Stream<UserModel> getMembersStream(OrganizationModel organization, String search, Boolean exact, Integer first, Integer max) {
        return getMembersStream(organization, Map.of(UserModel.SEARCH, search), exact, first, max);
    }

    @Override
    public Stream<UserModel> getMembersStream(OrganizationModel organization, Map<String, String> filters, Boolean exact, Integer first, Integer max) {
        throwExceptionIfObjectIsNull(organization, "Organization");
        var builder = em.getCriteriaBuilder();
        var queryBuilder = builder.createQuery(String.class);
        var groupMembership = queryBuilder.from(UserGroupMembershipEntity.class);

        queryBuilder.select(groupMembership.get("user").get("id"));

        var predicates = new ArrayList<>();
        var group = getOrganizationGroup(organization);

        predicates.add(builder.equal(groupMembership.get("groupId"), group.getId()));

        From<UserGroupMembershipEntity, UserEntity> userJoin = groupMembership.join("user");

        for (Entry<String, String> filter : Optional.ofNullable(filters).orElse(Map.of()).entrySet()) {
            switch (filter.getKey()) {
                case UserModel.SEARCH -> predicates.add(builder
                        .or(getSearchOptionPredicateArray(filter.getValue(), Optional.ofNullable(exact).orElse(false), builder, userJoin)));
                case MembershipType.NAME -> predicates.add(builder
                        .equal(groupMembership.get(MembershipType.NAME), filter.getValue().toUpperCase()));
            }
        }

        queryBuilder.where(predicates.toArray(new Predicate[0]));
        queryBuilder.orderBy(builder.asc(userJoin.get(USERNAME)));

        return closing(paginateQuery(em.createQuery(queryBuilder), first, max).getResultStream().map(id -> {
            UserModel user = userProvider.getUserById(getRealm(), id);

            if (isReadOnlyOrganizationMember(session, user)) {
                return new ReadOnlyUserModelDelegate(user) {
                    @Override
                    public boolean isEnabled() {
                        return false;
                    }
                };
            }

            return user;
        }));
    }

    private Predicate[] getSearchOptionPredicateArray(String value, boolean exact, CriteriaBuilder builder, From<?, UserEntity> from) {
        value = value.toLowerCase();

        List<Predicate> orPredicates = new ArrayList<>();

        if (exact) {
            orPredicates.add(builder.equal(from.get(USERNAME), value));
            orPredicates.add(builder.equal(from.get(EMAIL), value));
            orPredicates.add(builder.equal(builder.lower(from.get(FIRST_NAME)), value));
            orPredicates.add(builder.equal(builder.lower(from.get(LAST_NAME)), value));
        } else {
            value = "%" + value + "%";
            orPredicates.add(builder.like(from.get(USERNAME), value));
            orPredicates.add(builder.like(from.get(EMAIL), value));
            orPredicates.add(builder.like(builder.lower(from.get(FIRST_NAME)), value));
            orPredicates.add(builder.like(builder.lower(from.get(LAST_NAME)), value));
        }

        return orPredicates.toArray(Predicate[]::new);
    }

    @Override
    public long getMembersCount(OrganizationModel organization) {
        throwExceptionIfObjectIsNull(organization, "Organization");
        String groupId = getOrganizationGroup(organization).getId();

        return userProvider.getUsersCount(getRealm(), Set.of(groupId));
    }

    @Override
    public UserModel getMemberById(OrganizationModel organization, String id) {
        throwExceptionIfObjectIsNull(organization, "Organization");
        UserModel user = userProvider.getUserById(getRealm(), id);

        if (user == null) {
            return null;
        }

        if (getByMember(user).anyMatch(organization::equals)) {
            return user;
        }

        return null;
    }

    @Override
    public Stream<OrganizationModel> getByMember(UserModel member) {
        throwExceptionIfObjectIsNull(member, "User");

        TypedQuery<String> query;
        if(StorageId.isLocalStorage(member.getId())) {
            query = em.createNamedQuery("getGroupsByMember", String.class);
        } else {
            query = em.createNamedQuery("getGroupsByFederatedMember", String.class);
        }

        query.setParameter("userId", member.getId());

        OrganizationProvider organizations = session.getProvider(OrganizationProvider.class);
        GroupProvider groups = session.groups();

        return closing(query.getResultStream())
                .map((id) -> groups.getGroupById(getRealm(), id))
                .map((g) -> organizations.getById(g.getName()))
                .filter(Objects::nonNull);
    }

    @Override
    public boolean addIdentityProvider(OrganizationModel organization, IdentityProviderModel identityProvider) {
        throwExceptionIfObjectIsNull(organization, "Organization");
        throwExceptionIfObjectIsNull(identityProvider, "Identity provider");

        OrganizationEntity organizationEntity = getEntity(organization.getId());

        // check the identity provider and the organization belongs to the same realm
        if (!checkOrgIdpAndRealm(organizationEntity, identityProvider)) {
            return false;
        }

        String orgId = identityProvider.getOrganizationId();

        if (organizationEntity.getId().equals(orgId)) {
            return false;
        } else if (orgId != null) {
            throw new ModelValidationException("Identity provider already associated with a different organization");
        }

        identityProvider.setOrganizationId(organizationEntity.getId());
        session.identityProviders().update(identityProvider);

        return true;
    }

    @Override
    public Stream<IdentityProviderModel> getIdentityProviders(OrganizationModel organization) {
        throwExceptionIfObjectIsNull(organization, "Organization");
        throwExceptionIfObjectIsNull(organization.getId(), "Organization ID");

        OrganizationEntity organizationEntity = getEntity(organization.getId());

        return session.identityProviders().getByOrganization(organizationEntity.getId(), null, null);
    }

    @Override
    public boolean removeIdentityProvider(OrganizationModel organization, IdentityProviderModel identityProvider) {
        throwExceptionIfObjectIsNull(organization, "Organization");

        OrganizationEntity organizationEntity = getEntity(organization.getId());

        if (!organizationEntity.getId().equals(identityProvider.getOrganizationId())) {
            return false;
        }

        // clear the organization id and any domain assigned to the IDP.
        identityProvider.setOrganizationId(null);
        identityProvider.getConfig().remove(ORGANIZATION_DOMAIN_ATTRIBUTE);
        session.identityProviders().update(identityProvider);

        return true;
    }

    @Override
    public boolean isManagedMember(OrganizationModel organization, UserModel member) {
        throwExceptionIfObjectIsNull(organization, "organization");

        if (member == null) {
            return false;
        }

        UserEntity userEntity = em.find(UserEntity.class, member.getId());
        if (userEntity == null) {
            return false;
        }

        GroupModel organizationGroup = getOrganizationGroup(organization);
        try {
            UserGroupMembershipEntity membership = em.createNamedQuery("userMemberOf", UserGroupMembershipEntity.class)
                    .setParameter("user", userEntity)
                    .setParameter("groupId", organizationGroup.getId())
                    .getSingleResult();
            em.detach(membership);

            return MembershipType.MANAGED.equals(membership.getMembershipType());
        } catch (NoResultException e) {
            return false;
        }
    }

    @Override
    public boolean removeMember(OrganizationModel organization, UserModel member) {
        throwExceptionIfObjectIsNull(organization, "organization");
        throwExceptionIfObjectIsNull(member, "member");

        OrganizationModel userOrg = getByMember(member).filter(organization::equals).findAny().orElse(null);

        if (userOrg == null || !userOrg.equals(organization)) {
            return false;
        }

        if (isManagedMember(organization, member)) {
            userProvider.removeUser(getRealm(), member);
        } else {
            OrganizationModel current = Organizations.resolveOrganization(session);

            if (current == null) {
                session.getContext().setOrganization(organization);
            }

            try {
                member.leaveGroup(getOrganizationGroup(organization));
            } finally {
                if (current == null) {
                    session.getContext().setOrganization(null);
                }
            }
        }

        OrganizationModel.OrganizationMemberLeaveEvent.fire(organization, member, session);

        return true;
    }

    @Override
    public long count() {
        TypedQuery<Long> query;
        query = em.createNamedQuery("getCount", Long.class);
        query.setParameter("realmId", getRealm().getId());

        return query.getSingleResult();
    }

    @Override
    public boolean isEnabled() {
        return getRealm().isOrganizationsEnabled();
    }

    @Override
    public void close() {
    }

    /**
     * @throws ModelException if there is no entity with given {@code id}
     */
    private OrganizationEntity getEntity(String id) {
        return getEntity(id, true);
    }

    private OrganizationEntity getEntity(String id, boolean failIfNotFound) {
        OrganizationEntity entity = em.find(OrganizationEntity.class, id);

        if (entity == null) {
            if (failIfNotFound) {
                throw new ModelException("Organization [" + id + "] does not exist");
            }
            return null;
        }

        RealmModel realm = getRealm();
        if (!realm.getId().equals(entity.getRealmId())) {
            throw new ModelException("Organization [" + entity.getId() + "] does not belong to realm [" + realm.getId() + "]");
        }

        return entity;
    }

    private GroupModel createOrganizationGroup(String orgId) {
        return groupProvider.createGroup(getRealm(), null, Type.ORGANIZATION, orgId, null);
    }

    private GroupModel getOrganizationGroup(OrganizationModel organization) {
        throwExceptionIfObjectIsNull(organization, "Organization");
        OrganizationEntity entity = getEntity(organization.getId());
        GroupModel group = getOrganizationGroup(entity);

        if (group == null) {
            throw new ModelException("Organization group " + entity.getGroupId() + " not found");
        }

        return group;
    }

    private GroupModel getOrganizationGroup(OrganizationEntity entity) {
        return groupProvider.getGroupById(getRealm(), entity.getGroupId());
    }

    private void throwExceptionIfObjectIsNull(Object object, String objectName) {
        if (object == null) {
            throw new ModelException(String.format("%s cannot be null", objectName));
        }
    }

    private OrganizationEntity getByName(String name) {
        TypedQuery<OrganizationEntity> query = em.createNamedQuery("getByOrgName", OrganizationEntity.class);

        query.setParameter("name", name);
        query.setParameter("realmId", getRealm().getId());

        try {
            return query.getSingleResult();
        } catch (NoResultException nre) {
            return null;
        }
    }

    // return true only if the organization realm and the identity provider realm is the same
    private boolean checkOrgIdpAndRealm(OrganizationEntity orgEntity, IdentityProviderModel idp) {
        IdentityProviderModel orgIdpByAlias = session.identityProviders().getByAlias(idp.getAlias());
        return orgIdpByAlias != null && orgIdpByAlias.getInternalId().equals(idp.getInternalId());
    }

    private RealmModel getRealm() {
        RealmModel realm = session.getContext().getRealm();
        if (realm == null) {
            throw new IllegalArgumentException("Session not bound to a realm");
        }
        return realm;
    }
}
