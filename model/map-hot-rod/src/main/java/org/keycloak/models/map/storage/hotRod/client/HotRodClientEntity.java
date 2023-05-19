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

package org.keycloak.models.map.storage.hotRod.client;

import org.infinispan.api.annotations.indexing.Basic;
import org.infinispan.api.annotations.indexing.Indexed;
import org.infinispan.api.annotations.indexing.Keyword;
import org.infinispan.protostream.GeneratedSchema;
import org.infinispan.protostream.annotations.AutoProtoSchemaBuilder;
import org.infinispan.protostream.annotations.ProtoDoc;
import org.infinispan.protostream.annotations.ProtoField;
import org.keycloak.models.map.annotations.GenerateHotRodEntityImplementation;
import org.keycloak.models.map.annotations.IgnoreForEntityImplementationGenerator;
import org.keycloak.models.map.storage.hotRod.common.AbstractHotRodEntity;
import org.keycloak.models.map.storage.hotRod.common.CommonPrimitivesProtoSchemaInitializer;
import org.keycloak.models.map.storage.hotRod.common.HotRodAttributeEntity;
import org.keycloak.models.map.storage.hotRod.common.HotRodPair;
import org.keycloak.models.map.client.MapClientEntity;
import org.keycloak.models.map.client.MapProtocolMapperEntity;
import org.keycloak.models.map.storage.hotRod.common.UpdatableHotRodEntityDelegateImpl;

import java.util.Collection;
import java.util.LinkedList;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;


@GenerateHotRodEntityImplementation(
        implementInterface = "org.keycloak.models.map.client.MapClientEntity",
        inherits = "org.keycloak.models.map.storage.hotRod.client.HotRodClientEntity.AbstractHotRodClientEntityDelegate",
        topLevelEntity = true,
        modelClass = "org.keycloak.models.ClientModel"
)
@Indexed
@ProtoDoc("schema-version: " + HotRodClientEntity.VERSION)
public class HotRodClientEntity extends AbstractHotRodEntity {

    @IgnoreForEntityImplementationGenerator
    public static final int VERSION = 1;

    @AutoProtoSchemaBuilder(
            includeClasses = {
                    HotRodClientEntity.class,
                    HotRodProtocolMapperEntity.class,
            },
            schemaFilePath = "proto/",
            schemaPackageName = CommonPrimitivesProtoSchemaInitializer.HOT_ROD_ENTITY_PACKAGE,
            dependsOn = {CommonPrimitivesProtoSchemaInitializer.class}
    )
    public interface HotRodClientEntitySchema extends GeneratedSchema {
        HotRodClientEntitySchema INSTANCE = new HotRodClientEntitySchemaImpl();
    }

    @Basic(projectable = true)
    @ProtoField(number = 1)
    public Integer entityVersion = VERSION;

    @Basic(projectable = true, sortable = true)
    @ProtoField(number = 2)
    public String id;

    @Basic(sortable = true)
    @ProtoField(number = 3)
    public String realmId;

    @Keyword(sortable = true, normalizer = "lowercase")
    @ProtoField(number = 4)
    public String clientId;

    @ProtoField(number = 5)
    public String name;

    @ProtoField(number = 6)
    public String description;

    @ProtoField(number = 7)
    public Set<String> redirectUris;

    @ProtoField(number = 8)
    public Boolean enabled;

    @ProtoField(number = 9)
    public Boolean alwaysDisplayInConsole;

    @ProtoField(number = 10)
    public String clientAuthenticatorType;

    @ProtoField(number = 11)
    public String secret;

    @ProtoField(number = 12)
    public String registrationToken;

    @ProtoField(number = 13)
    public String protocol;

    @Basic(sortable = true)
    @ProtoField(number = 14)
    public Set<HotRodAttributeEntity> attributes;

    @ProtoField(number = 15)
    public Set<HotRodPair<String, String>> authenticationFlowBindingOverrides;

    @ProtoField(number = 16)
    public Boolean publicClient;

    @ProtoField(number = 17)
    public Boolean fullScopeAllowed;

    @ProtoField(number = 18)
    public Boolean frontchannelLogout;

    @ProtoField(number = 19)
    public Long notBefore;

    @ProtoField(number = 20)
    public Set<String> scope;

    @ProtoField(number = 21)
    public Set<String> webOrigins;

    @ProtoField(number = 22)
    public Set<HotRodProtocolMapperEntity> protocolMappers;

    @ProtoField(number = 23)
    public Set<HotRodPair<String, Boolean>> clientScopes;

    @Basic(sortable = true)
    @ProtoField(number = 24, collectionImplementation = LinkedList.class)
    public Collection<String> scopeMappings;

    @ProtoField(number = 25)
    public Boolean surrogateAuthRequired;

    @ProtoField(number = 26)
    public String managementUrl;

    @ProtoField(number = 27)
    public String baseUrl;

    @ProtoField(number = 28)
    public Boolean bearerOnly;

    @ProtoField(number = 29)
    public Boolean consentRequired;

    @ProtoField(number = 30)
    public String rootUrl;

    @ProtoField(number = 31)
    public Boolean standardFlowEnabled;

    @ProtoField(number = 32)
    public Boolean implicitFlowEnabled;

    @ProtoField(number = 33)
    public Boolean directAccessGrantsEnabled;

    @ProtoField(number = 34)
    public Boolean serviceAccountsEnabled;

    @ProtoField(number = 35)
    public Integer nodeReRegistrationTimeout;

    public static abstract class AbstractHotRodClientEntityDelegate extends UpdatableHotRodEntityDelegateImpl<HotRodClientEntity> implements MapClientEntity {

        @Override
        public String getId() {
            return getHotRodEntity().id;
        }

        @Override
        public void setId(String id) {
            HotRodClientEntity entity = getHotRodEntity();
            if (entity.id != null) throw new IllegalStateException("Id cannot be changed");
            entity.id = id;
            entity.updated |= id != null;
        }

        @Override
        public void setClientId(String clientId) {
            HotRodClientEntity entity = getHotRodEntity();
            entity.updated |= ! Objects.equals(entity.clientId, clientId);
            entity.clientId = clientId;
        }

        @Override
        public Stream<String> getClientScopes(boolean defaultScope) {
            final Map<String, Boolean> clientScopes = getClientScopes();
            return clientScopes == null ? Stream.empty() : clientScopes.entrySet().stream()
                    .filter(me -> Objects.equals(me.getValue(), defaultScope))
                    .map(Map.Entry::getKey);
        }
    }

    @Override
    public boolean equals(Object o) {
        return HotRodClientEntityDelegate.entityEquals(this, o);
    }

    @Override
    public int hashCode() {
        return HotRodClientEntityDelegate.entityHashCode(this);
    }
}
