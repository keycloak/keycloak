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
import org.keycloak.models.ActionTokenKeyModel;
import org.keycloak.models.ActionTokenStoreProvider;
import org.keycloak.models.ActionTokenValueModel;
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
public class MapSingleUseObjectProvider implements ActionTokenStoreProvider, SingleUseObjectProvider {

    private static final Logger LOG = Logger.getLogger(MapSingleUseObjectProvider.class);
    private final KeycloakSession session;
    protected final MapKeycloakTransaction<MapSingleUseObjectEntity, ActionTokenValueModel> actionTokenStoreTx;

    public MapSingleUseObjectProvider(KeycloakSession session, MapStorage<MapSingleUseObjectEntity, ActionTokenValueModel> storage) {
        this.session = session;
        actionTokenStoreTx = storage.createTransaction(session);

        session.getTransactionManager().enlistAfterCompletion(actionTokenStoreTx);
    }

    private ActionTokenValueModel singleUseEntityToAdapter(MapSingleUseObjectEntity origEntity) {
        if (isExpired(origEntity, false)) {
            actionTokenStoreTx.delete(origEntity.getId());
            return null;
        } else {
            return new MapSingleUseObjectAdapter(session, origEntity);
        }
    }

    @Override
    public void put(ActionTokenKeyModel actionTokenKey, Map<String, String> notes) {
        if (actionTokenKey == null || actionTokenKey.getUserId() == null || actionTokenKey.getActionId() == null || actionTokenKey.getActionVerificationNonce() == null) {
            return;
        }

        LOG.tracef("put(%s, %s, %s)%s", actionTokenKey.getUserId(), actionTokenKey.getActionId(), actionTokenKey.getActionVerificationNonce(), getShortStackTrace());

        DefaultModelCriteria<ActionTokenValueModel> mcb = criteria();
        mcb = mcb.compare(ActionTokenValueModel.SearchableFields.USER_ID, ModelCriteriaBuilder.Operator.EQ, actionTokenKey.getUserId())
                .compare(ActionTokenValueModel.SearchableFields.ACTION_ID, ModelCriteriaBuilder.Operator.EQ, actionTokenKey.getActionId())
                .compare(ActionTokenValueModel.SearchableFields.ACTION_VERIFICATION_NONCE, ModelCriteriaBuilder.Operator.EQ, actionTokenKey.getActionVerificationNonce().toString());

        ActionTokenValueModel existing = actionTokenStoreTx.read(withCriteria(mcb))
                .findFirst().map(this::singleUseEntityToAdapter).orElse(null);

        if (existing == null) {
            MapSingleUseObjectEntity actionTokenStoreEntity = new MapSingleUseObjectEntityImpl();
            actionTokenStoreEntity.setUserId(actionTokenKey.getUserId());
            actionTokenStoreEntity.setActionId(actionTokenKey.getActionId());
            actionTokenStoreEntity.setActionVerificationNonce(actionTokenKey.getActionVerificationNonce().toString());
            actionTokenStoreEntity.setExpiration(TimeAdapter.fromSecondsToMilliseconds(actionTokenKey.getExpiration()));
            actionTokenStoreEntity.setNotes(notes);

            LOG.debugf("Adding used action token to actionTokens cache: %s", actionTokenKey.toString());

            actionTokenStoreTx.create(actionTokenStoreEntity);
        }
    }

    @Override
    public ActionTokenValueModel get(ActionTokenKeyModel key) {
        if (key == null || key.getUserId() == null || key.getActionId() == null || key.getActionVerificationNonce() == null) {
            return null;
        }

        LOG.tracef("get(%s, %s, %s)%s", key.getUserId(), key.getActionId(), key.getActionVerificationNonce(), getShortStackTrace());

        DefaultModelCriteria<ActionTokenValueModel> mcb = criteria();
        mcb = mcb.compare(ActionTokenValueModel.SearchableFields.USER_ID, ModelCriteriaBuilder.Operator.EQ, key.getUserId())
                .compare(ActionTokenValueModel.SearchableFields.ACTION_ID, ModelCriteriaBuilder.Operator.EQ, key.getActionId())
                .compare(ActionTokenValueModel.SearchableFields.ACTION_VERIFICATION_NONCE, ModelCriteriaBuilder.Operator.EQ, key.getActionVerificationNonce().toString());

        return actionTokenStoreTx.read(withCriteria(mcb))
                .findFirst().map(this::singleUseEntityToAdapter).orElse(null);
    }

    @Override
    public ActionTokenValueModel remove(ActionTokenKeyModel key) {
        if (key == null || key.getUserId() == null || key.getActionId() == null || key.getActionVerificationNonce() == null) {
            return null;
        }
        
        LOG.tracef("remove(%s, %s, %s)%s", key.getUserId(), key.getActionId(), key.getActionVerificationNonce(), getShortStackTrace());

        DefaultModelCriteria<ActionTokenValueModel> mcb = criteria();
        mcb = mcb.compare(ActionTokenValueModel.SearchableFields.USER_ID, ModelCriteriaBuilder.Operator.EQ, key.getUserId())
                .compare(ActionTokenValueModel.SearchableFields.ACTION_ID, ModelCriteriaBuilder.Operator.EQ, key.getActionId())
                .compare(ActionTokenValueModel.SearchableFields.ACTION_VERIFICATION_NONCE, ModelCriteriaBuilder.Operator.EQ, key.getActionVerificationNonce().toString());
        MapSingleUseObjectEntity mapSingleUseObjectEntity = actionTokenStoreTx.read(withCriteria(mcb)).findFirst().orElse(null);
        if (mapSingleUseObjectEntity != null) {
            ActionTokenValueModel actionToken = singleUseEntityToAdapter(mapSingleUseObjectEntity);
            if (actionToken != null) {
                actionTokenStoreTx.delete(mapSingleUseObjectEntity.getId());
                return actionToken;
            }
        }

        return null;
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

        actionTokenStoreTx.create(singleUseEntity);
    }

    @Override
    public Map<String, String> get(String key) {
        LOG.tracef("get(%s)%s", key, getShortStackTrace());

        MapSingleUseObjectEntity actionToken = getWithExpiration(key);
        if (actionToken != null) {
            Map<String, String> notes = actionToken.getNotes();
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
            if (actionTokenStoreTx.delete(singleUseEntity.getId())) {
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

            actionTokenStoreTx.create(singleUseEntity);

            return true;
        }
    }

    @Override
    public boolean contains(String key) {
        LOG.tracef("contains(%s)%s", key, getShortStackTrace());

        MapSingleUseObjectEntity actionToken = getWithExpiration(key);

        return actionToken != null;
    }

    @Override
    public void close() {

    }

    private MapSingleUseObjectEntity getWithExpiration(String key) {
        DefaultModelCriteria<ActionTokenValueModel> mcb = criteria();
        mcb = mcb.compare(ActionTokenValueModel.SearchableFields.OBJECT_KEY, ModelCriteriaBuilder.Operator.EQ, key);

        MapSingleUseObjectEntity singleUseEntity = actionTokenStoreTx.read(withCriteria(mcb)).findFirst().orElse(null);
        if (singleUseEntity != null) {
            if (isExpired(singleUseEntity, false)) {
                actionTokenStoreTx.delete(singleUseEntity.getId());
            } else {
                return singleUseEntity;
            }
        }
        return null;
    }
 }
