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

import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.common.util.ObjectUtil;
import org.keycloak.models.ClientModel;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.jpa.entities.UserAttributeEntity;
import org.keycloak.models.jpa.entities.UserEntity;
import org.keycloak.models.jpa.entities.UserGroupMembershipEntity;
import org.keycloak.models.jpa.entities.UserRequiredActionEntity;
import org.keycloak.models.jpa.entities.UserRoleMappingEntity;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.models.utils.RoleUtils;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;
import javax.persistence.LockModeType;

import static org.keycloak.utils.StreamsUtil.closing;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class UserAdapter implements UserModel.Streams, JpaModel<UserEntity> {

    protected UserEntity user;
    protected EntityManager em;
    protected RealmModel realm;
    private final KeycloakSession session;

    public UserAdapter(KeycloakSession session, RealmModel realm, EntityManager em, UserEntity user) {
        this.em = em;
        this.user = user;
        this.realm = realm;
        this.session = session;
    }

    @Override
    public UserEntity getEntity() {
        return user;
    }

    @Override
    public String getId() {
        return user.getId();
    }

    @Override
    public String getUsername() {
        return user.getUsername();
    }

    @Override
    public void setUsername(String username) {
        username = KeycloakModelUtils.toLowerCaseSafe(username);
        user.setUsername(username);
    }

    @Override
    public Long getCreatedTimestamp() {
        return user.getCreatedTimestamp();
    }

    @Override
    public void setCreatedTimestamp(Long timestamp) {
        user.setCreatedTimestamp(timestamp);
    }

    @Override
    public boolean isEnabled() {
        return user.isEnabled();
    }

    @Override
    public void setEnabled(boolean enabled) {
        user.setEnabled(enabled);
    }

    @Override
    public void setSingleAttribute(String name, String value) {
        if (UserModel.FIRST_NAME.equals(name)) {
            user.setFirstName(value);
            return;
        } else if (UserModel.LAST_NAME.equals(name)) {
            user.setLastName(value);
            return;
        } else if (UserModel.EMAIL.equals(name)) {
            setEmail(value);
            return;
        } else if (UserModel.USERNAME.equals(name)) {
            setUsername(value);
            return;
        }
        // Remove all existing
        if (value == null) {
            user.getAttributes().removeIf(a -> a.getName().equals(name));
        } else {
            String firstExistingAttrId = null;
            List<UserAttributeEntity> toRemove = new ArrayList<>();
            for (UserAttributeEntity attr : user.getAttributes()) {
                if (attr.getName().equals(name)) {
                    if (firstExistingAttrId == null) {
                        attr.setValue(value);
                        firstExistingAttrId = attr.getId();
                    } else {
                        toRemove.add(attr);
                    }
                }
            }

            if (firstExistingAttrId != null) {
                // Remove attributes through HQL to avoid StaleUpdateException
                Query query = em.createNamedQuery("deleteUserAttributesByNameAndUserOtherThan");
                query.setParameter("name", name);
                query.setParameter("userId", user.getId());
                query.setParameter("attrId", firstExistingAttrId);
                int numUpdated = query.executeUpdate();

                // Remove attribute from local entity
                user.getAttributes().removeAll(toRemove);
            } else {
                persistAttributeValue(name, value);
            }
        }
    }

    @Override
    public void setAttribute(String name, List<String> values) {
        String valueToSet = (values != null && values.size() > 0) ? values.get(0) : null;
        if (UserModel.FIRST_NAME.equals(name)) {
            user.setFirstName(valueToSet);
            return;
        } else if (UserModel.LAST_NAME.equals(name)) {
            user.setLastName(valueToSet);
            return;
        } else if (UserModel.EMAIL.equals(name)) {
            setEmail(valueToSet);
            return;
        } else if (UserModel.USERNAME.equals(name)) {
            setUsername(valueToSet);
            return;
        }
        // Remove all existing
        removeAttribute(name);
        if (values != null) {
            for (Iterator<String> it = values.stream().filter(Objects::nonNull).iterator(); it.hasNext();) {
                persistAttributeValue(name, it.next());
            }
        }
    }

    private void persistAttributeValue(String name, String value) {
        UserAttributeEntity attr = new UserAttributeEntity();
        attr.setId(KeycloakModelUtils.generateId());
        attr.setName(name);
        attr.setValue(value);
        attr.setUser(user);
        em.persist(attr);
        user.getAttributes().add(attr);
    }

    @Override
    public void removeAttribute(String name) {
        List<UserAttributeEntity> toRemove = new ArrayList<>();
        for (UserAttributeEntity attr : user.getAttributes()) {
            if (attr.getName().equals(name)) {
                toRemove.add(attr);
            }
        }

        if (toRemove.isEmpty()) {
            return;
        }

        // KEYCLOAK-3296 : Remove attribute through HQL to avoid StaleUpdateException
        Query query = em.createNamedQuery("deleteUserAttributesByNameAndUser");
        query.setParameter("name", name);
        query.setParameter("userId", user.getId());
        query.executeUpdate();
        // KEYCLOAK-3494 : Also remove attributes from local user entity
        user.getAttributes().removeAll(toRemove);
    }

    @Override
    public String getFirstAttribute(String name) {
        if (UserModel.FIRST_NAME.equals(name)) {
            return user.getFirstName();
        } else if (UserModel.LAST_NAME.equals(name)) {
            return user.getLastName();
        } else if (UserModel.EMAIL.equals(name)) {
            return user.getEmail();
        } else if (UserModel.USERNAME.equals(name)) {
            return user.getUsername();
        }
        for (UserAttributeEntity attr : user.getAttributes()) {
            if (attr.getName().equals(name)) {
                return attr.getValue();
            }
        }
        return null;
    }

    @Override
    public Stream<String> getAttributeStream(String name) {
        if (UserModel.FIRST_NAME.equals(name)) {
            return Stream.of(user.getFirstName());
        } else if (UserModel.LAST_NAME.equals(name)) {
            return Stream.of(user.getLastName());
        } else if (UserModel.EMAIL.equals(name)) {
            return Stream.of(user.getEmail());
        } else if (UserModel.USERNAME.equals(name)) {
            return Stream.of(user.getUsername());
        }
        return user.getAttributes().stream().filter(attribute -> Objects.equals(attribute.getName(), name)).
                map(attribute -> attribute.getValue());
    }

    @Override
    public Map<String, List<String>> getAttributes() {
        MultivaluedHashMap<String, String> result = new MultivaluedHashMap<>();
        for (UserAttributeEntity attr : user.getAttributes()) {
            result.add(attr.getName(), attr.getValue());
        }
        result.add(UserModel.FIRST_NAME, user.getFirstName());
        result.add(UserModel.LAST_NAME, user.getLastName());
        result.add(UserModel.EMAIL, user.getEmail());
        result.add(UserModel.USERNAME, user.getUsername());
        return result;
    }

    @Override
    public Stream<String> getRequiredActionsStream() {
        return user.getRequiredActions().stream().map(action -> action.getAction()).distinct();
    }

    @Override
    public void addRequiredAction(String actionName) {
        for (UserRequiredActionEntity attr : user.getRequiredActions()) {
            if (attr.getAction().equals(actionName)) {
                return;
            }
        }
        UserRequiredActionEntity attr = new UserRequiredActionEntity();
        attr.setAction(actionName);
        attr.setUser(user);
        em.persist(attr);
        user.getRequiredActions().add(attr);
    }

    @Override
    public void removeRequiredAction(String actionName) {
        Iterator<UserRequiredActionEntity> it = user.getRequiredActions().iterator();
        while (it.hasNext()) {
            UserRequiredActionEntity attr = it.next();
            if (attr.getAction().equals(actionName)) {
                it.remove();
                em.remove(attr);
            }
        }
    }

    @Override
    public String getFirstName() {
        return user.getFirstName();
    }

    @Override
    public void setFirstName(String firstName) {
        user.setFirstName(firstName);
    }

    @Override
    public String getLastName() {
        return user.getLastName();
    }

    @Override
    public void setLastName(String lastName) {
        user.setLastName(lastName);
    }

    @Override
    public String getEmail() {
        return user.getEmail();
    }

    @Override
    public void setEmail(String email) {
        if (ObjectUtil.isBlank(email)) {
            email = null;
        }
        email = KeycloakModelUtils.toLowerCaseSafe(email);
        user.setEmail(email, realm.isDuplicateEmailsAllowed());
    }

    @Override
    public boolean isEmailVerified() {
        return user.isEmailVerified();
    }

    @Override
    public void setEmailVerified(boolean verified) {
        user.setEmailVerified(verified);
    }

    private TypedQuery<String> createGetGroupsQuery() {
        // we query ids only as the group  might be cached and following the @ManyToOne will result in a load
        // even if we're getting just the id.
        CriteriaBuilder builder = em.getCriteriaBuilder();
        CriteriaQuery<String> queryBuilder = builder.createQuery(String.class);
        Root<UserGroupMembershipEntity> root = queryBuilder.from(UserGroupMembershipEntity.class);

        List<Predicate> predicates = new ArrayList<>();
        predicates.add(builder.equal(root.get("user"), getEntity()));

        queryBuilder.select(root.get("groupId"));
        queryBuilder.where(predicates.toArray(new Predicate[0]));

        return em.createQuery(queryBuilder);
    }

    private TypedQuery<Long> createCountGroupsQuery() {
        // we query ids only as the group  might be cached and following the @ManyToOne will result in a load
        // even if we're getting just the id.
        CriteriaBuilder builder = em.getCriteriaBuilder();
        CriteriaQuery<Long> queryBuilder = builder.createQuery(Long.class);
        Root<UserGroupMembershipEntity> root = queryBuilder.from(UserGroupMembershipEntity.class);

        List<Predicate> predicates = new ArrayList<>();
        predicates.add(builder.equal(root.get("user"), getEntity()));

        queryBuilder.select(builder.count(root));
        queryBuilder.where(predicates.toArray(new Predicate[0]));
        return em.createQuery(queryBuilder);
    }

    @Override
    public Stream<GroupModel> getGroupsStream() {
        return getGroupsStream(null, null, null);
    }

    @Override
    public Stream<GroupModel> getGroupsStream(String search, Integer first, Integer max) {
        return session.groups().getGroupsStream(realm, closing(createGetGroupsQuery().getResultStream()), search, first, max);
    }

    @Override
    public long getGroupsCount() {
        return createCountGroupsQuery().getSingleResult();
    }

    @Override
    public long getGroupsCountByNameContaining(String search) {
        if (search == null) return getGroupsCount();
        return session.groups().getGroupsCount(realm, closing(createGetGroupsQuery().getResultStream()), search);
    }

    @Override
    public void joinGroup(GroupModel group) {
        if (isMemberOf(group)) return;
        joinGroupImpl(group);

    }

    protected void joinGroupImpl(GroupModel group) {
        UserGroupMembershipEntity entity = new UserGroupMembershipEntity();
        entity.setUser(getEntity());
        entity.setGroupId(group.getId());
        em.persist(entity);
        em.flush();
        em.detach(entity);

    }

    @Override
    public void leaveGroup(GroupModel group) {
        if (user == null || group == null) return;

        TypedQuery<UserGroupMembershipEntity> query = getUserGroupMappingQuery(group);
        query.setLockMode(LockModeType.PESSIMISTIC_WRITE);
        List<UserGroupMembershipEntity> results = query.getResultList();
        if (results.size() == 0) return;
        for (UserGroupMembershipEntity entity : results) {
            em.remove(entity);
        }
        em.flush();

    }

    @Override
    public boolean isMemberOf(GroupModel group) {
        return RoleUtils.isMember(getGroupsStream(), group);
    }

    protected TypedQuery<UserGroupMembershipEntity> getUserGroupMappingQuery(GroupModel group) {
        TypedQuery<UserGroupMembershipEntity> query = em.createNamedQuery("userMemberOf", UserGroupMembershipEntity.class);
        query.setParameter("user", getEntity());
        query.setParameter("groupId", group.getId());
        return query;
    }


    @Override
    public boolean hasRole(RoleModel role) {
        return RoleUtils.hasRole(getRoleMappingsStream(), role)
                || RoleUtils.hasRoleFromGroup(getGroupsStream(), role, true);
    }

    protected TypedQuery<UserRoleMappingEntity> getUserRoleMappingEntityTypedQuery(RoleModel role) {
        TypedQuery<UserRoleMappingEntity> query = em.createNamedQuery("userHasRole", UserRoleMappingEntity.class);
        query.setParameter("user", getEntity());
        query.setParameter("roleId", role.getId());
        return query;
    }

    @Override
    public void grantRole(RoleModel role) {
        if (hasDirectRole(role)) return;
        grantRoleImpl(role);
    }

    public void grantRoleImpl(RoleModel role) {
        UserRoleMappingEntity entity = new UserRoleMappingEntity();
        entity.setUser(getEntity());
        entity.setRoleId(role.getId());
        em.persist(entity);
        em.flush();
        em.detach(entity);
    }

    @Override
    public Stream<RoleModel> getRealmRoleMappingsStream() {
        return getRoleMappingsStream().filter(RoleUtils::isRealmRole);
    }


    @Override
    public Stream<RoleModel> getRoleMappingsStream() {
        // we query ids only as the role might be cached and following the @ManyToOne will result in a load
        // even if we're getting just the id.
        TypedQuery<String> query = em.createNamedQuery("userRoleMappingIds", String.class);
        query.setParameter("user", getEntity());
        return closing(query.getResultStream().map(realm::getRoleById).filter(Objects::nonNull));
    }

    @Override
    public void deleteRoleMapping(RoleModel role) {
        if (user == null || role == null) return;

        TypedQuery<UserRoleMappingEntity> query = getUserRoleMappingEntityTypedQuery(role);
        query.setLockMode(LockModeType.PESSIMISTIC_WRITE);
        List<UserRoleMappingEntity> results = query.getResultList();
        if (results.size() == 0) return;
        for (UserRoleMappingEntity entity : results) {
            em.remove(entity);
        }
        em.flush();
    }

    @Override
    public Stream<RoleModel> getClientRoleMappingsStream(ClientModel app) {
        return getRoleMappingsStream().filter(r -> RoleUtils.isClientRole(r, app));
    }

    @Override
    public String getFederationLink() {
        return user.getFederationLink();
    }

    @Override
    public void setFederationLink(String link) {
        user.setFederationLink(link);
    }

    @Override
    public String getServiceAccountClientLink() {
        return user.getServiceAccountClientLink();
    }

    @Override
    public void setServiceAccountClientLink(String clientInternalId) {
        user.setServiceAccountClientLink(clientInternalId);
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || !(o instanceof UserModel)) return false;

        UserModel that = (UserModel) o;
        return that.getId().equals(getId());
    }

    @Override
    public int hashCode() {
        return getId().hashCode();
    }


}
