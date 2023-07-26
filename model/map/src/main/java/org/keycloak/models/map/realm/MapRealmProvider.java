/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.models.map.realm;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;
import org.jboss.logging.Logger;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelDuplicateException;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RealmModel.SearchableFields;
import org.keycloak.models.RealmProvider;
import org.keycloak.models.map.common.DeepCloner;
import org.keycloak.models.map.storage.MapStorage;
import org.keycloak.models.map.storage.ModelCriteriaBuilder.Operator;
import org.keycloak.models.map.storage.criteria.DefaultModelCriteria;
import org.keycloak.models.utils.KeycloakModelUtils;

import static org.keycloak.common.util.StackUtil.getShortStackTrace;
import static org.keycloak.models.map.common.AbstractMapProviderFactory.MapProviderObjectType.REALM_AFTER_REMOVE;
import static org.keycloak.models.map.common.AbstractMapProviderFactory.MapProviderObjectType.REALM_BEFORE_REMOVE;
import static org.keycloak.models.map.storage.QueryParameters.Order.ASCENDING;
import static org.keycloak.models.map.storage.QueryParameters.withCriteria;
import static org.keycloak.models.map.storage.criteria.DefaultModelCriteria.criteria;

public class MapRealmProvider implements RealmProvider {

    private static final Logger LOG = Logger.getLogger(MapRealmProvider.class);
    private final KeycloakSession session;
    final MapStorage<MapRealmEntity, RealmModel> store;

    public MapRealmProvider(KeycloakSession session, MapStorage<MapRealmEntity, RealmModel> realmStore) {
        this.session = session;
        this.store = realmStore;
    }

    private RealmModel entityToAdapter(MapRealmEntity entity) {
        return new MapRealmAdapter(session, entity);
    }

    @Override
    public RealmModel createRealm(String name) {
        return createRealm(KeycloakModelUtils.generateId(), name);
    }

    @Override
    public RealmModel createRealm(String id, String name) {
        if (getRealmByName(name) != null) {
            throw new ModelDuplicateException("Realm with given name exists: " + name);
        }

        if (id != null && store.exists(id)) {
            throw new ModelDuplicateException("Realm exists: " + id);
        }

        LOG.tracef("createRealm(%s, %s)%s", id, name, getShortStackTrace());

        MapRealmEntity entity = DeepCloner.DUMB_CLONER.newInstance(MapRealmEntity.class);
        entity.setId(id);
        entity.setName(name);

        entity = store.create(entity);
        return entityToAdapter(entity);
    }

    @Override
    public RealmModel getRealm(String id) {
        if (id == null) return null;

        LOG.tracef("getRealm(%s)%s", id, getShortStackTrace());

        MapRealmEntity entity = store.read(id);
        return entity == null ? null : entityToAdapter(entity);
    }

    @Override
    public RealmModel getRealmByName(String name) {
        if (name == null) return null;

        LOG.tracef("getRealmByName(%s)%s", name, getShortStackTrace());

        DefaultModelCriteria<RealmModel> mcb = criteria();
        mcb = mcb.compare(SearchableFields.NAME, Operator.EQ, name);

        String realmId = store.read(withCriteria(mcb))
                .findFirst()
                .map(MapRealmEntity::getId)
                .orElse(null);
        //we need to go via session.realms() not to bypass cache
        return realmId == null ? null : session.realms().getRealm(realmId);
    }

    @Override
    public Stream<RealmModel> getRealmsStream() {
        return getRealmsStream(criteria());
    }

    @Override
    public Stream<RealmModel> getRealmsWithProviderTypeStream(Class<?> type) {
        DefaultModelCriteria<RealmModel> mcb = criteria();
        mcb = mcb.compare(SearchableFields.COMPONENT_PROVIDER_TYPE, Operator.EQ, type.getName());

        return getRealmsStream(mcb);
    }

    private Stream<RealmModel> getRealmsStream(DefaultModelCriteria<RealmModel> mcb) {
        return store.read(withCriteria(mcb).orderBy(SearchableFields.NAME, ASCENDING))
                .map(this::entityToAdapter);
    }

    @Override
    public boolean removeRealm(String id) {
        LOG.tracef("removeRealm(%s)%s", id, getShortStackTrace());

        RealmModel realm = getRealm(id);

        if (realm == null) return false;

        session.invalidate(REALM_BEFORE_REMOVE, realm);

        store.delete(id);

        session.invalidate(REALM_AFTER_REMOVE, realm);

        return true;
    }

    @Override
    public void removeExpiredClientInitialAccess() {
        DefaultModelCriteria<RealmModel> mcb = criteria();
        mcb = mcb.compare(SearchableFields.CLIENT_INITIAL_ACCESS, Operator.EXISTS);

        store.read(withCriteria(mcb))
                .forEach(MapRealmEntity::removeExpiredClientInitialAccesses);
    }

    //TODO move the following method to adapter
    @Override
    public void saveLocalizationText(RealmModel realm, String locale, String key, String text) {
        if (locale == null || key == null || text == null) return;
        Map<String, String> texts = new HashMap<>();
        texts.put(key, text);
        realm.createOrUpdateRealmLocalizationTexts(locale, texts);
    }

    //TODO move the following method to adapter
    @Override
    public void saveLocalizationTexts(RealmModel realm, String locale, Map<String, String> localizationTexts) {
        if (locale == null || localizationTexts == null) return;
        realm.createOrUpdateRealmLocalizationTexts(locale, localizationTexts);
    }

    //TODO move the following method to adapter
    @Override
    public boolean updateLocalizationText(RealmModel realm, String locale, String key, String text) {
        if (locale == null || key == null || text == null || (! realm.getRealmLocalizationTextsByLocale(locale).containsKey(key))) return false;
        saveLocalizationText(realm, locale, key, text);
        return true;
    }

    //TODO move the following method to adapter
    @Override
    public boolean deleteLocalizationTextsByLocale(RealmModel realm, String locale) {
        return realm.removeRealmLocalizationTexts(locale);
    }

    //TODO move the following method to adapter
    @Override
    public boolean deleteLocalizationText(RealmModel realm, String locale, String key) {
        if (locale == null || key == null || (! realm.getRealmLocalizationTextsByLocale(locale).containsKey(key))) return false;

        Map<String, String> texts = new HashMap<>(realm.getRealmLocalizationTextsByLocale(locale));
        texts.remove(key);
        realm.removeRealmLocalizationTexts(locale);
        realm.createOrUpdateRealmLocalizationTexts(locale, texts);
        return true;
    }

    //TODO move the following method to adapter
    @Override
    public String getLocalizationTextsById(RealmModel realm, String locale, String key) {
        if (locale == null || key == null || (! realm.getRealmLocalizationTextsByLocale(locale).containsKey(key))) return null;
        return realm.getRealmLocalizationTextsByLocale(locale).get(key);
    }

    @Override
    public void close() {
    }
}
