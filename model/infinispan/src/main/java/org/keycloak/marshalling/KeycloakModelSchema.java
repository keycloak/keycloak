/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.marshalling;

import java.util.Objects;
import java.util.Optional;

import org.keycloak.cluster.infinispan.LockEntry;
import org.keycloak.cluster.infinispan.LockEntryPredicate;
import org.keycloak.cluster.infinispan.WrapperClusterEvent;
import org.keycloak.component.ComponentModel;
import org.keycloak.jgroups.certificates.ReloadCertificateFunction;
import org.keycloak.keys.infinispan.PublicKeyStorageInvalidationEvent;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.cache.infinispan.ClearCacheEvent;
import org.keycloak.models.cache.infinispan.authorization.events.PermissionTicketRemovedEvent;
import org.keycloak.models.cache.infinispan.authorization.events.PermissionTicketUpdatedEvent;
import org.keycloak.models.cache.infinispan.authorization.events.PolicyRemovedEvent;
import org.keycloak.models.cache.infinispan.authorization.events.PolicyUpdatedEvent;
import org.keycloak.models.cache.infinispan.authorization.events.ResourceRemovedEvent;
import org.keycloak.models.cache.infinispan.authorization.events.ResourceServerRemovedEvent;
import org.keycloak.models.cache.infinispan.authorization.events.ResourceServerUpdatedEvent;
import org.keycloak.models.cache.infinispan.authorization.events.ResourceUpdatedEvent;
import org.keycloak.models.cache.infinispan.authorization.events.ScopeRemovedEvent;
import org.keycloak.models.cache.infinispan.authorization.events.ScopeUpdatedEvent;
import org.keycloak.models.cache.infinispan.authorization.stream.InResourcePredicate;
import org.keycloak.models.cache.infinispan.authorization.stream.InResourceServerPredicate;
import org.keycloak.models.cache.infinispan.authorization.stream.InScopePredicate;
import org.keycloak.models.cache.infinispan.events.AuthenticationSessionAuthNoteUpdateEvent;
import org.keycloak.models.cache.infinispan.events.CacheKeyInvalidatedEvent;
import org.keycloak.models.cache.infinispan.events.ClientAddedEvent;
import org.keycloak.models.cache.infinispan.events.ClientRemovedEvent;
import org.keycloak.models.cache.infinispan.events.ClientScopeAddedEvent;
import org.keycloak.models.cache.infinispan.events.ClientScopeRemovedEvent;
import org.keycloak.models.cache.infinispan.events.ClientUpdatedEvent;
import org.keycloak.models.cache.infinispan.events.GroupAddedEvent;
import org.keycloak.models.cache.infinispan.events.GroupMovedEvent;
import org.keycloak.models.cache.infinispan.events.GroupRemovedEvent;
import org.keycloak.models.cache.infinispan.events.GroupUpdatedEvent;
import org.keycloak.models.cache.infinispan.events.RealmRemovedEvent;
import org.keycloak.models.cache.infinispan.events.RealmUpdatedEvent;
import org.keycloak.models.cache.infinispan.events.RoleAddedEvent;
import org.keycloak.models.cache.infinispan.events.RoleRemovedEvent;
import org.keycloak.models.cache.infinispan.events.RoleUpdatedEvent;
import org.keycloak.models.cache.infinispan.events.UserCacheRealmInvalidationEvent;
import org.keycloak.models.cache.infinispan.events.UserConsentsUpdatedEvent;
import org.keycloak.models.cache.infinispan.events.UserFederationLinkRemovedEvent;
import org.keycloak.models.cache.infinispan.events.UserFederationLinkUpdatedEvent;
import org.keycloak.models.cache.infinispan.events.UserFullInvalidationEvent;
import org.keycloak.models.cache.infinispan.events.UserUpdatedEvent;
import org.keycloak.models.cache.infinispan.stream.GroupListPredicate;
import org.keycloak.models.cache.infinispan.stream.HasRolePredicate;
import org.keycloak.models.cache.infinispan.stream.InClientPredicate;
import org.keycloak.models.cache.infinispan.stream.InGroupPredicate;
import org.keycloak.models.cache.infinispan.stream.InIdentityProviderPredicate;
import org.keycloak.models.cache.infinispan.stream.InRealmPredicate;
import org.keycloak.models.sessions.infinispan.changes.ReplaceFunction;
import org.keycloak.models.sessions.infinispan.changes.SessionEntityWrapper;
import org.keycloak.models.sessions.infinispan.changes.sessions.SessionData;
import org.keycloak.models.sessions.infinispan.entities.AuthenticatedClientSessionEntity;
import org.keycloak.models.sessions.infinispan.entities.AuthenticatedClientSessionStore;
import org.keycloak.models.sessions.infinispan.entities.AuthenticationSessionEntity;
import org.keycloak.models.sessions.infinispan.entities.ClientSessionKey;
import org.keycloak.models.sessions.infinispan.entities.EmbeddedClientSessionKey;
import org.keycloak.models.sessions.infinispan.entities.LoginFailureEntity;
import org.keycloak.models.sessions.infinispan.entities.LoginFailureKey;
import org.keycloak.models.sessions.infinispan.entities.RemoteAuthenticatedClientSessionEntity;
import org.keycloak.models.sessions.infinispan.entities.RemoteUserSessionEntity;
import org.keycloak.models.sessions.infinispan.entities.RootAuthenticationSessionEntity;
import org.keycloak.models.sessions.infinispan.entities.SingleUseObjectValueEntity;
import org.keycloak.models.sessions.infinispan.entities.UserSessionEntity;
import org.keycloak.models.sessions.infinispan.events.RealmRemovedSessionEvent;
import org.keycloak.models.sessions.infinispan.events.RemoveAllUserLoginFailuresEvent;
import org.keycloak.models.sessions.infinispan.events.RemoveUserSessionsEvent;
import org.keycloak.models.sessions.infinispan.stream.AuthClientSessionSetMapper;
import org.keycloak.models.sessions.infinispan.stream.ClientSessionFilterByUser;
import org.keycloak.models.sessions.infinispan.stream.CollectionToStreamMapper;
import org.keycloak.models.sessions.infinispan.stream.GroupAndCountCollectorSupplier;
import org.keycloak.models.sessions.infinispan.stream.MapEntryToKeyMapper;
import org.keycloak.models.sessions.infinispan.stream.RemoveKeyConsumer;
import org.keycloak.models.sessions.infinispan.stream.SessionPredicate;
import org.keycloak.models.sessions.infinispan.stream.SessionUnwrapMapper;
import org.keycloak.models.sessions.infinispan.stream.SessionWrapperPredicate;
import org.keycloak.models.sessions.infinispan.stream.UserSessionPredicate;
import org.keycloak.sessions.CommonClientSessionModel;
import org.keycloak.storage.UserStorageProviderModel;
import org.keycloak.storage.managers.UserStorageSyncManager;

