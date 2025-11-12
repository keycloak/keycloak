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

import java.util.List;

import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;
import org.infinispan.commons.util.ServiceFinder;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.protostream.SerializationContextInitializer;

/**
 * Ids of the protostream type.
 * <p>
 * Read careful the following warning to ensure compatibility when updating schemas.
 * <p>
 * WARNING! IDs lower or equal than 65535 are reserved for internal Inifinispan classes and cannot be used.
 * WARNING! ID defined in this class must be unique. If one type is removed, its ID must not be reused. You have been
 * warned! The ID identifies the message, and it is stored and used to save space.
 * WARNING! The field IDs cannot be reused as well for the same reason.
 * WARNING! Primitive types cannot be null in proto3 syntax (Integer, String). Take that in consideration.
 * <p>
 * Be Aware of the following default in Proto3 syntax!
 * For strings, the default value is the empty string.
 * For bytes, the default value is empty bytes.
 * For bools, the default value is false.
 * For numeric types, the default value is zero.
 * For enums, the default value is the first defined enum value, which must be 0.
 * For message fields, the field is not set. (null)
 * <p>
 * Docs: <a href="https://protobuf.dev/programming-guides/proto3/">Language Guide (proto 3)</a>
 */
@SuppressWarnings("unused")
public final class Marshalling {

    public static final String PROTO_SCHEMA_PACKAGE = "keycloak";

    private static List<SerializationContextInitializer> SCHEMAS;

    public static List<SerializationContextInitializer> getSchemas() {
        if (SCHEMAS == null) {
            setSchemas(ServiceFinder.load(SerializationContextInitializer.class).stream().toList());
        }
        return SCHEMAS;
    }

    public static void setSchemas(List<SerializationContextInitializer> schemas) {
        SCHEMAS = List.copyOf(schemas);
    }

    private Marshalling() {
    }

    // Model
    // see org.keycloak.models.UserSessionModel.State
    public static final int USER_STATE_ENUM = 65536;
    // see org.keycloak.sessions.CommonClientSessionModel.ExecutionStatus
    public static final int CLIENT_SESSION_EXECUTION_STATUS = 65537;
    // see org.keycloak.component.ComponentModel.MultiMapEntry
    public static final int MULTIMAP_ENTRY = 65538;
    // see org.keycloak.storage.UserStorageProviderModel
    public static final int USER_STORAGE_PROVIDER_MODES = 65539;
    // see org.keycloak.storage.managers.UserStorageSyncManager.UserStorageProviderClusterEvent
    public static final int USER_STORAGE_PROVIDER_CLUSTER_EVENT = 65540;

    // clustering.infinispan package
    public static final int LOCK_ENTRY = 65541;
    public static final int LOCK_ENTRY_PREDICATE = 65542;
    public static final int WRAPPED_CLUSTER_EVENT = 65543;
    public static final int WRAPPED_CLUSTER_EVENT_SITE_FILTER = 65544;

    // keys.infinispan package
    public static final int PUBLIC_KEY_INVALIDATION_EVENT = 65545;

    //models.cache.infinispan.authorization.events package
    public static final int POLICY_UPDATED_EVENT = 65546;
    public static final int POLICY_REMOVED_EVENT = 65547;
    public static final int RESOURCE_UPDATED_EVENT = 65548;
    public static final int RESOURCE_REMOVED_EVENT = 65549;
    public static final int RESOURCE_SERVER_UPDATED_EVENT = 65550;
    public static final int RESOURCE_SERVER_REMOVED_EVENT = 65551;
    public static final int SCOPE_UPDATED_EVENT = 65552;
    public static final int SCOPE_REMOVED_EVENT = 65553;

    // models.sessions.infinispan.initializer package
    public static final int INITIALIZER_STATE = 65554;

    // models.sessions.infinispan.changes package
    public static final int SESSION_ENTITY_WRAPPER = 65555;
    public static final int REPLACE_FUNCTION = 65656;

    // models.sessions.infinispan.changes.sessions package
    public static final int LAST_SESSION_REFRESH_EVENT = 65557;
    public static final int SESSION_DATA = 65558;

    // models.cache.infinispan.authorization.stream package
    public static final int IN_RESOURCE_PREDICATE = 65559;
    public static final int IN_RESOURCE_SERVER_PREDICATE = 65560;
    public static final int IN_SCOPE_PREDICATE = 65561;

    // models.sessions.infinispan.events package
    public static final int REALM_REMOVED_SESSION_EVENT = 65562;
    public static final int REMOVE_ALL_USER_LOGIN_FAILURES_EVENT = 65563;
    public static final int REMOVE_ALL_USER_SESSIONS_EVENT = 65564;

