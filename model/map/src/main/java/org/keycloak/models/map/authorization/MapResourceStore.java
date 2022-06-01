/*
 * Copyright 2022 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.models.map.authorization;

import org.jboss.logging.Logger;
import org.keycloak.authorization.AuthorizationProvider;
import org.keycloak.authorization.model.Resource;
import org.keycloak.authorization.model.Resource.SearchableFields;
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.authorization.model.Scope;
import org.keycloak.authorization.store.ResourceStore;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelDuplicateException;
import org.keycloak.models.RealmModel;
import org.keycloak.models.map.authorization.adapter.MapResourceAdapter;
import org.keycloak.models.map.authorization.entity.MapResourceEntity;
import org.keycloak.models.map.authorization.entity.MapResourceEntityImpl;
import org.keycloak.models.map.storage.MapKeycloakTransaction;
import org.keycloak.models.map.storage.MapStorage;
import org.keycloak.models.map.storage.ModelCriteriaBuilder.Operator;
import org.keycloak.models.map.storage.criteria.DefaultModelCriteria;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.keycloak.common.util.StackUtil.getShortStackTrace;
import static org.keycloak.models.map.storage.QueryParameters.withCriteria;
import static org.keycloak.models.map.storage.criteria.DefaultModelCriteria.criteria;

public class MapResourceStore implements ResourceStore {

    private static final Logger LOG = Logger.getLogger(MapResourceStore.class);
    private final AuthorizationProvider authorizationProvider;
    final MapKeycloakTransaction<MapResourceEntity, Resource> tx;
    private final KeycloakSession session;

    public MapResourceStore(KeycloakSession session, MapStorage<MapResourceEntity, Resource> resourceStore, AuthorizationProvider provider) {
        this.tx = resourceStore.createTransaction(session);
        session.getTransactionManager().enlist(tx);
        authorizationProvider = provider;
        this.session = session;
    }

    private Function<MapResourceEntity, Resource> entityToAdapterFunc(RealmModel realm, final ResourceServer resourceServer) {
        return origEntity ->  new MapResourceAdapter(realm, resourceServer, origEntity, authorizationProvider.getStoreFactory());
    }
    
    private DefaultModelCriteria<Resource> forRealmAndResourceServer(RealmModel realm, ResourceServer resourceServer) {
        DefaultModelCriteria<Resource> mcb = DefaultModelCriteria.<Resource>criteria()
                .compare(Resource.SearchableFields.REALM_ID, Operator.EQ, realm.getId());

        return resourceServer == null
                ? mcb
                : mcb.compare(SearchableFields.RESOURCE_SERVER_ID, Operator.EQ,
                resourceServer.getId());
    }

    @Override
    public Resource create(ResourceServer resourceServer, String id, String name, String owner) {
        LOG.tracef("create(%s, %s, %s, %s)%s", id, name, resourceServer, owner, getShortStackTrace());
        // @UniqueConstraint(columnNames = {"NAME", "RESOURCE_SERVER_ID", "OWNER"})
        RealmModel realm = resourceServer.getRealm();

        DefaultModelCriteria<Resource> mcb = forRealmAndResourceServer(realm, resourceServer)
                .compare(SearchableFields.NAME, Operator.EQ, name)
                .compare(SearchableFields.OWNER, Operator.EQ, owner);

        if (tx.getCount(withCriteria(mcb)) > 0) {
            throw new ModelDuplicateException("Resource with name '" + name + "' for " + resourceServer.getId() + " already exists for request owner " + owner);
        }

        MapResourceEntity entity = new MapResourceEntityImpl();
        entity.setId(id);
        entity.setName(name);
        entity.setResourceServerId(resourceServer.getId());
        entity.setOwner(owner);
        entity.setRealmId(realm.getId());

        entity = tx.create(entity);

        return entity == null ? null : entityToAdapterFunc(realm, resourceServer).apply(entity);
    }

    @Override
    public void delete(RealmModel realm, String id) {
        LOG.tracef("delete(%s)%s", id, getShortStackTrace());
        Resource resource = findById(realm, null, id);
        if (resource == null) return;

        tx.delete(id);
    }

    @Override
    public Resource findById(RealmModel realm, ResourceServer resourceServer, String id) {
        LOG.tracef("findById(%s, %s)%s", id, resourceServer, getShortStackTrace());

        if (id == null) return null;

        return tx.read(withCriteria(forRealmAndResourceServer(realm, resourceServer)
                .compare(SearchableFields.ID, Operator.EQ, id)))
                .findFirst()
                .map(entityToAdapterFunc(realm, resourceServer))
                .orElse(null);
    }

    @Override
    public void findByOwner(RealmModel realm, ResourceServer resourceServer, String ownerId, Consumer<Resource> consumer) {
        LOG.tracef("findByOwner(%s, %s, %s)%s", realm, resourceServer, resourceServer, ownerId, getShortStackTrace());

        tx.read(withCriteria(forRealmAndResourceServer(realm, resourceServer)
                        .compare(SearchableFields.OWNER, Operator.EQ, ownerId)))
                .map(entityToAdapterFunc(realm, resourceServer))
                .forEach(consumer);
    }

    @Override
    public List<Resource> findByResourceServer(ResourceServer resourceServer) {
        LOG.tracef("findByResourceServer(%s)%s", resourceServer, getShortStackTrace());
        RealmModel realm = resourceServer.getRealm();

        return tx.read(withCriteria(forRealmAndResourceServer(realm, resourceServer)))
                .map(entityToAdapterFunc(realm, resourceServer))
                .collect(Collectors.toList());
    }

    @Override
    public List<Resource> find(RealmModel realm, ResourceServer resourceServer, Map<Resource.FilterOption, String[]> attributes, Integer firstResult, Integer maxResults) {
        LOG.tracef("findByResourceServer(%s, %s, %s, %d, %d)%s", realm, resourceServer, attributes, firstResult, maxResults, getShortStackTrace());
        DefaultModelCriteria<Resource> mcb = forRealmAndResourceServer(realm, resourceServer).and(
                attributes.entrySet().stream()
                        .map(this::filterEntryToDefaultModelCriteria)
                        .toArray(DefaultModelCriteria[]::new)
        );

        return tx.read(withCriteria(mcb).pagination(firstResult, maxResults, SearchableFields.NAME))
                .map(entityToAdapterFunc(realm, resourceServer))
                .collect(Collectors.toList());
    }

    private DefaultModelCriteria<Resource> filterEntryToDefaultModelCriteria(Map.Entry<Resource.FilterOption, String[]> entry) {
        Resource.FilterOption name = entry.getKey();
        String[] value = entry.getValue();

        DefaultModelCriteria<Resource> mcb = criteria();
        switch (name) {
            case ID:
            case SCOPE_ID:
            case OWNER:
            case URI:
                return mcb.compare(name.getSearchableModelField(), Operator.IN, Arrays.asList(value));
            case URI_NOT_NULL:
                return mcb.compare(SearchableFields.URI, Operator.EXISTS);
            case OWNER_MANAGED_ACCESS:
                return mcb.compare(SearchableFields.OWNER_MANAGED_ACCESS, Operator.EQ, Boolean.valueOf(value[0]));
            case EXACT_NAME:
                return mcb.compare(SearchableFields.NAME, Operator.EQ, value[0]);
            case NAME:
                return mcb.compare(SearchableFields.NAME, Operator.ILIKE, "%" + value[0] + "%");
            case TYPE:
                return mcb.compare(SearchableFields.TYPE, Operator.ILIKE, "%" + value[0] + "%");
            default:
                throw new IllegalArgumentException("Unsupported filter [" + name + "]");

        }
    }

    @Override
    public void findByScopes(ResourceServer resourceServer, Set<Scope> scopes, Consumer<Resource> consumer) {
        LOG.tracef("findByScope(%s, %s, %s)%s", scopes, resourceServer, consumer, getShortStackTrace());
        RealmModel realm = resourceServer.getRealm();

        tx.read(withCriteria(forRealmAndResourceServer(realm, resourceServer)
                .compare(SearchableFields.SCOPE_ID, Operator.IN, scopes.stream().map(Scope::getId))))
                .map(entityToAdapterFunc(realm, resourceServer))
                .forEach(consumer);
    }

    @Override
    public Resource findByName(ResourceServer resourceServer, String name, String ownerId) {
        LOG.tracef("findByName(%s, %s, %s)%s", name, ownerId, resourceServer, getShortStackTrace());
        RealmModel realm = resourceServer.getRealm();

        return tx.read(withCriteria(forRealmAndResourceServer(realm, resourceServer)
                .compare(SearchableFields.OWNER, Operator.EQ, ownerId)
                .compare(SearchableFields.NAME, Operator.EQ, name)))
                .findFirst()
                .map(entityToAdapterFunc(realm, resourceServer))
                .orElse(null);
    }

    @Override
    public void findByType(ResourceServer resourceServer, String type, Consumer<Resource> consumer) {
        LOG.tracef("findByType(%s, %s, %s)%s", type, resourceServer, consumer, getShortStackTrace());
        RealmModel realm = authorizationProvider.getRealm();

        tx.read(withCriteria(forRealmAndResourceServer(realm, resourceServer)
                .compare(SearchableFields.TYPE, Operator.EQ, type)))
            .map(entityToAdapterFunc(realm, resourceServer))
            .forEach(consumer);
    }

    @Override
    public void findByType(ResourceServer resourceServer, String type, String owner, Consumer<Resource> consumer) {
        LOG.tracef("findByType(%s, %s, %s, %s)%s", type, owner, resourceServer, consumer, getShortStackTrace());
        RealmModel realm = resourceServer.getRealm();

        DefaultModelCriteria<Resource> mcb = forRealmAndResourceServer(realm, resourceServer)
                .compare(SearchableFields.TYPE, Operator.EQ, type);

        if (owner != null) {
            mcb = mcb.compare(SearchableFields.OWNER, Operator.EQ, owner);
        }

        tx.read(withCriteria(mcb))
                .map(entityToAdapterFunc(realm, resourceServer))
                .forEach(consumer);
    }

    @Override
    public void findByTypeInstance(ResourceServer resourceServer, String type, Consumer<Resource> consumer) {
        LOG.tracef("findByTypeInstance(%s, %s, %s)%s", type, resourceServer, consumer, getShortStackTrace());
        RealmModel realm = resourceServer.getRealm();
        tx.read(withCriteria(forRealmAndResourceServer(realm, resourceServer)
                .compare(SearchableFields.OWNER, Operator.NE, resourceServer.getClientId())
                .compare(SearchableFields.TYPE, Operator.EQ, type)))
                .map(entityToAdapterFunc(realm, resourceServer))
                .forEach(consumer);
    }

    public void preRemove(RealmModel realm) {
        LOG.tracef("preRemove(%s)%s", realm, getShortStackTrace());

        DefaultModelCriteria<Resource> mcb = criteria();
        mcb = mcb.compare(SearchableFields.REALM_ID, Operator.EQ, realm.getId());

        tx.delete(withCriteria(mcb));
    }

    public void preRemove(ResourceServer resourceServer) {
        LOG.tracef("preRemove(%s)%s", resourceServer, getShortStackTrace());

        tx.delete(withCriteria(forRealmAndResourceServer(resourceServer.getRealm(), resourceServer)));
    }
}
