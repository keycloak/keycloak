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

package org.keycloak.keys;

import org.jboss.logging.Logger;
import org.keycloak.common.util.KeyUtils;
import org.keycloak.common.util.Time;
import org.keycloak.crypto.*;

import java.security.KeyPair;
import java.util.Collections;
import java.util.List;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class FailsafeRsaKeyProvider implements KeyProvider {

    private static final Logger logger = Logger.getLogger(FailsafeRsaKeyProvider.class);

    private static KeyWrapper KEY;

    private static long EXPIRES;

    private KeyWrapper key;

    public FailsafeRsaKeyProvider() {
        logger.errorv("No active keys found, using failsafe provider, please login to admin console to add keys. Clustering is not supported.");

        synchronized (FailsafeRsaKeyProvider.class) {
            if (EXPIRES < Time.currentTime()) {
                KEY = createKeyWrapper();
                EXPIRES = Time.currentTime() + 60 * 10;

                if (EXPIRES > 0) {
                    logger.warnv("Keys expired, re-generated kid={0}", KEY.getKid());
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
        KeyPair keyPair = KeyUtils.generateRsaKeyPair(2048);

        KeyWrapper key = new KeyWrapper();

        key.setKid(KeyUtils.createKeyId(keyPair.getPublic()));
        key.setUse(KeyUse.SIG);
        key.setType(KeyType.RSA);
        key.setAlgorithms(Algorithm.RS256, Algorithm.RS384, Algorithm.RS512);
        key.setStatus(KeyStatus.ACTIVE);
        key.setSignKey(keyPair.getPrivate());
        key.setVerifyKey(keyPair.getPublic());

        return key;
    }

}