import org.infinispan.protostream.FileDescriptorSource;
import org.infinispan.protostream.GeneratedSchema;
import org.infinispan.protostream.annotations.ProtoSchema;
import org.infinispan.protostream.annotations.ProtoSyntax;
import org.infinispan.protostream.config.Configuration;
import org.infinispan.protostream.descriptors.Descriptor;
import org.infinispan.protostream.descriptors.FileDescriptor;
import org.infinispan.protostream.impl.parser.ProtostreamProtoParser;
import org.infinispan.protostream.types.java.CommonTypes;

@ProtoSchema(
        syntax = ProtoSyntax.PROTO3,
        schemaPackageName = Marshalling.PROTO_SCHEMA_PACKAGE,
        schemaFilePath = "proto/generated",
        allowNullFields = true,

        // common-types for UUID
        dependsOn = CommonTypes.class,

        includeClasses = {
                // Model
                UserSessionModel.State.class,
                CommonClientSessionModel.ExecutionStatus.class,
                ComponentModel.MultiMapEntry.class,
                UserStorageProviderModel.class,
                UserStorageSyncManager.UserStorageProviderClusterEvent.class,

                // clustering.infinispan package
                LockEntry.class,
                LockEntryPredicate.class,
                WrapperClusterEvent.class,
                WrapperClusterEvent.SiteFilter.class,

                // keys.infinispan package
                PublicKeyStorageInvalidationEvent.class,

                // models.cache.infinispan
                ClearCacheEvent.class,

                //models.cache.infinispan.authorization.events package
                PermissionTicketRemovedEvent.class,
                PermissionTicketUpdatedEvent.class,
                PolicyUpdatedEvent.class,
                PolicyRemovedEvent.class,
                ResourceUpdatedEvent.class,
                ResourceRemovedEvent.class,
                ResourceServerUpdatedEvent.class,
                ResourceServerRemovedEvent.class,
                ScopeUpdatedEvent.class,
                ScopeRemovedEvent.class,

                // models.sessions.infinispan.changes package
                SessionEntityWrapper.class,

                // models.sessions.infinispan.changes.sessions package
                SessionData.class,

                // models.cache.infinispan.authorization.stream package
                InResourcePredicate.class,
                InResourceServerPredicate.class,
                InScopePredicate.class,

                // models.sessions.infinispan.events package
                RealmRemovedSessionEvent.class,
                RemoveAllUserLoginFailuresEvent.class,
                RemoveUserSessionsEvent.class,

                // models.sessions.infinispan.stream package
                SessionPredicate.class,
                SessionWrapperPredicate.class,
                UserSessionPredicate.class,

                // models.cache.infinispan.stream package
                GroupListPredicate.class,
                HasRolePredicate.class,
                InClientPredicate.class,
                InGroupPredicate.class,
                InIdentityProviderPredicate.class,
                InRealmPredicate.class,

                // models.cache.infinispan.events package
                AuthenticationSessionAuthNoteUpdateEvent.class,
                CacheKeyInvalidatedEvent.class,
                ClientAddedEvent.class,
                ClientUpdatedEvent.class,
                ClientRemovedEvent.class,
                ClientScopeAddedEvent.class,
                ClientScopeRemovedEvent.class,
                GroupAddedEvent.class,
                GroupMovedEvent.class,
                GroupRemovedEvent.class,
                GroupUpdatedEvent.class,
                RealmUpdatedEvent.class,
                RealmRemovedEvent.class,
                RoleAddedEvent.class,
                RoleUpdatedEvent.class,
                RoleRemovedEvent.class,
                UserCacheRealmInvalidationEvent.class,
                UserConsentsUpdatedEvent.class,
                UserFederationLinkRemovedEvent.class,
                UserFederationLinkUpdatedEvent.class,
                UserFullInvalidationEvent.class,
                UserUpdatedEvent.class,

                // sessions.infinispan.entities package
                AuthenticatedClientSessionStore.class,
                AuthenticatedClientSessionEntity.class,
                AuthenticationSessionEntity.class,
                ClientSessionKey.class,
                EmbeddedClientSessionKey.class,
                LoginFailureEntity.class,
                LoginFailureKey.class,
                RemoteAuthenticatedClientSessionEntity.class,
                RemoteUserSessionEntity.class,
                RootAuthenticationSessionEntity.class,
                SingleUseObjectValueEntity.class,
                UserSessionEntity.class,
                ReplaceFunction.class,

                // sessions.infinispan.stream
                AuthClientSessionSetMapper.class,
                CollectionToStreamMapper.class,
                GroupAndCountCollectorSupplier.class,
                MapEntryToKeyMapper.class,
                SessionUnwrapMapper.class,
                ClientSessionFilterByUser.class,
                RemoveKeyConsumer.class,

                // infinispan.module.certificates
                ReloadCertificateFunction.class,
        }
)
public interface KeycloakModelSchema extends GeneratedSchema {

    KeycloakModelSchema INSTANCE = new KeycloakModelSchemaImpl();

    /**
     * Parses a Google Protocol Buffers schema file.
     */
    static FileDescriptor parseProtoSchema(String fileContent) {
        var files = FileDescriptorSource.fromString("a", fileContent);
        var builder = Configuration.builder();
        KeycloakIndexSchemaUtil.configureAnnotationProcessor(builder);
        var parser = new ProtostreamProtoParser(builder.build());
        return parser.parse(files).get("a");
    }

    /**
     * Finds an entity in a Google Protocol Buffers schema file
     */
    static Optional<Descriptor> findEntity(FileDescriptor fileDescriptor, String entity) {
        return fileDescriptor.getMessageTypes().stream()
                .filter(descriptor -> Objects.equals(entity, descriptor.getFullName()))
                .findFirst();
    }
}
