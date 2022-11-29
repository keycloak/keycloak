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

package org.keycloak.models.cache.infinispan.stream;

import org.keycloak.models.cache.infinispan.entities.GroupNameQuery;
import org.keycloak.models.cache.infinispan.entities.Revisioned;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Serializable;
import java.util.Map;
import java.util.function.Predicate;
import org.infinispan.commons.marshall.Externalizer;
import org.infinispan.commons.marshall.MarshallUtil;
import org.infinispan.commons.marshall.SerializeWith;

@SerializeWith(InGroupPredicate.ExternalizerImpl.class)
public class InGroupPredicate implements Predicate<Map.Entry<String, Revisioned>>, Serializable {
    private String group;

    public static InGroupPredicate create() {
        return new InGroupPredicate();
    }

    public InGroupPredicate group(String id) {
        group = id;
        return this;
    }

    @Override
    public boolean test(Map.Entry<String, Revisioned> entry) {
        Object value = entry.getValue();
        if (value == null) return false;
        if (!(value instanceof GroupNameQuery)) return false;

        return group.equals(((GroupNameQuery)value).getGroupId());
    }

    public static class ExternalizerImpl implements Externalizer<InGroupPredicate> {

        private static final int VERSION_1 = 1;

        @Override
        public void writeObject(ObjectOutput output, InGroupPredicate obj) throws IOException {
            output.writeByte(VERSION_1);

            MarshallUtil.marshallString(obj.group, output);
        }

        @Override
        public InGroupPredicate readObject(ObjectInput input) throws IOException, ClassNotFoundException {
            switch (input.readByte()) {
                case VERSION_1:
                    return readObjectVersion1(input);
                default:
                    throw new IOException("Unknown version");
            }
        }

        public InGroupPredicate readObjectVersion1(ObjectInput input) throws IOException, ClassNotFoundException {
            InGroupPredicate res = new InGroupPredicate();
            res.group = MarshallUtil.unmarshallString(input);

            return res;
        }
    }
}