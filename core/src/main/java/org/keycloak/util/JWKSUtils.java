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

package org.keycloak.util;

import org.jboss.logging.Logger;
import org.keycloak.crypto.KeyUse;
import org.keycloak.crypto.KeyWrapper;
import org.keycloak.crypto.PublicKeysWrapper;
import org.keycloak.jose.jwk.JSONWebKeySet;
import org.keycloak.jose.jwk.JWK;
import org.keycloak.jose.jwk.JWKParser;

import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class JWKSUtils {

    private static final Logger logger = Logger.getLogger(JWKSUtils.class.getName());

    /**
     * @deprecated Use {@link #getKeyWrappersForUse(JSONWebKeySet, JWK.Use)}
     **/
    @Deprecated
    public static Map<String, PublicKey> getKeysForUse(JSONWebKeySet keySet, JWK.Use requestedUse) {
        return getKeyWrappersForUse(keySet, requestedUse).getKeys()
                .stream()
                .collect(Collectors.toMap(KeyWrapper::getKid, keyWrapper -> (PublicKey) keyWrapper.getPublicKey()));
    }

    public static PublicKeysWrapper getKeyWrappersForUse(JSONWebKeySet keySet, JWK.Use requestedUse) {
        List<KeyWrapper> result = new ArrayList<>();
        for (JWK jwk : keySet.getKeys()) {
            JWKParser parser = JWKParser.create(jwk);
            if (jwk.getPublicKeyUse() == null) {
                logger.debugf("Ignoring JWK key '%s'. Missing required field 'use'.", jwk.getKeyId());
            } else if (requestedUse.asString().equals(jwk.getPublicKeyUse()) && parser.isKeyTypeSupported(jwk.getKeyType())) {
                KeyWrapper keyWrapper = new KeyWrapper();
                keyWrapper.setKid(jwk.getKeyId());
                if (jwk.getAlgorithm() != null) {
                    keyWrapper.setAlgorithm(jwk.getAlgorithm());
                }
                keyWrapper.setType(jwk.getKeyType());
                keyWrapper.setUse(getKeyUse(jwk.getPublicKeyUse()));
                keyWrapper.setPublicKey(parser.toPublicKey());
                result.add(keyWrapper);
            }
        }
        return new PublicKeysWrapper(result);
    }

    private static KeyUse getKeyUse(String keyUse) {
        switch (keyUse) {
            case "sig" : 
                return KeyUse.SIG;
            case "enc" : 
                return KeyUse.ENC;
            default :
                return null;
        }
    }

    public static JWK getKeyForUse(JSONWebKeySet keySet, JWK.Use requestedUse) {
        for (JWK jwk : keySet.getKeys()) {
            JWKParser parser = JWKParser.create(jwk);
            if (jwk.getPublicKeyUse() == null) {
                logger.debugf("Ignoring JWK key '%s'. Missing required field 'use'.", jwk.getKeyId());
            } else if (requestedUse.asString().equals(parser.getJwk().getPublicKeyUse()) && parser.isKeyTypeSupported(jwk.getKeyType())) {
                return jwk;
            }
        }

        return null;
    }
}
