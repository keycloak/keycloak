/*
 * Copyright 2022 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.models.map.singleUseObject;

import org.keycloak.models.KeycloakSession;

import java.util.Collections;
import java.util.Map;

/**
 * @author <a href="mailto:mkanis@redhat.com">Martin Kanis</a>
 */
public class MapSingleUseObjectAdapter extends AbstractSingleUseObjectModel<MapSingleUseObjectEntity> {

    public MapSingleUseObjectAdapter(KeycloakSession session, MapSingleUseObjectEntity entity) {
        super(session, entity);
    }

    @Override
    public Map<String, String> getNotes() {
        Map<String, String> notes = entity.getNotes();
        return notes == null ? Collections.emptyMap() : Collections.unmodifiableMap(notes);
    }

    @Override
    public String getNote(String name) {
        Map<String, String> notes = entity.getNotes();
        return notes == null ? null : notes.get(name);
    }
}
