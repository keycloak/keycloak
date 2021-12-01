/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.models.map.storage.hotRod.common;

import org.infinispan.protostream.GeneratedSchema;
import org.infinispan.protostream.annotations.AutoProtoSchemaBuilder;
import org.keycloak.models.map.storage.hotRod.client.HotRodAttributeEntity;
import org.keycloak.models.map.storage.hotRod.client.HotRodClientEntity;
import org.keycloak.models.map.storage.hotRod.client.HotRodProtocolMapperEntity;

/**
 * @author <a href="mailto:mkanis@redhat.com">Martin Kanis</a>
 */
@AutoProtoSchemaBuilder(
        includeClasses = {
                HotRodAttributeEntity.class,
                HotRodClientEntity.class,
                HotRodProtocolMapperEntity.class,
                HotRodPair.class
        },
        schemaFileName = "KeycloakHotRodMapStorage.proto",
        schemaFilePath = "proto/",
        schemaPackageName = "org.keycloak.models.map.storage.hotrod")
public interface ProtoSchemaInitializer extends GeneratedSchema {
        ProtoSchemaInitializer INSTANCE = new ProtoSchemaInitializerImpl();
}
