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
package org.keycloak.models.cache.infinispan.authorization.stream;

import java.io.Serializable;
import java.util.Map;
import java.util.function.Predicate;

import org.keycloak.models.cache.infinispan.authorization.entities.InResource;
import org.keycloak.models.cache.infinispan.entities.Revisioned;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class InResourcePredicate implements Predicate<Map.Entry<String, Revisioned>>, Serializable {

    private String resourceId;

    public static InResourcePredicate create() {
        return new InResourcePredicate();
    }

    public InResourcePredicate resource(String id) {
        resourceId = id;
        return this;
    }

    @Override
    public boolean test(Map.Entry<String, Revisioned> entry) {
        Object value = entry.getValue();
        if (value == null) return false;
        if (!(value instanceof InResource)) return false;

        return resourceId.equals(((InResource)value).getResourceId());
    }
}
