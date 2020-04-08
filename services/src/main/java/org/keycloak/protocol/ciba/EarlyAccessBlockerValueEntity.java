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
import java.util.HashMap;
import java.util.Map;

public class EarlyAccessBlockerValueEntity {

    private final Map<String, String> notes;

    public EarlyAccessBlockerValueEntity(Map<String, String> notes) {
        this.notes = notes == null ? Collections.emptyMap() : new HashMap<>(notes);
    }

    public Map<String, String> getNotes() {
        return Collections.unmodifiableMap(notes);
    }

    public String getNote(String name) {
        return notes.get(name);
    }
}
