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

package org.keycloak.tests.utils.admin;

import java.net.URI;

import jakarta.ws.rs.core.UriBuilder;

import org.keycloak.admin.client.resource.AttackDetectionResource;
import org.keycloak.admin.client.resource.AuthenticationManagementResource;
import org.keycloak.admin.client.resource.ClientAttributeCertificateResource;
import org.keycloak.admin.client.resource.ClientInitialAccessResource;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.ClientScopeResource;
import org.keycloak.admin.client.resource.ClientScopesResource;
import org.keycloak.admin.client.resource.ClientsResource;
import org.keycloak.admin.client.resource.ComponentsResource;
import org.keycloak.admin.client.resource.GroupResource;
import org.keycloak.admin.client.resource.GroupsResource;
import org.keycloak.admin.client.resource.IdentityProviderResource;
import org.keycloak.admin.client.resource.IdentityProvidersResource;
import org.keycloak.admin.client.resource.ProtocolMappersResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.RoleByIdResource;
import org.keycloak.admin.client.resource.RoleMappingResource;
import org.keycloak.admin.client.resource.RoleResource;
import org.keycloak.admin.client.resource.RolesResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class AdminEventPaths {

    // REALM

    public static String deleteSessionPath(String userSessionId) {
        URI uri = UriBuilder.fromUri("").path(RealmResource.class, "deleteSession").build(userSessionId);
        return uri.toString();
    }

    public static String defaultGroupPath(String groupId) {
        URI uri = UriBuilder.fromUri("").path(RealmResource.class, "addDefaultGroup").build(groupId);
        return uri.toString();
    }

    public static String defaultDefaultClientScopePath(String clientScopeId) {
        URI uri = UriBuilder.fromUri("").path(RealmResource.class, "addDefaultDefaultClientScope").build(clientScopeId);
        return uri.toString();
    }

    public static String defaultOptionalClientScopePath(String clientScopeId) {
        URI uri = UriBuilder.fromUri("").path(RealmResource.class, "addDefaultOptionalClientScope").build(clientScopeId);
        return uri.toString();
    }

    public static String userProfilePath() {
        URI uri = UriBuilder.fromUri("").path(RealmResource.class, "users")
                .path(UsersResource.class, "userProfile")
                .build();
        return uri.toString();
    }

    // CLIENT RESOURCE

    public static String clientResourcePath(String clientDbId) {
        URI uri = UriBuilder.fromUri("").path(RealmResource.class, "clients").path(ClientsResource.class, "get").build(clientDbId);
        return uri.toString();
    }

    public static String clientRolesResourcePath(String clientDbId) {
        URI uri = UriBuilder.fromUri(clientResourcePath(clientDbId)).path(ClientResource.class, "roles").build();
        return uri.toString();
    }

    public static String clientRoleResourcePath(String clientDbId, String roleName) {
        URI uri = UriBuilder.fromUri(clientRolesResourcePath(clientDbId)).path(RolesResource.class, "get").build(roleName);
        return uri.toString();
    }

    public static String clientRoleResourceCompositesPath(String clientDbId, String roleName) {
        URI uri = UriBuilder.fromUri(clientRoleResourcePath(clientDbId, roleName))
                .path(RoleResource.class, "getRoleComposites").build();
        return uri.toString();
    }

    public static String clientProtocolMappersPath(String clientDbId) {
        URI uri = UriBuilder.fromUri(clientResourcePath(clientDbId))
                .path(ClientResource.class, "getProtocolMappers")
                .build();
        return uri.toString();
    }


    public static String clientProtocolMapperPath(String clientDbId, String protocolMapperId) {
        URI uri = UriBuilder.fromUri(clientProtocolMappersPath(clientDbId))
                .path(ProtocolMappersResource.class, "getMapperById")
                .build(protocolMapperId);
        return uri.toString();
    }

    public static String clientPushRevocationPath(String clientDbId) {
        URI uri = UriBuilder.fromUri(clientResourcePath(clientDbId)).path(ClientResource.class, "pushRevocation").build();
        return uri.toString();
    }

    public static String clientNodesPath(String clientDbId) {
        URI uri = UriBuilder.fromUri(clientResourcePath(clientDbId)).path(ClientResource.class, "registerNode").build();
        return uri.toString();
    }

    public static String clientNodePath(String clientDbId, String node) {
        URI uri = UriBuilder.fromUri(clientResourcePath(clientDbId)).path(ClientResource.class, "unregisterNode").build(node);
        return uri.toString();
    }

    public static String clientTestNodesAvailablePath(String clientDbId) {
        URI uri = UriBuilder.fromUri(clientResourcePath(clientDbId)).path(ClientResource.class, "testNodesAvailable").build();
        return uri.toString();
    }

    public static String clientGenerateSecretPath(String clientDbId) {
        URI uri = UriBuilder.fromUri(clientResourcePath(clientDbId)).path(ClientResource.class, "generateNewSecret").build();
        return uri.toString();
    }

    public static String clientRegenerateRegistrationAccessTokenPath(String clientDbId) {
        URI uri = UriBuilder.fromUri(clientResourcePath(clientDbId)).path(ClientResource.class, "regenerateRegistrationAccessToken").build();
        return uri.toString();
    }

    public static String clientCertificateGenerateSecretPath(String clientDbId, String certificateAttribute) {
        URI uri = UriBuilder.fromUri(clientResourcePath(clientDbId))
                .path(ClientResource.class, "getCertficateResource")
                .path(ClientAttributeCertificateResource.class, "generate")
                .build(certificateAttribute);
        return uri.toString();
    }


    public static String clientScopeMappingsRealmLevelPath(String clientDbId) {
        URI uri = UriBuilder.fromUri(clientResourcePath(clientDbId)).path(ClientResource.class, "getScopeMappings")
                .path(RoleMappingResource.class, "realmLevel")
                .build();
        return uri.toString();
    }

    public static String clientScopeMappingsClientLevelPath(String clientDbId, String clientOwningRoleId) {
        URI uri = UriBuilder.fromUri(clientResourcePath(clientDbId)).path(ClientResource.class, "getScopeMappings")
                .path(RoleMappingResource.class, "clientLevel")
                .build(clientOwningRoleId);
        return uri.toString();
    }



    // CLIENT SCOPES

    public static String clientScopeResourcePath(String clientScopeId) {
        URI uri = UriBuilder.fromUri("").path(RealmResource.class, "clientScopes").path(ClientScopesResource.class, "get").build(clientScopeId);
        return uri.toString();
    }

    public static String clientScopeGenerateAudienceClientScopePath() {
        URI uri = UriBuilder.fromUri("").path(RealmResource.class, "clientScopes").path(ClientScopesResource.class, "generateAudienceClientScope").build();
        return uri.toString();
    }

    public static String clientScopeRoleMappingsRealmLevelPath(String clientScopeDbId) {
        URI uri = UriBuilder.fromUri(clientScopeResourcePath(clientScopeDbId)).path(ClientScopeResource.class, "getScopeMappings")
                .path(RoleMappingResource.class, "realmLevel")
                .build();
        return uri.toString();
    }

    public static String clientScopeRoleMappingsClientLevelPath(String clientScopeDbId, String clientOwningRoleId) {
        URI uri = UriBuilder.fromUri(clientScopeResourcePath(clientScopeDbId)).path(ClientScopeResource.class, "getScopeMappings")
                .path(RoleMappingResource.class, "clientLevel")
                .build(clientOwningRoleId);
        return uri.toString();
    }

    public static String clientScopeProtocolMappersPath(String clientScopeDbId) {
        URI uri = UriBuilder.fromUri(clientScopeResourcePath(clientScopeDbId))
                .path(ClientScopeResource.class, "getProtocolMappers")
                .build();
        return uri.toString();
    }


    public static String clientScopeProtocolMapperPath(String clientScopeDbId, String protocolMapperId) {
        URI uri = UriBuilder.fromUri(clientScopeProtocolMappersPath(clientScopeDbId))
                .path(ProtocolMappersResource.class, "getMapperById")
                .build(protocolMapperId);
        return uri.toString();
    }

    // ROLES

    public static String rolesResourcePath() {
        URI uri = UriBuilder.fromUri("").path(RealmResource.class, "roles").build();
        return uri.toString();
    }

    public static String roleResourcePath(String roleName) {
        URI uri = UriBuilder.fromUri(rolesResourcePath()).path(RolesResource.class, "get").build(roleName);
        return uri.toString();
    }

    public static String roleResourceCompositesPath(String roleName) {
        URI uri = UriBuilder.fromUri(roleResourcePath(roleName)).path(RoleResource.class, "getRoleComposites").build();
        return uri.toString();
    }

    public static String rolesByIdResourcePath() {
        URI uri = UriBuilder.fromUri("").path(RealmResource.class, "rolesById").build();
        return uri.toString();
    }

    public static String roleByIdResourcePath(String roleId) {
        URI uri = UriBuilder.fromUri(rolesByIdResourcePath()).path(RoleByIdResource.class, "getRole").build(roleId);
        return uri.toString();
    }

    public static String roleByIdResourceCompositesPath(String roleId) {
        URI uri = UriBuilder.fromUri(rolesByIdResourcePath()).path(RoleByIdResource.class, "getRoleComposites").build(roleId);
        return uri.toString();
    }

    // USERS

    public static String userResourcePath(String userId) {
        URI uri = UriBuilder.fromUri("").path(RealmResource.class, "users").path(UsersResource.class, "get").build(userId);
        return uri.toString();
    }

    public static String userResetPasswordPath(String userId) {
        URI uri = UriBuilder.fromUri(userResourcePath(userId)).path(UserResource.class, "resetPassword").build(userId);
        return uri.toString();
    }

    public static String userRealmRoleMappingsPath(String userId) {
        URI uri = UriBuilder.fromUri(userResourcePath(userId))
                .path(UserResource.class, "roles")
                .path(RoleMappingResource.class, "realmLevel").build();
        return uri.toString();
    }

    public static String userClientRoleMappingsPath(String userId, String clientDbId) {
        URI uri = UriBuilder.fromUri(userResourcePath(userId))
                .path(UserResource.class, "roles")
                .path(RoleMappingResource.class, "clientLevel").build(clientDbId);
        return uri.toString();
    }

    public static String userFederatedIdentityLink(String userId, String idpAlias) {
        URI uri = UriBuilder.fromUri(userResourcePath(userId))
                .path(UserResource.class, "addFederatedIdentity")
                .build(idpAlias);
        return uri.toString();
    }

    public static String userGroupPath(String userId, String groupId) {
        URI uri = UriBuilder.fromUri(userResourcePath(userId))
                .path(UserResource.class, "joinGroup")
                .build(groupId);
        return uri.toString();
    }

    // IDENTITY PROVIDERS

    public static String identityProvidersPath() {
        URI uri = UriBuilder.fromUri("").path(RealmResource.class, "identityProviders").build();
        return uri.toString();
    }

    public static String identityProviderCreatePath() {
        URI uri = UriBuilder.fromUri(identityProvidersPath()).path(IdentityProvidersResource.class, "create").build();
        return uri.toString();
    }

    public static String identityProviderPath(String idpAlias) {
        URI uri = UriBuilder.fromUri(identityProvidersPath()).path(IdentityProvidersResource.class, "get").build(idpAlias);
        return uri.toString();
    }

    public static String identityProviderMapperPath(String idpAlias, String idpMapperId) {
        URI uri = UriBuilder.fromUri(identityProviderPath(idpAlias)).path(IdentityProviderResource.class, "getMapperById").build(idpMapperId);
        return uri.toString();
    }

    // COMPONENTS
    public static String componentsPath() {
        URI uri = UriBuilder.fromUri("").path(RealmResource.class, "components").build();
        return uri.toString();
    }

    public static String componentPath(String componentId) {
        URI uri = UriBuilder.fromUri(componentsPath()).path(ComponentsResource.class, "component").build(componentId);
        return uri.toString();
    }




    // CLIENT INITIAL ACCESS

    public static String clientInitialAccessPath(String clientInitialAccessId) {
        URI uri = UriBuilder.fromUri("").path(RealmResource.class, "clientInitialAccess")
                .path(ClientInitialAccessResource.class, "delete")
                .build(clientInitialAccessId);
        return uri.toString();
    }

    // GROUPS

    public static String groupsPath() {
        URI uri = UriBuilder.fromUri("").path(RealmResource.class, "groups")
                .build();
        return uri.toString();
    }

    public static String groupPath(String groupId) {
        URI uri = UriBuilder.fromUri(groupsPath()).path(GroupsResource.class, "group")
                .build(groupId);
        return uri.toString();
    }

    public static String groupRolesPath(String groupId) {
        URI uri = UriBuilder.fromUri(groupPath(groupId))
                .path(GroupResource.class, "roles")
                .build();
        return uri.toString();
    }

    public static String groupRolesRealmRolesPath(String groupId) {
        URI uri = UriBuilder.fromUri(groupRolesPath(groupId))
                .path(RoleMappingResource.class, "realmLevel")
                .build();
        return uri.toString();
    }

    public static String groupRolesClientRolesPath(String groupId, String clientDbId) {
        URI uri = UriBuilder.fromUri(groupRolesPath(groupId))
                .path(RoleMappingResource.class, "clientLevel")
                .build(clientDbId);
        return uri.toString();
    }

    public static String groupSubgroupsPath(String groupId) {
        URI uri = UriBuilder.fromUri(groupPath(groupId))
                .path(GroupResource.class, "subGroup")
                .build();
        return uri.toString();
    }


    // AUTHENTICATION FLOWS

    public static String authMgmtBasePath() {
        URI uri = UriBuilder.fromUri("").path(RealmResource.class, "flows")
                .build();
        return uri.toString();
    }

    public static String authFlowsPath() {
        URI uri = UriBuilder.fromUri(authMgmtBasePath()).path(AuthenticationManagementResource.class, "getFlows")
                .build();
        return uri.toString();
    }

    public static String authFlowPath(String flowId) {
        URI uri = UriBuilder.fromUri(authMgmtBasePath()).path(AuthenticationManagementResource.class, "getFlow")
                .build(flowId);
        return uri.toString();
    }

    public static String authCopyFlowPath(String flowAlias) {
        URI uri = UriBuilder.fromUri(authMgmtBasePath()).path(AuthenticationManagementResource.class, "copy")
                .build(flowAlias);
        return uri.toString();
    }

    public static String authEditFlowPath(String flowId) {
        URI uri = UriBuilder.fromUri(authMgmtBasePath()).path(AuthenticationManagementResource.class, "updateFlow")
        .build(flowId);
        return uri.toString();
    }
    public static String authAddExecutionFlowPath(String flowAlias) {
        URI uri = UriBuilder.fromUri(authMgmtBasePath()).path(AuthenticationManagementResource.class, "addExecutionFlow")
                .build(flowAlias);
        return uri.toString();
    }

    public static String authAddExecutionPath(String flowAlias) {
        return authFlowPath(flowAlias) + "/executions/execution";
    }

    public static String authUpdateExecutionPath(String flowAlias) {
        URI uri = UriBuilder.fromUri(authMgmtBasePath()).path(AuthenticationManagementResource.class, "updateExecutions")
                .build(flowAlias);
        return uri.toString();
    }

    public static String authExecutionPath(String executionId) {
        URI uri = UriBuilder.fromUri(authMgmtBasePath()).path(AuthenticationManagementResource.class, "removeExecution")
                .build(executionId);
        return uri.toString();
    }

    public static String authAddExecutionConfigPath(String executionId) {
        URI uri = UriBuilder.fromUri(authMgmtBasePath()).path(AuthenticationManagementResource.class, "newExecutionConfig")
                .build(executionId);
        return uri.toString();
    }

    public static String authExecutionConfigPath(String configId) {
        URI uri = UriBuilder.fromUri(authMgmtBasePath()).path(AuthenticationManagementResource.class, "getAuthenticatorConfig")
                .build(configId);
        return uri.toString();
    }

    public static String authRaiseExecutionPath(String executionId) {
        URI uri = UriBuilder.fromUri(authMgmtBasePath()).path(AuthenticationManagementResource.class, "raisePriority")
                .build(executionId);
        return uri.toString();
    }

    public static String authLowerExecutionPath(String executionId) {
        URI uri = UriBuilder.fromUri(authMgmtBasePath()).path(AuthenticationManagementResource.class, "lowerPriority")
                .build(executionId);
        return uri.toString();
    }

    public static String authRequiredActionPath(String requiredActionAlias) {
        URI uri = UriBuilder.fromUri(authMgmtBasePath()).path(AuthenticationManagementResource.class, "getRequiredAction")
                .build(requiredActionAlias);
        return uri.toString();
    }

    public static String authRequiredActionConfigPath(String requiredActionAlias) {
        URI uri = UriBuilder.fromUri(authMgmtBasePath()).path(AuthenticationManagementResource.class, "getRequiredActionConfig")
                .build(requiredActionAlias);
        return uri.toString();
    }

    public static String authRaiseRequiredActionPath(String requiredActionAlias) {
        URI uri = UriBuilder.fromUri(authMgmtBasePath()).path(AuthenticationManagementResource.class, "raiseRequiredActionPriority")
                .build(requiredActionAlias);
        return uri.toString();
    }

    public static String authLowerRequiredActionPath(String requiredActionAlias) {
        URI uri = UriBuilder.fromUri(authMgmtBasePath()).path(AuthenticationManagementResource.class, "lowerRequiredActionPriority")
                .build(requiredActionAlias);
        return uri.toString();
    }

    // ATTACK DETECTION

    public static String attackDetectionClearBruteForceForUserPath(String username) {
        URI uri = UriBuilder.fromUri("").path(RealmResource.class, "attackDetection")
                .path(AttackDetectionResource.class, "clearBruteForceForUser")
                .build(username);
        return uri.toString();
    }

    public static String attackDetectionClearAllBruteForcePath() {
        URI uri = UriBuilder.fromUri("").path(RealmResource.class, "attackDetection")
                .path(AttackDetectionResource.class, "clearAllBruteForce")
                .build();
        return uri.toString();
    }


}