    // models.sessions.infinispan.stream package
    public static final int SESSION_PREDICATE = 65565;
    public static final int SESSION_WRAPPER_PREDICATE = 65566;
    public static final int USER_SESSION_PREDICATE = 65567;

    // models.cache.infinispan.stream package
    public static final int GROUP_LIST_PREDICATE = 65568;
    public static final int HAS_ROLE_PREDICATE = 65569;
    public static final int IN_CLIENT_PREDICATE = 65570;
    public static final int IN_GROUP_PREDICATE = 65571;
    public static final int IN_IDENTITY_PROVIDER_PREDICATE = 65572;
    public static final int IN_REALM_PREDICATE = 65573;

    // models.cache.infinispan.events package
    public static final int AUTHENTICATION_SESSION_AUTH_NOTE_UPDATE_EVENT = 65574;
    public static final int CLIENT_ADDED_EVENT = 65575;
    public static final int CLIENT_UPDATED_EVENT = 65576;
    public static final int CLIENT_REMOVED_EVENT = 65577;
    public static final int CLIENT_SCOPE_ADDED_EVENT = 65578;
    public static final int CLIENT_SCOPE_REMOVED_EVENT = 65579;
    public static final int GROUP_ADDED_EVENT = 65580;
    public static final int GROUP_MOVED_EVENT = 65581;
    public static final int GROUP_REMOVED_EVENT = 65582;
    public static final int GROUP_UPDATED_EVENT = 65583;
    public static final int REALM_UPDATED_EVENT = 65584;
    public static final int REALM_REMOVED_EVENT = 65585;
    public static final int ROLE_ADDED_EVENT = 65586;
    public static final int ROLE_UPDATED_EVENT = 65587;
    public static final int ROLE_REMOVED_EVENT = 65588;
    public static final int USER_CACHE_REALM_INVALIDATION_EVENT = 65589;
    public static final int USER_CONSENTS_UPDATED_EVENT = 65590;
    public static final int USER_FEDERATION_LINK_REMOVED_EVENT = 65591;
    public static final int USER_FEDERATION_LINK_UPDATED_EVENT = 65592;
    public static final int USER_FULL_INVALIDATION_EVENT = 65593;
    public static final int USER_UPDATED_EVENT = 65594;

    // sessions.infinispan.entities package
    public static final int AUTHENTICATED_CLIENT_SESSION_STORE = 65595;
    public static final int AUTHENTICATED_CLIENT_SESSION_ENTITY = 65596;
    public static final int AUTHENTICATION_SESSION_ENTITY = 65597;
    public static final int LOGIN_FAILURE_ENTITY = 65598;
    public static final int LOGIN_FAILURE_KEY = 65599;
    public static final int ROOT_AUTHENTICATION_SESSION_ENTITY = 65600;
    public static final int SINGLE_USE_OBJECT_VALUE_ENTITY = 65601;
    public static final int USER_SESSION_ENTITY = 65602;

    public static final int CACHE_KEY_INVALIDATION_EVENT = 65603;
    public static final int CLEAR_CACHE_EVENT = 65604;

    public static final int REMOTE_USER_SESSION_ENTITY = 65605;

    public static final int CLIENT_SESSION_KEY = 65606;
    public static final int REMOTE_CLIENT_SESSION_ENTITY = 65607;

    public static final int AUTHENTICATION_CLIENT_SESSION_KEY_SET_MAPPER = 65608;
    public static final int COLLECTION_TO_STREAM_MAPPER = 65609;
    public static final int GROUP_AND_COUNT_COLLECTOR_SUPPLIER = 65610;
    public static final int MAP_ENTRY_TO_KEY_FUNCTION = 65611;
    public static final int SESSION_UNWRAP_MAPPER = 65612;

    public static final int PERMISSION_TICKET_REMOVED_EVENT = 65613;
    public static final int PERMISSION_TICKET_UPDATED_EVENT = 65614;

    public static final int RELOAD_CERTIFICATE_FUNCTION = 65615;

    public static final int EMBEDDED_CLIENT_SESSION_KEY = 65616;
    public static final int CLIENT_SESSION_USER_FILTER = 65617;
    public static final int REMOVE_KEY_BI_CONSUMER = 65618;

    public static void configure(GlobalConfigurationBuilder builder) {
        getSchemas().forEach(builder.serialization()::addContextInitializer);
    }

    public static void configure(ConfigurationBuilder builder) {
        getSchemas().forEach(builder::addContextInitializer);
    }

    public static String protoEntity(Class<?> clazz) {
        return PROTO_SCHEMA_PACKAGE + "." + clazz.getSimpleName();
    }
}
