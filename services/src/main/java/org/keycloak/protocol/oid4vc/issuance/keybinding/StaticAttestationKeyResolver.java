/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.protocol.oid4vc.issuance.keybinding;

import java.util.Map;

import org.keycloak.jose.jwk.JWK;

import org.jboss.logging.Logger;

/**
 * Simple static implementation of AttestationKeyResolver using an in-memory trusted map.
 *
 * @author <a href="mailto:Rodrick.Awambeng@adorsys.com">Rodrick Awambeng</a>
 */
public class StaticAttestationKeyResolver implements AttestationKeyResolver {
    private static final Logger logger = Logger.getLogger(StaticAttestationKeyResolver.class);
    private final Map<String, JWK> trustedKeys;

    public StaticAttestationKeyResolver(Map<String, JWK> trustedKeys) {
        this.trustedKeys = trustedKeys;
    }

    @Override
    public JWK resolveKey(String kid, Map<String, Object> header, Map<String, Object> payload) {
        JWK key = trustedKeys.get(kid);
        if (key == null) {
            logger.warnf("Key with kid '%s' not found in trusted static key registry", kid);
        }
        return key;
    }
}
