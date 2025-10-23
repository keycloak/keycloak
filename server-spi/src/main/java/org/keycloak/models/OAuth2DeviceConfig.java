/*
 *
 *  * Copyright 2021  Red Hat, Inc. and/or its affiliates
 *  * and other contributors as indicated by the @author tags.
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  * http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package org.keycloak.models;

import java.io.Serializable;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public final class OAuth2DeviceConfig implements Serializable {

    // 10 minutes
    public static final int DEFAULT_OAUTH2_DEVICE_CODE_LIFESPAN = 600;
    // 5 seconds
    public static final int DEFAULT_OAUTH2_DEVICE_POLLING_INTERVAL = 5;

    // realm attribute names
    public static String OAUTH2_DEVICE_CODE_LIFESPAN = "oauth2DeviceCodeLifespan";
    public static String OAUTH2_DEVICE_POLLING_INTERVAL = "oauth2DevicePollingInterval";

    // client attribute names
    public static String OAUTH2_DEVICE_CODE_LIFESPAN_PER_CLIENT = "oauth2.device.code.lifespan";
    public static String OAUTH2_DEVICE_POLLING_INTERVAL_PER_CLIENT = "oauth2.device.polling.interval";
    public static final String OAUTH2_DEVICE_AUTHORIZATION_GRANT_ENABLED = "oauth2.device.authorization.grant.enabled";

    private int lifespan = DEFAULT_OAUTH2_DEVICE_CODE_LIFESPAN;
    private int poolingInterval = DEFAULT_OAUTH2_DEVICE_POLLING_INTERVAL;

    public OAuth2DeviceConfig(RealmModel realm) {
        String lifespan = realm.getAttribute(OAUTH2_DEVICE_CODE_LIFESPAN);

        if (lifespan != null && !lifespan.trim().isEmpty()) {
            setOAuth2DeviceCodeLifespan(Integer.parseInt(lifespan));
        }

        String pooling = realm.getAttribute(OAUTH2_DEVICE_POLLING_INTERVAL);

        if (pooling != null && !pooling.trim().isEmpty()) {
            setOAuth2DevicePollingInterval(Integer.parseInt(pooling));
        }
    }

    public int getLifespan() {
        return lifespan;
    }

    public void setOAuth2DeviceCodeLifespan(Integer seconds) {
        setOAuth2DeviceCodeLifespan(null, seconds);
    }

    public void setOAuth2DeviceCodeLifespan(RealmModel realm, Integer seconds) {
        if (seconds == null) {
            seconds = DEFAULT_OAUTH2_DEVICE_CODE_LIFESPAN;
        }
        this.lifespan = seconds;
        persistRealmAttribute(realm, OAUTH2_DEVICE_CODE_LIFESPAN, lifespan);
    }

    public int getPoolingInterval() {
        return poolingInterval;
    }

    public void setOAuth2DevicePollingInterval(Integer seconds) {
        setOAuth2DevicePollingInterval(null, seconds);
    }

    public void setOAuth2DevicePollingInterval(RealmModel realm, Integer seconds) {
        if (seconds == null) {
            seconds = DEFAULT_OAUTH2_DEVICE_POLLING_INTERVAL;
        }
        this.poolingInterval = seconds;

        persistRealmAttribute(realm, OAUTH2_DEVICE_POLLING_INTERVAL, poolingInterval);
    }

    public int getLifespan(ClientModel client) {
        String lifespan = client.getAttribute(OAUTH2_DEVICE_CODE_LIFESPAN_PER_CLIENT);

        if (lifespan != null && !lifespan.trim().isEmpty()) {
            return Integer.parseInt(lifespan);
        }

        return getLifespan();
    }

    public int getPoolingInterval(ClientModel client) {
        String interval = client.getAttribute(OAUTH2_DEVICE_POLLING_INTERVAL_PER_CLIENT);

        if (interval != null && !interval.trim().isEmpty()) {
            return Integer.parseInt(interval);
        }

        return getPoolingInterval();
    }

    public boolean isOAuth2DeviceAuthorizationGrantEnabled(ClientModel client) {
        String enabled = client.getAttribute(OAUTH2_DEVICE_AUTHORIZATION_GRANT_ENABLED);
        return Boolean.parseBoolean(enabled);
    }

    private void persistRealmAttribute(RealmModel realm, String name, Integer value) {
        if (realm != null) {
            realm.setAttribute(name, value);
        }
    }
}
