/*
 * Copyright 2019 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.models.sessions.infinispan;

import org.infinispan.client.hotrod.exceptions.HotRodClientException;
import org.infinispan.commons.api.BasicCache;
import org.jboss.logging.Logger;
import org.keycloak.authorization.policy.evaluation.Realm;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.OAuth2DeviceCodeModel;
import org.keycloak.models.OAuth2DeviceTokenStoreProvider;
import org.keycloak.models.OAuth2DeviceUserCodeModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.sessions.infinispan.entities.ActionTokenValueEntity;

import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * @author <a href="mailto:h2-wada@nri.co.jp">Hiroyuki Wada</a>
 */
public class InfinispanOAuth2DeviceTokenStoreProvider implements OAuth2DeviceTokenStoreProvider {

    public static final Logger logger = Logger.getLogger(InfinispanOAuth2DeviceTokenStoreProvider.class);

    private final Supplier<BasicCache<String, ActionTokenValueEntity>> codeCache;
    private final KeycloakSession session;

    public InfinispanOAuth2DeviceTokenStoreProvider(KeycloakSession session, Supplier<BasicCache<String, ActionTokenValueEntity>> actionKeyCache) {
        this.session = session;
        this.codeCache = actionKeyCache;
    }

    @Override
    public OAuth2DeviceCodeModel getByDeviceCode(RealmModel realm, String deviceCode) {
        try {
            BasicCache<String, ActionTokenValueEntity> cache = codeCache.get();
            ActionTokenValueEntity existing = cache.get(OAuth2DeviceCodeModel.createKey(deviceCode));

            if (existing == null) {
                return null;
            }

            return OAuth2DeviceCodeModel.fromCache(realm, deviceCode, existing.getNotes());
        } catch (HotRodClientException re) {
            // No need to retry. The hotrod (remoteCache) has some retries in itself in case of some random network error happened.
            // In case of lock conflict, we don't want to retry anyway as there was likely an attempt to remove the code from different place.
            if (logger.isDebugEnabled()) {
                logger.debugf(re, "Failed when getting device code %s", deviceCode);
            }

            return null;
        }
    }

    @Override
    public void close() {

    }

    @Override
    public void put(OAuth2DeviceCodeModel deviceCode, OAuth2DeviceUserCodeModel userCode, int lifespanSeconds) {
        ActionTokenValueEntity deviceCodeValue = new ActionTokenValueEntity(deviceCode.toMap());
        ActionTokenValueEntity userCodeValue = new ActionTokenValueEntity(userCode.serializeValue());

        try {
            BasicCache<String, ActionTokenValueEntity> cache = codeCache.get();
            cache.put(deviceCode.serializeKey(), deviceCodeValue, lifespanSeconds, TimeUnit.SECONDS);
            cache.put(userCode.serializeKey(), userCodeValue, lifespanSeconds, TimeUnit.SECONDS);
        } catch (HotRodClientException re) {
            // No need to retry. The hotrod (remoteCache) has some retries in itself in case of some random network error happened.
            if (logger.isDebugEnabled()) {
                logger.debugf(re, "Failed when adding device code %s and user code %s",
                        deviceCode.getDeviceCode(), userCode.getUserCode());
            }
            throw re;
        }
    }

    @Override
    public boolean isPollingAllowed(OAuth2DeviceCodeModel deviceCode) {
        try {
            BasicCache<String, ActionTokenValueEntity> cache = codeCache.get();
            String key = deviceCode.serializePollingKey();
            ActionTokenValueEntity value = new ActionTokenValueEntity(null);
            ActionTokenValueEntity existing = cache.putIfAbsent(key, value, deviceCode.getPollingInterval(), TimeUnit.SECONDS);
            return existing == null;
        } catch (HotRodClientException re) {
            // No need to retry. The hotrod (remoteCache) has some retries in itself in case of some random network error happened.
            // In case of lock conflict, we don't want to retry anyway as there was likely an attempt to remove the code from different place.
            if (logger.isDebugEnabled()) {
                logger.debugf(re, "Failed when putting polling key for device code %s", deviceCode.getDeviceCode());
            }

            return false;
        }
    }

    @Override
    public OAuth2DeviceCodeModel getByUserCode(RealmModel realm, String userCode) {
        try {
            OAuth2DeviceCodeModel deviceCode = findDeviceCodeByUserCode(realm, userCode);
            if (deviceCode == null) {
                return null;
            }

            return deviceCode;
        } catch (HotRodClientException re) {
            // No need to retry. The hotrod (remoteCache) has some retries in itself in case of some random network error happened.
            // In case of lock conflict, we don't want to retry anyway as there was likely an attempt to remove the code from different place.
            if (logger.isDebugEnabled()) {
                logger.debugf(re, "Failed when getting device code by user code %s", userCode);
            }

            return null;
        }
    }

