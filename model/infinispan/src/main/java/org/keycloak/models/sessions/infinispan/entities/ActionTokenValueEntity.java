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
package org.keycloak.models.sessions.infinispan.entities;

import org.keycloak.models.ActionTokenValueModel;

import java.io.*;
import java.util.*;
import org.infinispan.commons.marshall.Externalizer;
import org.infinispan.commons.marshall.SerializeWith;

/**
 * @author hmlnarik
 */
@SerializeWith(ActionTokenValueEntity.ExternalizerImpl.class)
public class ActionTokenValueEntity implements ActionTokenValueModel {

    private final Map<String, String> notes;

    public ActionTokenValueEntity(Map<String, String> notes) {
        this.notes = notes == null ? Collections.EMPTY_MAP : new HashMap<>(notes);
    }

    @Override
    public Map<String, String> getNotes() {
        return Collections.unmodifiableMap(notes);
    }

    @Override
    public String getNote(String name) {
        return notes.get(name);
    }

    @Override
    public String toString() {
        return String.format("ActionTokenValueEntity [ notes=%s ]", notes.toString());
    }

    public static class ExternalizerImpl implements Externalizer<ActionTokenValueEntity> {

        private static final int VERSION_1 = 1;

        @Override
        public void writeObject(ObjectOutput output, ActionTokenValueEntity t) throws IOException {
            output.writeByte(VERSION_1);

            output.writeBoolean(t.notes.isEmpty());
            if (! t.notes.isEmpty()) {
                output.writeObject(t.notes);
            }
        }

        @Override
        public ActionTokenValueEntity readObject(ObjectInput input) throws IOException, ClassNotFoundException {
            byte version = input.readByte();
            
            if (version != VERSION_1) {
                throw new IOException("Invalid version: " + version);
            }
            boolean notesEmpty = input.readBoolean();

            Map<String, String> notes = notesEmpty ? Collections.EMPTY_MAP : (Map<String, String>) input.readObject();
            
            return new ActionTokenValueEntity(notes);
        }
    }
}
