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
package org.keycloak.keys.loader;

import org.keycloak.common.util.PemUtils;
import org.keycloak.keys.PublicKeyLoader;

import java.security.PublicKey;
import java.util.*;

/**
 *
 * @author hmlnarik
 */
public class HardcodedPublicKeyLoader implements PublicKeyLoader {

    private final String kid;
    private final String pem;

    public HardcodedPublicKeyLoader(String kid, String pem) {
        this.kid = kid;
        this.pem = pem;
    }

    @Override
    public Map<String, PublicKey> loadKeys() throws Exception {
        return Collections.unmodifiableMap(Collections.singletonMap(kid, getSavedPublicKey()));
    }

    protected PublicKey getSavedPublicKey() {
        if (pem != null && ! pem.trim().equals("")) {
            return PemUtils.decodePublicKey(pem);
        } else {
            return null;
        }
    }
}
