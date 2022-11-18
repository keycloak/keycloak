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

package org.keycloak.models.map.singleUseObject;

import org.jboss.logging.Logger;
import org.keycloak.common.util.Time;
import org.keycloak.models.SingleUseObjectValueModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelDuplicateException;
import org.keycloak.models.SingleUseObjectProvider;
import org.keycloak.models.map.common.TimeAdapter;
import org.keycloak.models.map.storage.MapKeycloakTransaction;
import org.keycloak.models.map.storage.MapStorage;
import org.keycloak.models.map.storage.ModelCriteriaBuilder;
import org.keycloak.models.map.storage.criteria.DefaultModelCriteria;

import java.util.Collections;
import java.util.Map;

import static org.keycloak.common.util.StackUtil.getShortStackTrace;
import static org.keycloak.models.map.common.ExpirationUtils.isExpired;
import static org.keycloak.models.map.storage.QueryParameters.withCriteria;
import static org.keycloak.models.map.storage.criteria.DefaultModelCriteria.criteria;

/**
 * @author <a href="mailto:mkanis@redhat.com">Martin Kanis</a>
 */
public class MapSingleUseObjectProvider implements SingleUseObjectProvider {

    private static final Logger LOG = Logger.getLogger(MapSingleUseObjectProvider.class);
    private final KeycloakSession session;
    protected final MapKeycloakTransaction<MapSingleUseObjectEntity, SingleUseObjectValueModel> singleUseObjectTx;

    public MapSingleUseObjectProvider(KeycloakSession session, MapStorage<MapSingleUseObjectEntity, SingleUseObjectValueModel> storage) {
        this.session = session;
        singleUseObjectTx = storage.createTransaction(session);

        session.getTransactionManager().enlistAfterCompletion(singleUseObjectTx);
    }

    @Override
    public void put(String key, long lifespanSeconds, Map<String, String> notes) {
        LOG.tracef("put(%s)%s", key, getShortStackTrace());

        MapSingleUseObjectEntity singleUseEntity = getWithExpiration(key);

        if (singleUseEntity != null) {
            throw new ModelDuplicateException("Single-use object entity exists: " + singleUseEntity.getObjectKey());
        }

        singleUseEntity = new MapSingleUseObjectEntityImpl();
        singleUseEntity.setObjectKey(key);
        singleUseEntity.setExpiration(Time.currentTimeMillis() + TimeAdapter.fromSecondsToMilliseconds(lifespanSeconds));
        singleUseEntity.setNotes(notes);

        singleUseObjectTx.create(singleUseEntity);
    }

    @Override
    public Map<String, String> get(String key) {
        LOG.tracef("get(%s)%s", key, getShortStackTrace());

        MapSingleUseObjectEntity singleUseObject = getWithExpiration(key);
        if (singleUseObject != null) {
            Map<String, String> notes = singleUseObject.getNotes();
            return notes == null ? Collections.emptyMap() : Collections.unmodifiableMap(notes);
        }

        return null;
    }

    @Override
    public Map<String, String> remove(String key) {
        LOG.tracef("remove(%s)%s", key, getShortStackTrace());

        MapSingleUseObjectEntity singleUseEntity = getWithExpiration(key);

        if (singleUseEntity != null) {
            Map<String, String> notes = singleUseEntity.getNotes();
            if (singleUseObjectTx.delete(singleUseEntity.getId())) {
                return notes == null ? Collections.emptyMap() : Collections.unmodifiableMap(notes);
            }
        }
        // the single-use entity expired or someone else already used and deleted it
        return null;
    }

    @Override
    public boolean replace(String key, Map<String, String> notes) {
        LOG.tracef("replace(%s)%s", key, getShortStackTrace());

        MapSingleUseObjectEntity singleUseEntity = getWithExpiration(key);
        if (singleUseEntity != null) {
            singleUseEntity.setNotes(notes);
            return true;
        }

        return false;
    }

    @Override
    public boolean putIfAbsent(String key, long lifespanInSeconds) {
        LOG.tracef("putIfAbsent(%s)%s", key, getShortStackTrace());

        MapSingleUseObjectEntity singleUseEntity = getWithExpiration(key);
        if (singleUseEntity != null) {
            return false;
        } else {
            singleUseEntity = new MapSingleUseObjectEntityImpl();
            singleUseEntity.setObjectKey(key);
            singleUseEntity.setExpiration(Time.currentTimeMillis() + TimeAdapter.fromSecondsToMilliseconds(lifespanInSeconds));

            singleUseObjectTx.create(singleUseEntity);

            return true;
        }
    }

    @Override
    public boolean contains(String key) {
        LOG.tracef("contains(%s)%s", key, getShortStackTrace());

        MapSingleUseObjectEntity singleUseObject = getWithExpiration(key);

        return singleUseObject != null;
    }

    @Override
    public void close() {

    }

    private MapSingleUseObjectEntity getWithExpiration(String key) {
        DefaultModelCriteria<SingleUseObjectValueModel> mcb = criteria();
        mcb = mcb.compare(SingleUseObjectValueModel.SearchableFields.OBJECT_KEY, ModelCriteriaBuilder.Operator.EQ, key);

        MapSingleUseObjectEntity singleUseEntity = singleUseObjectTx.read(withCriteria(mcb)).findFirst().orElse(null);
        if (singleUseEntity != null) {
            if (isExpired(singleUseEntity, false)) {
                singleUseObjectTx.delete(singleUseEntity.getId());
            } else {
                return singleUseEntity;
            }
        }
        return null;
    }
 }
