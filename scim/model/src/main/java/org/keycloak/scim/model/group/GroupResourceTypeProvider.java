/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */

package org.keycloak.scim.model.group;

import java.util.stream.Stream;

import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import org.keycloak.authorization.fgap.AdminPermissionsSchema;
import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.jpa.GroupAdapter;
import org.keycloak.models.jpa.entities.GroupEntity;
import org.keycloak.scim.filter.FilterUtils;
import org.keycloak.scim.filter.ScimFilterParser;
import org.keycloak.scim.model.filter.AttributeInfo;
import org.keycloak.scim.model.filter.ScimJPAPredicateEvaluator;
import org.keycloak.scim.protocol.request.SearchRequest;
import org.keycloak.scim.resource.Scim;
import org.keycloak.scim.resource.group.Group;
import org.keycloak.scim.resource.spi.AbstractScimResourceTypeProvider;
import org.keycloak.utils.StringUtil;

import static org.keycloak.models.jpa.PaginationUtils.paginateQuery;
import static org.keycloak.utils.StreamsUtil.closing;

public class GroupResourceTypeProvider extends AbstractScimResourceTypeProvider<GroupModel, Group> {

    public GroupResourceTypeProvider(KeycloakSession session) {
        super(session, new GroupCoreModelSchema());
    }

    @Override
    public Group onCreate(Group group) {
        RealmModel realm = session.getContext().getRealm();
        GroupModel model = session.groups().createGroup(realm, group.getDisplayName());
        populate(model, group);
        return group;
    }

    @Override
    protected Group onUpdate(GroupModel model, Group resource) {
        return resource;
    }

    @Override
    protected GroupModel getModel(String id) {
        RealmModel realm = session.getContext().getRealm();
        return session.groups().getGroupById(realm, id);
    }

    @Override
    protected String getRealmResourceType() {
        return AdminPermissionsSchema.GROUPS_RESOURCE_TYPE;
    }

    @Override
    protected Stream<GroupModel> getModels(SearchRequest searchRequest) {
        RealmModel realm = session.getContext().getRealm();
        Integer firstResult = searchRequest.getStartIndex() != null ? searchRequest.getStartIndex() - 1 : null;
        Integer maxResults = searchRequest.getCount();

        if (StringUtil.isNotBlank(searchRequest.getFilter())) {
            // Parse filter into AST
            ScimFilterParser.FilterContext filterContext = FilterUtils.parseFilter(searchRequest.getFilter());

            // Execute JPA query with filter
            EntityManager em = session.getProvider(JpaConnectionProvider.class).getEntityManager();
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<GroupEntity> query = cb.createQuery(GroupEntity.class);
            Root<GroupEntity> root = query.from(GroupEntity.class);

            // Create filter predicate using the same query and root that will be used for execution
            ScimJPAPredicateEvaluator evaluator = new ScimJPAPredicateEvaluator(scimAttrPath -> {
                // first split the attribute path into schema and attribute name. If no schema is specified, use the core user schema by default
                String[] splitAttrPath = splitScimAttribute(scimAttrPath);

                if (Scim.GROUP_CORE_SCHEMA.equals(splitAttrPath[0]) && "displayName".equalsIgnoreCase(splitAttrPath[1])) {;
                    return new AttributeInfo("name", true, null);
                }
                return null;
            }, cb, query, root);
            Predicate filterPredicate = evaluator.visit(filterContext).predicate();

            // Apply realm restriction
            Predicate realmPredicate = cb.equal(root.get("realm"), realm.getId());

            // Combine with filter predicate
            query.where(cb.and(realmPredicate, filterPredicate));

            // Execute query and convert to UserModel stream
            return closing(paginateQuery(em.createQuery(query), firstResult, maxResults).getResultStream()
                    .map(entity -> new GroupAdapter(session, realm, em, entity)));
        } else {
            return session.groups().getTopLevelGroupsStream(realm, firstResult, maxResults);
        }
    }

    @Override
    public boolean onDelete(String id) {
        RealmModel realm = session.getContext().getRealm();
        return session.groups().removeGroup(realm, getModel(id));
    }

    @Override
    public Class<Group> getResourceType() {
        return Group.class;
    }

    @Override
    public void close() {

    }
}
