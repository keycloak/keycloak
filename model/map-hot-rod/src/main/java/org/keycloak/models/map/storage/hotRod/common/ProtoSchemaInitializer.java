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
import org.keycloak.models.map.storage.hotRod.authSession.HotRodAuthenticationSessionEntity;
import org.keycloak.models.map.storage.hotRod.authSession.HotRodExecutionStatus;
import org.keycloak.models.map.storage.hotRod.authSession.HotRodRootAuthenticationSessionEntity;
import org.keycloak.models.map.storage.hotRod.authorization.HotRodDecisionStrategy;
import org.keycloak.models.map.storage.hotRod.authorization.HotRodLogic;
import org.keycloak.models.map.storage.hotRod.authorization.HotRodPermissionTicketEntity;
import org.keycloak.models.map.storage.hotRod.authorization.HotRodPolicyEnforcementMode;
import org.keycloak.models.map.storage.hotRod.authorization.HotRodPolicyEntity;
import org.keycloak.models.map.storage.hotRod.authorization.HotRodResourceEntity;
import org.keycloak.models.map.storage.hotRod.authorization.HotRodResourceServerEntity;
import org.keycloak.models.map.storage.hotRod.authorization.HotRodScopeEntity;
import org.keycloak.models.map.storage.hotRod.client.HotRodClientEntity;
import org.keycloak.models.map.storage.hotRod.client.HotRodProtocolMapperEntity;
import org.keycloak.models.map.storage.hotRod.clientscope.HotRodClientScopeEntity;
import org.keycloak.models.map.storage.hotRod.group.HotRodGroupEntity;
import org.keycloak.models.map.storage.hotRod.loginFailure.HotRodUserLoginFailureEntity;
import org.keycloak.models.map.storage.hotRod.realm.HotRodRealmEntity;
import org.keycloak.models.map.storage.hotRod.realm.entity.HotRodAuthenticationExecutionEntity;
import org.keycloak.models.map.storage.hotRod.realm.entity.HotRodAuthenticationFlowEntity;
import org.keycloak.models.map.storage.hotRod.realm.entity.HotRodAuthenticatorConfigEntity;
import org.keycloak.models.map.storage.hotRod.realm.entity.HotRodClientInitialAccessEntity;
import org.keycloak.models.map.storage.hotRod.realm.entity.HotRodComponentEntity;
import org.keycloak.models.map.storage.hotRod.realm.entity.HotRodIdentityProviderEntity;
import org.keycloak.models.map.storage.hotRod.realm.entity.HotRodIdentityProviderMapperEntity;
import org.keycloak.models.map.storage.hotRod.realm.entity.HotRodLocalizationTexts;
import org.keycloak.models.map.storage.hotRod.realm.entity.HotRodOTPPolicyEntity;
import org.keycloak.models.map.storage.hotRod.realm.entity.HotRodRequiredActionProviderEntity;
import org.keycloak.models.map.storage.hotRod.realm.entity.HotRodRequiredCredentialEntity;
import org.keycloak.models.map.storage.hotRod.realm.entity.HotRodRequirement;
import org.keycloak.models.map.storage.hotRod.realm.entity.HotRodWebAuthnPolicyEntity;
import org.keycloak.models.map.storage.hotRod.role.HotRodRoleEntity;
import org.keycloak.models.map.storage.hotRod.user.HotRodUserConsentEntity;
import org.keycloak.models.map.storage.hotRod.user.HotRodUserCredentialEntity;
import org.keycloak.models.map.storage.hotRod.user.HotRodUserEntity;
import org.keycloak.models.map.storage.hotRod.user.HotRodUserFederatedIdentityEntity;
import org.keycloak.models.map.storage.hotRod.userSession.HotRodAuthenticatedClientSessionEntity;
import org.keycloak.models.map.storage.hotRod.userSession.HotRodSessionState;
import org.keycloak.models.map.storage.hotRod.userSession.HotRodUserSessionEntity;

/**
 * @author <a href="mailto:mkanis@redhat.com">Martin Kanis</a>
 */
@AutoProtoSchemaBuilder(
        includeClasses = {
                // Authentication sessions
                HotRodRootAuthenticationSessionEntity.class,
                HotRodAuthenticationSessionEntity.class,
                HotRodExecutionStatus.class,

                // Clients
                HotRodClientEntity.class,
                HotRodProtocolMapperEntity.class,

                // Client scopes
                HotRodClientScopeEntity.class,

                // Groups
                HotRodGroupEntity.class,

                // Roles
                HotRodRoleEntity.class,

                // Users
                HotRodUserEntity.class,
                HotRodUserConsentEntity.class,
                HotRodUserCredentialEntity.class,
                HotRodUserFederatedIdentityEntity.class,

                // Login Failures
                HotRodUserLoginFailureEntity.class,

                // Realms
                HotRodAuthenticationExecutionEntity.class,
                HotRodAuthenticationFlowEntity.class,
                HotRodAuthenticatorConfigEntity.class,
                HotRodClientInitialAccessEntity.class,
                HotRodComponentEntity.class,
                HotRodIdentityProviderEntity.class,
                HotRodIdentityProviderMapperEntity.class,
                HotRodLocalizationTexts.class,
                HotRodOTPPolicyEntity.class,
                HotRodRequiredActionProviderEntity.class,
                HotRodRequiredCredentialEntity.class,
                HotRodRequirement.class,
                HotRodWebAuthnPolicyEntity.class,
                HotRodRealmEntity.class,

                // User sessions
                HotRodUserSessionEntity.class,
                HotRodSessionState.class,

                // Client sessions
                HotRodAuthenticatedClientSessionEntity.class,

                // Authz
                HotRodResourceServerEntity.class,
                HotRodResourceEntity.class,
                HotRodScopeEntity.class,
                HotRodPolicyEntity.class,
                HotRodPermissionTicketEntity.class,
                HotRodDecisionStrategy.class,
                HotRodLogic.class,
                HotRodPolicyEnforcementMode.class,

                // Common
                HotRodPair.class,
                HotRodStringPair.class,
                HotRodAttributeEntity.class,
                HotRodAttributeEntityNonIndexed.class
        },
        schemaFileName = "KeycloakHotRodMapStorage.proto",
        schemaFilePath = "proto/",
        schemaPackageName = ProtoSchemaInitializer.HOT_ROD_ENTITY_PACKAGE)
public interface ProtoSchemaInitializer extends GeneratedSchema {
        String HOT_ROD_ENTITY_PACKAGE = "kc";

        ProtoSchemaInitializer INSTANCE = new ProtoSchemaInitializerImpl();
}
