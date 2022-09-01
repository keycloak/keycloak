/*
 * Copyright 2022 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.models.map.storage.hotRod.realm;

import org.infinispan.protostream.GeneratedSchema;
import org.infinispan.protostream.annotations.AutoProtoSchemaBuilder;
import org.infinispan.protostream.annotations.ProtoDoc;
import org.infinispan.protostream.annotations.ProtoField;
import org.keycloak.models.map.annotations.GenerateHotRodEntityImplementation;
import org.keycloak.models.map.annotations.IgnoreForEntityImplementationGenerator;
import org.keycloak.models.map.common.UpdatableEntity;
import org.keycloak.models.map.realm.MapRealmEntity;
import org.keycloak.models.map.realm.entity.MapAuthenticationExecutionEntity;
import org.keycloak.models.map.realm.entity.MapAuthenticationFlowEntity;
import org.keycloak.models.map.realm.entity.MapAuthenticatorConfigEntity;
import org.keycloak.models.map.realm.entity.MapClientInitialAccessEntity;
import org.keycloak.models.map.realm.entity.MapComponentEntity;
import org.keycloak.models.map.realm.entity.MapIdentityProviderEntity;
import org.keycloak.models.map.realm.entity.MapIdentityProviderMapperEntity;
import org.keycloak.models.map.realm.entity.MapOTPPolicyEntity;
import org.keycloak.models.map.realm.entity.MapRequiredActionProviderEntity;
import org.keycloak.models.map.realm.entity.MapRequiredCredentialEntity;
import org.keycloak.models.map.realm.entity.MapWebAuthnPolicyEntity;
import org.keycloak.models.map.storage.hotRod.common.AbstractHotRodEntity;
import org.keycloak.models.map.storage.hotRod.common.CommonPrimitivesProtoSchemaInitializer;
import org.keycloak.models.map.storage.hotRod.common.HotRodAttributeEntityNonIndexed;
import org.keycloak.models.map.storage.hotRod.common.HotRodPair;
import org.keycloak.models.map.storage.hotRod.common.UpdatableHotRodEntityDelegateImpl;
import org.keycloak.models.map.storage.hotRod.realm.entity.HotRodAuthenticationExecutionEntity;
import org.keycloak.models.map.storage.hotRod.realm.entity.HotRodAuthenticationExecutionEntityDelegate;
import org.keycloak.models.map.storage.hotRod.realm.entity.HotRodAuthenticationFlowEntity;
import org.keycloak.models.map.storage.hotRod.realm.entity.HotRodAuthenticationFlowEntityDelegate;
import org.keycloak.models.map.storage.hotRod.realm.entity.HotRodAuthenticatorConfigEntity;
import org.keycloak.models.map.storage.hotRod.realm.entity.HotRodAuthenticatorConfigEntityDelegate;
import org.keycloak.models.map.storage.hotRod.realm.entity.HotRodClientInitialAccessEntity;
import org.keycloak.models.map.storage.hotRod.realm.entity.HotRodClientInitialAccessEntityDelegate;
import org.keycloak.models.map.storage.hotRod.realm.entity.HotRodComponentEntity;
import org.keycloak.models.map.storage.hotRod.realm.entity.HotRodComponentEntityDelegate;
import org.keycloak.models.map.storage.hotRod.realm.entity.HotRodIdentityProviderEntity;
import org.keycloak.models.map.storage.hotRod.realm.entity.HotRodIdentityProviderMapperEntity;
import org.keycloak.models.map.storage.hotRod.realm.entity.HotRodIdentityProviderMapperEntityDelegate;
import org.keycloak.models.map.storage.hotRod.realm.entity.HotRodLocalizationTexts;
import org.keycloak.models.map.storage.hotRod.realm.entity.HotRodOTPPolicyEntity;
import org.keycloak.models.map.storage.hotRod.realm.entity.HotRodRequiredActionProviderEntity;
import org.keycloak.models.map.storage.hotRod.realm.entity.HotRodRequiredActionProviderEntityDelegate;
import org.keycloak.models.map.storage.hotRod.realm.entity.HotRodRequiredCredentialEntity;
import org.keycloak.models.map.storage.hotRod.realm.entity.HotRodWebAuthnPolicyEntity;

import java.util.Collections;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.keycloak.models.map.common.ExpirationUtils.isExpired;

@GenerateHotRodEntityImplementation(
        implementInterface = "org.keycloak.models.map.realm.MapRealmEntity",
        inherits = "org.keycloak.models.map.storage.hotRod.realm.HotRodRealmEntity.AbstractHotRodRealmEntityDelegate",
        topLevelEntity = true,
        modelClass = "org.keycloak.models.RealmModel"
)
@ProtoDoc("@Indexed")
@ProtoDoc("schema-version: " + HotRodRealmEntity.VERSION)
public class HotRodRealmEntity extends AbstractHotRodEntity {

    @IgnoreForEntityImplementationGenerator
    public static final int VERSION = 1;

    @AutoProtoSchemaBuilder(
            includeClasses = {
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
                    HotRodWebAuthnPolicyEntity.class,
                    HotRodRealmEntity.class
            },
            schemaFilePath = "proto/",
            schemaPackageName = CommonPrimitivesProtoSchemaInitializer.HOT_ROD_ENTITY_PACKAGE,
            dependsOn = {CommonPrimitivesProtoSchemaInitializer.class}
    )
    public interface HotRodRealmEntitySchema extends GeneratedSchema {
        HotRodRealmEntitySchema INSTANCE = new HotRodRealmEntitySchemaImpl();
    }

    @ProtoDoc("@Field(index = Index.YES, store = Store.YES)")
    @ProtoField(number = 1)
    public Integer entityVersion = VERSION;

    @ProtoField(number = 2)
    public String id;

    @ProtoDoc("@Field(index = Index.YES, store = Store.YES)")
    @ProtoField(number = 3)
    public String name;

    @ProtoField(number = 4)
    public Boolean adminEventsDetailsEnabled;
    @ProtoField(number = 5)
    public Boolean adminEventsEnabled;
    @ProtoField(number = 6)
    public Boolean allowUserManagedAccess;
    @ProtoField(number = 7)
    public Boolean duplicateEmailsAllowed;
    @ProtoField(number = 8)
    public Boolean editUsernameAllowed;
    @ProtoField(number = 9)
    public Boolean enabled;
    @ProtoField(number = 10)
    public Boolean eventsEnabled;
    @ProtoField(number = 11)
    public Boolean internationalizationEnabled;
    @ProtoField(number = 12)
    public Boolean loginWithEmailAllowed;
    @ProtoField(number = 13)
    public Boolean offlineSessionMaxLifespanEnabled;
    @ProtoField(number = 14)
    public Boolean registrationAllowed;
    @ProtoField(number = 15)
    public Boolean registrationEmailAsUsername;
    @ProtoField(number = 16)
    public Boolean rememberMe;
    @ProtoField(number = 17)
    public Boolean resetPasswordAllowed;
    @ProtoField(number = 18)
    public Boolean revokeRefreshToken;
    @ProtoField(number = 19)
    public Boolean verifyEmail;
    @ProtoField(number = 20)
    public Integer accessCodeLifespan;
    @ProtoField(number = 21)
    public Integer accessCodeLifespanLogin;
    @ProtoField(number = 22)
    public Integer accessCodeLifespanUserAction;
    @ProtoField(number = 23)
    public Integer accessTokenLifespan;
    @ProtoField(number = 24)
    public Integer accessTokenLifespanForImplicitFlow;
    @ProtoField(number = 25)
    public Integer actionTokenGeneratedByAdminLifespan;
    @ProtoField(number = 26)
    public Integer clientOfflineSessionIdleTimeout;
    @ProtoField(number = 27)
    public Integer clientOfflineSessionMaxLifespan;
    @ProtoField(number = 28)
    public Integer clientSessionIdleTimeout;
    @ProtoField(number = 29)
    public Integer clientSessionMaxLifespan;
    @ProtoField(number = 30)
    public Long notBefore;
    @ProtoField(number = 31)
    public Integer offlineSessionIdleTimeout;
    @ProtoField(number = 32)
    public Integer offlineSessionMaxLifespan;
    @ProtoField(number = 33)
    public Integer refreshTokenMaxReuse;
    @ProtoField(number = 34)
    public Integer ssoSessionIdleTimeout;
    @ProtoField(number = 35)
    public Integer ssoSessionIdleTimeoutRememberMe;
    @ProtoField(number = 36)
    public Integer ssoSessionMaxLifespan;
    @ProtoField(number = 37)
    public Integer ssoSessionMaxLifespanRememberMe;
    @ProtoField(number = 38)
    public Long eventsExpiration;
    @ProtoField(number = 39)
    public HotRodOTPPolicyEntity oTPPolicy;
    @ProtoField(number = 40)
    public HotRodWebAuthnPolicyEntity webAuthnPolicy;
    @ProtoField(number = 41)
    public HotRodWebAuthnPolicyEntity webAuthnPolicyPasswordless;
    @ProtoField(number = 42)
    public String accountTheme;
    @ProtoField(number = 43)
    public String adminTheme;
    @ProtoField(number = 44)
    public String browserFlow;
    @ProtoField(number = 45)
    public String clientAuthenticationFlow;
    @ProtoField(number = 46)
    public String defaultLocale;
    @ProtoField(number = 47)
    public String defaultRoleId;
    @ProtoField(number = 48)
    public String directGrantFlow;
    @ProtoField(number = 49)
    public String displayName;
    @ProtoField(number = 50)
    public String displayNameHtml;
    @ProtoField(number = 51)
    public String dockerAuthenticationFlow;
    @ProtoField(number = 52)
    public String emailTheme;
    @ProtoField(number = 53)
    public String loginTheme;
    @ProtoField(number = 54)
    public String masterAdminClient;
    @ProtoField(number = 55)
    public String passwordPolicy;
    @ProtoField(number = 56)
    public String registrationFlow;
    @ProtoField(number = 57)
    public String resetCredentialsFlow;
    @ProtoField(number = 58)
    public String sslRequired;
    @ProtoField(number = 59)
    public Set<HotRodAttributeEntityNonIndexed> attributes;
    @ProtoField(number = 60)
    public Set<HotRodLocalizationTexts> localizationTexts;
    @ProtoField(number = 61)
    public Set<HotRodPair<String, String>> browserSecurityHeaders;
    @ProtoField(number = 62)
    public Set<HotRodPair<String, String>> smtpConfig;
    @ProtoField(number = 63)
    public Set<HotRodAuthenticationExecutionEntity> authenticationExecutions;
    @ProtoField(number = 64)
    public Set<HotRodAuthenticationFlowEntity> authenticationFlows;
    @ProtoField(number = 65)
    public Set<HotRodAuthenticatorConfigEntity> authenticatorConfigs;

    @ProtoDoc("@Field(index = Index.YES, store = Store.YES)")
    @ProtoField(number = 66)
    public Set<HotRodClientInitialAccessEntity> clientInitialAccesses;

    @ProtoDoc("@Field(index = Index.YES, store = Store.YES)")
    @ProtoField(number = 67)
    public Set<HotRodComponentEntity> components;

    @ProtoField(number = 68)
    public Set<HotRodIdentityProviderEntity> identityProviders;
    @ProtoField(number = 69)
    public Set<HotRodIdentityProviderMapperEntity> identityProviderMappers;
    @ProtoField(number = 70)
    public Set<HotRodRequiredActionProviderEntity> requiredActionProviders;
    @ProtoField(number = 71)
    public Set<HotRodRequiredCredentialEntity> requiredCredentials;
    @ProtoField(number = 72)
    public Set<String> defaultClientScopeIds;
    @ProtoField(number = 73)
    public Set<String> defaultGroupIds;
    @ProtoField(number = 74)
    public Set<String> enabledEventTypes;
    @ProtoField(number = 75)
    public Set<String> eventsListeners;
    @ProtoField(number = 76)
    public Set<String> optionalClientScopeIds;
    @ProtoField(number = 77)
    public Set<String> supportedLocales;


    public static abstract class AbstractHotRodRealmEntityDelegate extends UpdatableHotRodEntityDelegateImpl<HotRodRealmEntity> implements MapRealmEntity {

        @Override
        public String getId() {
            return getHotRodEntity().id;
        }

        @Override
        public void setId(String id) {
            HotRodRealmEntity entity = getHotRodEntity();
            if (entity.id != null) throw new IllegalStateException("Id cannot be changed");
            entity.id = id;
            entity.updated |= id != null;
        }

        @Override
        public boolean isUpdated() {
            return getHotRodEntity().updated
                    || Optional.ofNullable(getAuthenticationExecutions()).orElseGet(Collections::emptySet).stream().anyMatch(MapAuthenticationExecutionEntity::isUpdated)
                    || Optional.ofNullable(getAuthenticationFlows()).orElseGet(Collections::emptySet).stream().anyMatch(MapAuthenticationFlowEntity::isUpdated)
                    || Optional.ofNullable(getAuthenticatorConfigs()).orElseGet(Collections::emptySet).stream().anyMatch(MapAuthenticatorConfigEntity::isUpdated)
                    || Optional.ofNullable(getClientInitialAccesses()).orElseGet(Collections::emptySet).stream().anyMatch(MapClientInitialAccessEntity::isUpdated)
                    || Optional.ofNullable(getComponents()).orElseGet(Collections::emptySet).stream().anyMatch(MapComponentEntity::isUpdated)
                    || Optional.ofNullable(getIdentityProviders()).orElseGet(Collections::emptySet).stream().anyMatch(MapIdentityProviderEntity::isUpdated)
                    || Optional.ofNullable(getIdentityProviderMappers()).orElseGet(Collections::emptySet).stream().anyMatch(MapIdentityProviderMapperEntity::isUpdated)
                    || Optional.ofNullable(getRequiredActionProviders()).orElseGet(Collections::emptySet).stream().anyMatch(MapRequiredActionProviderEntity::isUpdated)
                    || Optional.ofNullable(getRequiredCredentials()).orElseGet(Collections::emptySet).stream().anyMatch(MapRequiredCredentialEntity::isUpdated)
                    || Optional.ofNullable(getOTPPolicy()).map(MapOTPPolicyEntity::isUpdated).orElse(false)
                    || Optional.ofNullable(getWebAuthnPolicy()).map(MapWebAuthnPolicyEntity::isUpdated).orElse(false)
                    || Optional.ofNullable(getWebAuthnPolicyPasswordless()).map(MapWebAuthnPolicyEntity::isUpdated).orElse(false);
        }

        @Override
        public void clearUpdatedFlag() {
            getHotRodEntity().updated = false;
            Optional.ofNullable(getAuthenticationExecutions()).orElseGet(Collections::emptySet).forEach(UpdatableEntity::clearUpdatedFlag);
            Optional.ofNullable(getAuthenticationFlows()).orElseGet(Collections::emptySet).forEach(UpdatableEntity::clearUpdatedFlag);
            Optional.ofNullable(getAuthenticatorConfigs()).orElseGet(Collections::emptySet).forEach(UpdatableEntity::clearUpdatedFlag);
            Optional.ofNullable(getClientInitialAccesses()).orElseGet(Collections::emptySet).forEach(UpdatableEntity::clearUpdatedFlag);
            Optional.ofNullable(getComponents()).orElseGet(Collections::emptySet).forEach(UpdatableEntity::clearUpdatedFlag);
            Optional.ofNullable(getIdentityProviders()).orElseGet(Collections::emptySet).forEach(UpdatableEntity::clearUpdatedFlag);
            Optional.ofNullable(getIdentityProviderMappers()).orElseGet(Collections::emptySet).forEach(UpdatableEntity::clearUpdatedFlag);
            Optional.ofNullable(getRequiredActionProviders()).orElseGet(Collections::emptySet).forEach(UpdatableEntity::clearUpdatedFlag);
            Optional.ofNullable(getRequiredCredentials()).orElseGet(Collections::emptySet).forEach(UpdatableEntity::clearUpdatedFlag);
            Optional.ofNullable(getOTPPolicy()).ifPresent(UpdatableEntity::clearUpdatedFlag);
            Optional.ofNullable(getWebAuthnPolicy()).ifPresent(UpdatableEntity::clearUpdatedFlag);
            Optional.ofNullable(getWebAuthnPolicyPasswordless()).ifPresent(UpdatableEntity::clearUpdatedFlag);
        }

        @Override
        public Optional<MapComponentEntity> getComponent(String id) {
            Set<HotRodComponentEntity> set = getHotRodEntity().components;
            if (set == null || set.isEmpty()) return Optional.empty();

            return set.stream().filter(ob -> Objects.equals(ob.id, id)).findFirst().map(HotRodComponentEntityDelegate::new);
        }

        @Override
        public Boolean removeComponent(String componentId) {
            Set<HotRodComponentEntity> set = getHotRodEntity().components;
            boolean removed = set != null && set.removeIf(ob -> Objects.equals(ob.id, componentId));
            getHotRodEntity().updated |= removed;
            return removed;
        }

        @Override
        public Optional<MapAuthenticationExecutionEntity> getAuthenticationExecution(String id) {
            Set<HotRodAuthenticationExecutionEntity> set = getHotRodEntity().authenticationExecutions;
            if (set == null || set.isEmpty()) return Optional.empty();

            return set.stream().filter(ob -> Objects.equals(ob.id, id)).findFirst().map(HotRodAuthenticationExecutionEntityDelegate::new);
        }

        @Override
        public Boolean removeAuthenticationExecution(String executionId) {
            Set<HotRodAuthenticationExecutionEntity> set = getHotRodEntity().authenticationExecutions;
            boolean removed = set != null && set.removeIf(ob -> Objects.equals(ob.id, executionId));
            getHotRodEntity().updated |= removed;
            return removed;
        }

        @Override
        public Optional<MapAuthenticationFlowEntity> getAuthenticationFlow(String flowId) {
            Set<HotRodAuthenticationFlowEntity> set = getHotRodEntity().authenticationFlows;
            if (set == null || set.isEmpty()) return Optional.empty();

            return set.stream().filter(ob -> Objects.equals(ob.id, flowId)).findFirst().map(HotRodAuthenticationFlowEntityDelegate::new);
        }

        @Override
        public Boolean removeAuthenticationFlow(String flowId) {
            Set<HotRodAuthenticationFlowEntity> set = getHotRodEntity().authenticationFlows;
            boolean removed = set != null && set.removeIf(ob -> Objects.equals(ob.id, flowId));
            getHotRodEntity().updated |= removed;
            return removed;
        }

        @Override
        public Boolean removeAuthenticatorConfig(String authenticatorConfigId) {
            Set<HotRodAuthenticatorConfigEntity> set = getHotRodEntity().authenticatorConfigs;
            boolean removed = set != null && set.removeIf(ob -> Objects.equals(ob.id, authenticatorConfigId));
            getHotRodEntity().updated |= removed;
            return removed;
        }

        @Override
        public Optional<MapAuthenticatorConfigEntity> getAuthenticatorConfig(String authenticatorConfigId) {
            Set<HotRodAuthenticatorConfigEntity> set = getHotRodEntity().authenticatorConfigs;
            if (set == null || set.isEmpty()) return Optional.empty();

            return set.stream().filter(ob -> Objects.equals(ob.id, authenticatorConfigId)).findFirst().map(HotRodAuthenticatorConfigEntityDelegate::new);
        }

        @Override
        public Boolean removeIdentityProviderMapper(String identityProviderMapperId) {
            Set<HotRodIdentityProviderMapperEntity> set = getHotRodEntity().identityProviderMappers;
            boolean removed = set != null && set.removeIf(ob -> Objects.equals(ob.id, identityProviderMapperId));
            getHotRodEntity().updated |= removed;
            return removed;
        }

        @Override
        public Optional<MapIdentityProviderMapperEntity> getIdentityProviderMapper(String identityProviderMapperId) {
            Set<HotRodIdentityProviderMapperEntity> set = getHotRodEntity().identityProviderMappers;
            if (set == null || set.isEmpty()) return Optional.empty();

            return set.stream().filter(ob -> Objects.equals(ob.id, identityProviderMapperId)).findFirst().map(HotRodIdentityProviderMapperEntityDelegate::new);
        }

        @Override
        public Boolean removeIdentityProvider(String identityProviderId) {
            Set<HotRodIdentityProviderEntity> set = getHotRodEntity().identityProviders;
            boolean removed = set != null && set.removeIf(ob -> Objects.equals(ob.id, identityProviderId));
            getHotRodEntity().updated |= removed;
            return removed;
        }

        @Override
        public Optional<MapClientInitialAccessEntity> getClientInitialAccess(String clientInitialAccessId) {
            Set<HotRodClientInitialAccessEntity> set = getHotRodEntity().clientInitialAccesses;
            if (set == null || set.isEmpty()) return Optional.empty();

            return set.stream().filter(ob -> Objects.equals(ob.id, clientInitialAccessId)).findFirst().map(HotRodClientInitialAccessEntityDelegate::new);
        }

        @Override
        public Boolean removeClientInitialAccess(String clientInitialAccessId) {
            Set<HotRodClientInitialAccessEntity> set = getHotRodEntity().clientInitialAccesses;
            boolean removed = set != null && set.removeIf(ob -> Objects.equals(ob.id, clientInitialAccessId));
            getHotRodEntity().updated |= removed;
            return removed;
        }

        @Override
        public Optional<MapRequiredActionProviderEntity> getRequiredActionProvider(String requiredActionProviderId) {
            Set<HotRodRequiredActionProviderEntity> set = getHotRodEntity().requiredActionProviders;
            if (set == null || set.isEmpty()) return Optional.empty();

            return set.stream().filter(ob -> Objects.equals(ob.id, requiredActionProviderId)).findFirst().map(HotRodRequiredActionProviderEntityDelegate::new);
        }

        @Override
        public Boolean removeRequiredActionProvider(String requiredActionProviderId) {
            Set<HotRodRequiredActionProviderEntity> set = getHotRodEntity().requiredActionProviders;
            boolean removed = set != null && set.removeIf(ob -> Objects.equals(ob.id, requiredActionProviderId));
            getHotRodEntity().updated |= removed;
            return removed;
        }

        @Override
        public boolean hasClientInitialAccess() {
            Set<MapClientInitialAccessEntity> cias = getClientInitialAccesses();
            return cias != null && !cias.isEmpty();
        }

        @Override
        public void removeExpiredClientInitialAccesses() {
            Set<MapClientInitialAccessEntity> cias = getClientInitialAccesses();
            if (cias != null)
                cias.stream()
                        .filter(this::checkIfExpired)
                        .map(MapClientInitialAccessEntity::getId)
                        .collect(Collectors.toSet())
                        .forEach(this::removeClientInitialAccess);
        }

        private boolean checkIfExpired(MapClientInitialAccessEntity cia) {
            return cia.getRemainingCount() < 1 || isExpired(cia, true);
        }
    }
    @Override
    public boolean equals(Object o) {
        return HotRodRealmEntityDelegate.entityEquals(this, o);
    }

    @Override
    public int hashCode() {
        return HotRodRealmEntityDelegate.entityHashCode(this);
    }
}
