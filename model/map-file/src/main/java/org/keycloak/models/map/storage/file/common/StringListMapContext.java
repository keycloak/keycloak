/*
 * Copyright 2023 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.models.map.storage.file.common;

import java.util.Collection;
import java.util.LinkedList;
import java.util.Map;
import java.util.TreeMap;
import org.keycloak.models.map.common.UndefinedValuesUtils;
import org.keycloak.models.map.storage.file.common.BlockContext.DefaultListContext;
import org.keycloak.models.map.storage.file.common.BlockContext.DefaultMapContext;
import org.keycloak.models.map.storage.file.yaml.YamlParser;
import java.util.List;

/**
 * Block context which suitable for properties stored in a {@code Map<String, List<String>>}
 * which accepts string mapping key, and entry value is recognized both as a plain value
 * (converted to string) or a list of values
 *
 * @author hmlnarik
 */
public class StringListMapContext extends DefaultMapContext<Collection<String>> {

    @SuppressWarnings("unchecked")
    public StringListMapContext() {
        super((Class) Collection.class);
    }

    /**
     * Returns a YAML attribute-like context where key of each element
     * is stored in YAML file without a given prefix, and in the internal
     * representation each key has that prefix.
     *
     * @param prefix
     * @return
     */
    public static StringListMapContext prefixed(String prefix) {
        return new Prefixed(prefix);
    }

    @Override
    public AttributeValueYamlContext getContext(String nameOfSubcontext) {
        // regardless of the key name, the values need to be converted into List<String> which is the purpose of AttributeValueYamlContext
        return new AttributeValueYamlContext();
    }

    @Override
    public void writeValue(Map<String, Collection<String>> value, WritingMechanism mech) {
        if (UndefinedValuesUtils.isUndefined(value)) return;
        mech.writeMapping(() -> {
            AttributeValueYamlContext c = getContext(YamlParser.ARRAY_CONTEXT);
            for (Map.Entry<String, Collection<String>> entry : new TreeMap<>(value).entrySet()) {
                Collection<String> attrValues = entry.getValue();
                mech.writePair(entry.getKey(), () -> c.writeValue(attrValues, mech));
            }
        });
    }

    private static class Prefixed extends StringListMapContext {

        protected final String prefix;

        public Prefixed(String prefix) {
            this.prefix = prefix;
        }

        @Override
        public void add(String name, Object value) {
            super.add(prefix + name, value);
        }
    }

    public static class AttributeValueYamlContext extends DefaultListContext<String> {

        public AttributeValueYamlContext() {
            super(String.class);
        }

        @Override
        public void writeValue(Collection<String> value, WritingMechanism mech) {
            if (UndefinedValuesUtils.isUndefined(value)) return;
            if (value.size() == 1) {
                mech.writeObject(value.iterator().next());
            } else {
                //sequence
                super.writeValue(value, mech);
            }
        }

        @Override
        public void add(Object value) {
            if (value != null) {
                super.add(String.valueOf(value));
            }
        }
    }

}
