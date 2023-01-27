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
package org.keycloak.models.map.storage.file.yaml.parser.map;

import org.keycloak.models.map.storage.file.yaml.parser.YamlContext;
import org.keycloak.models.map.storage.file.yaml.parser.YamlContext.DefaultListContext;
import org.keycloak.models.map.storage.file.yaml.parser.YamlContext.DefaultMapContext;
import java.util.LinkedList;

/**
 * YAML parser context which suitable for properties stored in a {@code Map<String, List<String>>}
 * which accepts
 *
 * @author hmlnarik
 */
public class AttributesLikeYamlContext extends DefaultMapContext {

    /**
     * Returns a YAML attribute-like context where key of each element
     * is stored in YAML file without a given prefix, and in the internal
     * representation each key has that prefix.
     *
     * @param prefix
     * @return
     */
    public static AttributesLikeYamlContext prefixed(String prefix) {
        return new Prefixed(prefix);
    }

    public static DefaultMapContext singletonAttributesMap(String key) {
        return new SingletonAttributesMapYamlContext(key);
    }

    @Override
    public YamlContext<?> getContext(String nameOfSubcontext) {
        // regardless of the key name, the values need to be converted into Set<String> which is the purpose of AttributeValueYamlContext
        return new AttributeValueYamlContext();
    }

    private static class Prefixed extends AttributesLikeYamlContext {

        protected final String prefix;

        public Prefixed(String prefix) {
            this.prefix = prefix;
        }

        @Override
        public void add(String name, Object value) {
            super.add(prefix + name, value);
        }
    }

    private static class SingletonAttributesMapYamlContext extends DefaultMapContext {

        protected final String key;

        public SingletonAttributesMapYamlContext(String key) {
            this.key = key;
        }

        @Override
        public void add(Object value) {
            if (value != null) {
                LinkedList<String> stringList = (LinkedList<String>) getResult().computeIfAbsent(key, s -> new LinkedList<>());
                stringList.add(String.valueOf(value));
            }
        }
    }

    public static class AttributeValueYamlContext extends DefaultListContext {

        @Override
        public void add(Object value) {
            if (value != null) {
                super.add(String.valueOf(value));
            }
        }
    }

}
