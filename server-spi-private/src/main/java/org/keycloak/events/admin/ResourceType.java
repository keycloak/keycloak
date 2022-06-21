/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.events.admin;

/**
 * Represents Keycloak resource types for which {@link AdminEvent AdminEvent's} can be triggered.
 *
 * @author <a href="mailto:thomas.darimont@gmail.com">Thomas Darimont</a>
 */
public interface ResourceType {

    public static String REALM = "REALM";
    public static String REALM_ROLE = "REALM_ROLE";
    public static String REALM_ROLE_MAPPING = "REALM_ROLE_MAPPING";
    public static String REALM_SCOPE_MAPPING = "REALM_SCOPE_MAPPING";
    public static String AUTH_FLOW = "AUTH_FLOW";
    public static String AUTH_EXECUTION_FLOW = "AUTH_EXECUTION_FLOW";
    public static String AUTH_EXECUTION = "AUTH_EXECUTION";
    public static String AUTHENTICATOR_CONFIG = "AUTHENTICATOR_CONFIG";
    public static String REQUIRED_ACTION = "REQUIRED_ACTION";
    public static String IDENTITY_PROVIDER = "IDENTITY_PROVIDER";
    public static String IDENTITY_PROVIDER_MAPPER = "IDENTITY_PROVIDER_MAPPER";
    public static String PROTOCOL_MAPPER = "PROTOCOL_MAPPER";
    public static String USER = "USER";
    public static String USER_LOGIN_FAILURE = "USER_LOGIN_FAILURE";
    public static String USER_SESSION = "USER_SESSION";
    public static String USER_FEDERATION_PROVIDER = "USER_FEDERATION_PROVIDER";
    public static String USER_FEDERATION_MAPPER = "USER_FEDERATION_MAPPER";
    public static String GROUP = "GROUP";
    public static String GROUP_MEMBERSHIP = "GROUP_MEMBERSHIP";
    public static String CLIENT = "CLIENT";
    public static String CLIENT_INITIAL_ACCESS_MODEL = "CLIENT_INITIAL_ACCESS_MODEL";
    public static String CLIENT_ROLE = "CLIENT_ROLE";
    public static String CLIENT_ROLE_MAPPING = "CLIENT_ROLE_MAPPING";
    public static String CLIENT_SCOPE = "CLIENT_SCOPE";
    public static String CLIENT_SCOPE_MAPPING = "CLIENT_SCOPE_MAPPING";
    public static String CLIENT_SCOPE_CLIENT_MAPPING = "CLIENT_SCOPE_CLIENT_MAPPING";
    public static String CLUSTER_NODE = "CLUSTER_NODE";
    public static String COMPONENT = "COMPONENT";
    public static String AUTHORIZATION_RESOURCE_SERVER = "AUTHORIZATION_RESOURCE_SERVER";
    public static String AUTHORIZATION_RESOURCE = "AUTHORIZATION_RESOURCE";
    public static String AUTHORIZATION_SCOPE = "AUTHORIZATION_SCOPE";
    public static String AUTHORIZATION_POLICY = "AUTHORIZATION_POLICY";
}
