/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.keys;

import org.jboss.logging.Logger;
import org.keycloak.common.util.KeyUtils;
import org.keycloak.common.util.Time;
import org.keycloak.crypto.JavaAlgorithm;
import org.keycloak.crypto.KeyStatus;
import org.keycloak.crypto.KeyUse;
import org.keycloak.crypto.KeyWrapper;
import org.keycloak.models.utils.KeycloakModelUtils;

import javax.crypto.SecretKey;
import java.util.Collections;
import java.util.List;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public abstract class FailsafeSecretKeyProvider implements KeyProvider {

    private static KeyWrapper KEY;

    private static long EXPIRES;

    private KeyWrapper key;

    public FailsafeSecretKeyProvider() {
        logger().errorv("No active keys found, using failsafe provider, please login to admin console to add keys. Clustering is not supported.");

        synchronized (FailsafeHmacKeyProvider.class) {
            if (EXPIRES < Time.currentTime()) {
                KEY = createKeyWrapper();
                EXPIRES = Time.currentTime() + 60 * 10;

                if (EXPIRES > 0) {
                    logger().warnv("Keys expired, re-generated kid={0}", KEY.getKid());
                }
            }

            key = KEY;
        }
    }

    @Override
    public List<KeyWrapper> getKeys() {
        return Collections.singletonList(key);
    }

    private KeyWrapper createKeyWrapper() {
        SecretKey secretKey = KeyUtils.loadSecretKey(KeycloakModelUtils.generateSecret(32), JavaAlgorithm.getJavaAlgorithm(getAlgorithm()));

        KeyWrapper key = new KeyWrapper();

        key.setKid(KeycloakModelUtils.generateId());
        key.setUse(getUse());
        key.setType(getType());
        key.setAlgorithms(getAlgorithm());
        key.setStatus(KeyStatus.ACTIVE);
        key.setSecretKey(secretKey);

        return key;
    }

    protected abstract KeyUse getUse();

    protected abstract String getType();

    protected abstract String getAlgorithm();

    protected abstract Logger logger();
}
