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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import org.keycloak.common.util.Time;

public class EarlyAccessBlockerStore {

    private final static Map<String, EarlyAccessBlockerValueEntity> blockerCache = Collections.synchronizedMap(new LinkedHashMap<>());

    public static void put(String id, int lifespanSeconds, Map<String, String> blockerData) {
        EarlyAccessBlockerValueEntity blockerValue = new EarlyAccessBlockerValueEntity(blockerData);
        blockerCache.put(id, blockerValue);
    }

    public static Map<String, String> remove(String id) {
        EarlyAccessBlockerValueEntity existing = blockerCache.remove(id);
        return existing == null ? null : existing.getNotes();
    }

    public static void sweepExpiredEntries() {
        blockerCache.keySet().stream()
                .filter(i -> Optional.ofNullable(blockerCache.get(i)).isPresent())
                .filter(i -> Optional.ofNullable(blockerCache.get(i).getNotes()).isPresent())
                .filter(i -> Time.currentTime() > EarlyAccessBlocker.deserializeCode(blockerCache.get(i).getNotes()).getExpiration())
                .forEach(i -> blockerCache.remove(i));
    }
}
