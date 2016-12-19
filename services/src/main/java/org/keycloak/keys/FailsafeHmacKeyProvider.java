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
import org.keycloak.models.utils.KeycloakModelUtils;

import javax.crypto.SecretKey;
import java.util.Collections;
import java.util.List;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class FailsafeHmacKeyProvider implements HmacKeyProvider {

    private static final Logger logger = Logger.getLogger(FailsafeHmacKeyProvider.class);

    private static String KID;

    private static SecretKey KEY;

    private static long EXPIRES;

    private SecretKey key;

    private String kid;

    public FailsafeHmacKeyProvider() {
        logger.errorv("No active keys found, using failsafe provider, please login to admin console to add keys. Clustering is not supported.");

        synchronized (FailsafeHmacKeyProvider.class) {
            if (EXPIRES < Time.currentTime()) {
                KEY = KeyUtils.loadSecretKey(KeycloakModelUtils.generateSecret(32));
                KID = KeycloakModelUtils.generateId();
                EXPIRES = Time.currentTime() + 60 * 10;

                if (EXPIRES > 0) {
                    logger.warnv("Keys expired, re-generated kid={0}", KID);
                }
            }

            kid = KID;
            key = KEY;
        }
    }

    @Override
    public String getKid() {
        return kid;
    }

    @Override
    public SecretKey getSecretKey() {
        return key;
    }

    @Override
    public SecretKey getSecretKey(String kid) {
        return kid.equals(this.kid) ? key : null;
    }

    @Override
    public List<HmacKeyMetadata> getKeyMetadata() {
        return Collections.emptyList();
    }

    @Override
    public void close() {
    }

}
