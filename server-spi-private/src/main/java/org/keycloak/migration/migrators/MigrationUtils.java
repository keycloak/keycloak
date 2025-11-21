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

package org.keycloak.migration.migrators;

import java.util.stream.Collectors;

import org.keycloak.Config;
import org.keycloak.OAuth2Constants;
import org.keycloak.OAuthErrorException;
import org.keycloak.models.AdminRoles;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientScopeModel;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ProtocolMapperContainerModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RequiredActionProviderModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserConsentModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.KeycloakModelUtils;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class MigrationUtils {

    public static void addAdminRole(RealmModel realm, String roleName) {
        ClientModel client = realm.getMasterAdminClient();
        if (client != null && client.getRole(roleName) == null) {
            RoleModel role = client.addRole(roleName);
            role.setDescription("${role_" + roleName + "}");

            client.getRealm().getRole(AdminRoles.ADMIN).addCompositeRole(role);
        }

        if (!realm.getName().equals(Config.getAdminRealm())) {
            client = realm.getClientByClientId(Constants.REALM_MANAGEMENT_CLIENT_ID);
            if (client != null && client.getRole(roleName) == null) {
                RoleModel role = client.addRole(roleName);
                role.setDescription("${role_" + roleName + "}");

                client.getRole(AdminRoles.REALM_ADMIN).addCompositeRole(role);
            }
        }
    }

    public static void updateOTPRequiredAction(RequiredActionProviderModel otpAction) {
        if (otpAction == null) return;
        if (!UserModel.RequiredAction.CONFIGURE_TOTP.name().equals(otpAction.getProviderId())) return;
        if (!"Configure Totp".equals(otpAction.getName())) return;

        otpAction.setName("Configure OTP");
    }
    
    public static void updateProtocolMappers(ProtocolMapperContainerModel client) {
        client.getProtocolMappersStream()
                .filter(mapper -> !mapper.getConfig().containsKey("userinfo.token.claim") && mapper.getConfig().containsKey("id.token.claim"))
                .peek(mapper -> mapper.getConfig().put("userinfo.token.claim", mapper.getConfig().get("id.token.claim")))
                .collect(Collectors.toSet()).stream() // to avoid ConcurrentModificationException
                .forEach(client::updateProtocolMapper);
    }


    // Called when offline token older than 4.0 (Offline token without clientScopeIds) is called
    public static void migrateOldOfflineToken(KeycloakSession session, RealmModel realm, ClientModel client, UserModel user) throws OAuthErrorException {
        ClientScopeModel offlineScope = KeycloakModelUtils.getClientScopeByName(realm, OAuth2Constants.OFFLINE_ACCESS);
        if (offlineScope == null) {
            throw new OAuthErrorException(OAuthErrorException.INVALID_GRANT, "Offline Access scope not found");
        }

        if (client.isConsentRequired()) {
            // Automatically add consents for client and for offline_access. We know that both were defacto approved by user already and offlineSession is still valid
            UserConsentModel consent = session.users().getConsentByClient(realm, user.getId(), client.getId());
            if (consent != null) {
                if (client.isDisplayOnConsentScreen()) {
                    consent.addGrantedClientScope(client);
                }
                if (offlineScope.isDisplayOnConsentScreen()) {
                    consent.addGrantedClientScope(offlineScope);
                }
                session.users().updateConsent(realm, user.getId(), consent);
            }
        }
    }

    public static void setDefaultClientAuthenticatorType(ClientModel s) {
        s.setClientAuthenticatorType(KeycloakModelUtils.getDefaultClientAuthenticatorType());
    }

    public static boolean isOIDCNonBearerOnlyClient(ClientModel c) {
        return (c.getProtocol() == null || "openid-connect".equals(c.getProtocol())) && !c.isBearerOnly();
    }
}
