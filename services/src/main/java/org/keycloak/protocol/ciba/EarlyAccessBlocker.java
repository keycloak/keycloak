/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.protocol.ciba;

import java.util.HashMap;
import java.util.Map;

public class EarlyAccessBlocker {
    private static final String EXPIRATION_NOTE = "exp";

    private final int expiration;

    public EarlyAccessBlocker(int expiration) {
        this.expiration = expiration;
    }

    private EarlyAccessBlocker(Map<String, String> data) {
        expiration = Integer.parseInt(data.get(EXPIRATION_NOTE));
    }

    public static final EarlyAccessBlocker deserializeCode(Map<String, String> data) {
        return new EarlyAccessBlocker(data);
    }

    public Map<String, String> serializeCode() {
        Map<String, String> result = new HashMap<>();
        result.put(EXPIRATION_NOTE, String.valueOf(expiration));
        return result;
    }

    public int getExpiration() {
        return expiration;
    }
}
