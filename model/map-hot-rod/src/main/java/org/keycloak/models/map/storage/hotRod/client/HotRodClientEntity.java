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
@ProtoDoc("@Indexed")
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

    @ProtoDoc("@Field(index = Index.YES, store = Store.YES)")
    @ProtoField(number = 1)
    public Integer entityVersion = VERSION;

    @ProtoDoc("@Field(index = Index.YES, store = Store.YES)")
    @ProtoField(number = 2)
    public String id;

    @ProtoDoc("@Field(index = Index.YES, store = Store.YES)")
    @ProtoField(number = 3)
    public String realmId;

    @ProtoDoc("@Field(index = Index.YES, store = Store.YES)")
    @ProtoField(number = 4)
    public String clientId;

    /**
     * Lowercase interpretation of {@link #clientId} field. Infinispan doesn't support case-insensitive LIKE for non-analyzed fields.
     * Search on analyzed fields can be case-insensitive (based on used analyzer) but doesn't support ORDER BY analyzed field.
     */
    @ProtoDoc("@Field(index = Index.YES, store = Store.YES)")
    @ProtoField(number = 5)
    public String clientIdLowercase;

    @ProtoField(number = 6)
    public String name;

    @ProtoField(number = 7)
    public String description;

    @ProtoField(number = 8)
    public Set<String> redirectUris;

    @ProtoField(number = 9)
    public Boolean enabled;

    @ProtoField(number = 10)
    public Boolean alwaysDisplayInConsole;

    @ProtoField(number = 11)
    public String clientAuthenticatorType;

    @ProtoField(number = 12)
    public String secret;

    @ProtoField(number = 13)
    public String registrationToken;

    @ProtoField(number = 14)
    public String protocol;

    @ProtoDoc("@Field(index = Index.YES, store = Store.YES)")
    @ProtoField(number = 15)
    public Set<HotRodAttributeEntity> attributes;

    @ProtoField(number = 16)
    public Set<HotRodPair<String, String>> authenticationFlowBindingOverrides;

    @ProtoField(number = 17)
    public Boolean publicClient;

    @ProtoField(number = 18)
    public Boolean fullScopeAllowed;

    @ProtoField(number = 19)
    public Boolean frontchannelLogout;

    @ProtoField(number = 20)
    public Long notBefore;

    @ProtoField(number = 21)
    public Set<String> scope;

    @ProtoField(number = 22)
    public Set<String> webOrigins;

    @ProtoField(number = 23)
    public Set<HotRodProtocolMapperEntity> protocolMappers;

    @ProtoField(number = 24)
    public Set<HotRodPair<String, Boolean>> clientScopes;

    @ProtoDoc("@Field(index = Index.YES, store = Store.YES)")
    @ProtoField(number = 25, collectionImplementation = LinkedList.class)
    public Collection<String> scopeMappings;

    @ProtoField(number = 26)
    public Boolean surrogateAuthRequired;

    @ProtoField(number = 27)
    public String managementUrl;

    @ProtoField(number = 28)
    public String baseUrl;

    @ProtoField(number = 29)
    public Boolean bearerOnly;

    @ProtoField(number = 30)
    public Boolean consentRequired;

    @ProtoField(number = 31)
    public String rootUrl;

    @ProtoField(number = 32)
    public Boolean standardFlowEnabled;

    @ProtoField(number = 33)
    public Boolean implicitFlowEnabled;

    @ProtoField(number = 34)
    public Boolean directAccessGrantsEnabled;

    @ProtoField(number = 35)
    public Boolean serviceAccountsEnabled;

    @ProtoField(number = 36)
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
            entity.clientIdLowercase = clientId == null ? null : clientId.toLowerCase();
        }

        @Override
        public Stream<String> getClientScopes(boolean defaultScope) {
            final Map<String, Boolean> clientScopes = getClientScopes();
            return clientScopes == null ? Stream.empty() : clientScopes.entrySet().stream()
                    .filter(me -> Objects.equals(me.getValue(), defaultScope))
                    .map(Map.Entry::getKey);
        }

        @Override
        public Optional<MapProtocolMapperEntity> getProtocolMapper(String id) {
            Set<MapProtocolMapperEntity> mappers = getProtocolMappers();
            if (mappers == null || mappers.isEmpty()) return Optional.empty();

            return mappers.stream().filter(m -> Objects.equals(m.getId(), id)).findFirst();
        }

        @Override
        public void removeProtocolMapper(String id) {
            HotRodClientEntity entity = getHotRodEntity();
            entity.updated |= entity.protocolMappers != null && entity.protocolMappers.removeIf(m -> Objects.equals(m.id, id));
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