    private OAuth2DeviceCodeModel findDeviceCodeByUserCode(RealmModel realm, String userCode) throws HotRodClientException {
        BasicCache<String, ActionTokenValueEntity> cache = codeCache.get();
        String userCodeKey = OAuth2DeviceUserCodeModel.createKey(realm, userCode);
        ActionTokenValueEntity existing = cache.get(userCodeKey);

        if (existing == null) {
            return null;
        }

        OAuth2DeviceUserCodeModel data = OAuth2DeviceUserCodeModel.fromCache(realm, userCode, existing.getNotes());
        String deviceCode = data.getDeviceCode();

        String deviceCodeKey = OAuth2DeviceCodeModel.createKey(deviceCode);
        ActionTokenValueEntity existingDeviceCode = cache.get(deviceCodeKey);

        if (existingDeviceCode == null) {
            return null;
        }

        return OAuth2DeviceCodeModel.fromCache(realm, deviceCode, existingDeviceCode.getNotes());
    }

    @Override
    public boolean approve(RealmModel realm, String userCode, String userSessionId, Map<String, String> additionalParams) {
        try {
            OAuth2DeviceCodeModel deviceCode = findDeviceCodeByUserCode(realm, userCode);
            if (deviceCode == null) {
                return false;
            }

            OAuth2DeviceCodeModel approved = deviceCode.approve(userSessionId, additionalParams);

            // Update the device code with approved status
            BasicCache<String, ActionTokenValueEntity> cache = codeCache.get();
            cache.replace(approved.serializeKey(), new ActionTokenValueEntity(approved.toMap()));

            return true;
        } catch (HotRodClientException re) {
            // No need to retry. The hotrod (remoteCache) has some retries in itself in case of some random network error happened.
            // In case of lock conflict, we don't want to retry anyway as there was likely an attempt to remove the code from different place.
            if (logger.isDebugEnabled()) {
                logger.debugf(re, "Failed when verifying device user code %s", userCode);
            }

            return false;
        }
    }

    @Override
    public boolean deny(RealmModel realm, String userCode) {
        try {
            OAuth2DeviceCodeModel deviceCode = findDeviceCodeByUserCode(realm, userCode);
            if (deviceCode == null) {
                return false;
            }

            OAuth2DeviceCodeModel denied = deviceCode.deny();

            BasicCache<String, ActionTokenValueEntity> cache = codeCache.get();
            cache.replace(denied.serializeKey(), new ActionTokenValueEntity(denied.toMap()));

            return true;
        } catch (HotRodClientException re) {
            // No need to retry. The hotrod (remoteCache) has some retries in itself in case of some random network error happened.
            // In case of lock conflict, we don't want to retry anyway as there was likely an attempt to remove the code from different place.
            if (logger.isDebugEnabled()) {
                logger.debugf(re, "Failed when denying device user code %s", userCode);
            }

            return false;
        }
    }

    @Override
    public boolean removeDeviceCode(RealmModel realm, String deviceCode) {
        try {
            BasicCache<String, ActionTokenValueEntity> cache = codeCache.get();
            String key = OAuth2DeviceCodeModel.createKey(deviceCode);
            ActionTokenValueEntity existing = cache.remove(key);
            return existing == null ? false : true;
        } catch (HotRodClientException re) {
            // No need to retry. The hotrod (remoteCache) has some retries in itself in case of some random network error happened.
            // In case of lock conflict, we don't want to retry anyway as there was likely an attempt to remove the code from different place.
            if (logger.isDebugEnabled()) {
                logger.debugf(re, "Failed when removing device code %s", deviceCode);
            }

            return false;
        }
    }

    @Override
    public boolean removeUserCode(RealmModel realm, String userCode) {
        try {
            BasicCache<String, ActionTokenValueEntity> cache = codeCache.get();
            String key = OAuth2DeviceUserCodeModel.createKey(realm, userCode);
            ActionTokenValueEntity existing = cache.remove(key);
            return existing == null ? false : true;
        } catch (HotRodClientException re) {
            // No need to retry. The hotrod (remoteCache) has some retries in itself in case of some random network error happened.
            // In case of lock conflict, we don't want to retry anyway as there was likely an attempt to remove the code from different place.
            if (logger.isDebugEnabled()) {
                logger.debugf(re, "Failed when removing user code %s", userCode);
            }

            return false;
        }
    }
}
