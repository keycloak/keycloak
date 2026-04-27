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

import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;

import org.keycloak.marshalling.Marshalling;
import org.keycloak.models.cache.infinispan.authorization.entities.InResource;
import org.keycloak.models.cache.infinispan.entities.Revisioned;

import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.annotations.ProtoTypeId;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
@ProtoTypeId(Marshalling.IN_RESOURCE_PREDICATE)
public class InResourcePredicate implements Predicate<Map.Entry<String, Revisioned>> {

    private final String resourceId;

    private InResourcePredicate(String resourceId) {
        this.resourceId = Objects.requireNonNull(resourceId);
    }

    @ProtoFactory
    public static InResourcePredicate create(String resourceId) {
        return new InResourcePredicate(resourceId);
    }

    @ProtoField(1)
    String getResourceId() {
        return resourceId;
    }

    @Override
    public boolean test(Map.Entry<String, Revisioned> entry) {
        return entry.getValue() instanceof InResource inResource && resourceId.equals(inResource.getResourceId());
    }

}
