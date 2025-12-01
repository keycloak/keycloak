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
package org.keycloak.services;

import java.net.URI;

import jakarta.ws.rs.core.UriBuilder;

import org.keycloak.broker.saml.SAMLEndpoint;
import org.keycloak.common.Version;
import org.keycloak.models.Constants;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.OIDCLoginProtocolService;
import org.keycloak.protocol.oidc.endpoints.LogoutEndpoint;
import org.keycloak.protocol.saml.SamlProtocol;
import org.keycloak.services.clientregistration.ClientRegistrationService;
import org.keycloak.services.clientregistration.oidc.OIDCClientRegistrationProvider;
import org.keycloak.services.resources.IdentityBrokerService;
import org.keycloak.services.resources.LoginActionsService;
import org.keycloak.services.resources.RealmsResource;
import org.keycloak.services.resources.ThemeResource;
import org.keycloak.services.resources.admin.AdminRoot;
import org.keycloak.utils.StringUtil;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class Urls {

    public static URI adminConsoleRoot(URI baseUri, String realmName) {
        return UriBuilder.fromUri(baseUri).path(AdminRoot.class).path("{realm}/console/").build(realmName);
    }

    public static UriBuilder accountBase(URI baseUri) {
        return realmBase(baseUri).path(RealmsResource.class, "getAccountService");
    }

    public static URI identityProviderAuthnResponse(URI baseUri, String providerAlias, String realmName) {
        return realmBase(baseUri).path(RealmsResource.class, "getBrokerService")
                .path(IdentityBrokerService.class, "getEndpoint")
                .build(realmName, providerAlias);
    }

    public static URI identityProviderAuthnResponse(URI baseUri, String providerAlias, String realmName, String client_id) {
        return realmBase(baseUri).path(RealmsResource.class, "getBrokerService")
                .path(IdentityBrokerService.class, "getEndpoint")
                .path(SAMLEndpoint.class, "redirectBindingIdpInitiated")
                .build(realmName, providerAlias, client_id);
    }

    public static URI identityProviderAuthnRequest(URI baseUri, String providerAlias, String realmName, String accessCode, String clientId, String tabId, String clientData, String loginHint) {
        UriBuilder uriBuilder = realmBase(baseUri).path(RealmsResource.class, "getBrokerService")
                .path(IdentityBrokerService.class, "performLogin");

        if (accessCode != null) {
            uriBuilder.replaceQueryParam(LoginActionsService.SESSION_CODE, accessCode);
        }
        if (clientId != null) {
            uriBuilder.replaceQueryParam(Constants.CLIENT_ID, clientId);
        }
        if (tabId != null) {
            uriBuilder.replaceQueryParam(Constants.TAB_ID, tabId);
        }
        if (clientData != null) {
            uriBuilder.replaceQueryParam(Constants.CLIENT_DATA, clientData);
        }
        if (loginHint != null) {
            uriBuilder.replaceQueryParam(OIDCLoginProtocol.LOGIN_HINT_PARAM, loginHint);
        }

        return uriBuilder.build(realmName, providerAlias);
    }

    public static URI identityProviderLinkRequest(URI baseUri, String providerAlias, String realmName) {
        UriBuilder uriBuilder = realmBase(baseUri).path(RealmsResource.class, "getBrokerService")
                .replaceQuery(null)
                .path(IdentityBrokerService.class, "clientInitiatedAccountLinking");

        return uriBuilder.build(realmName, providerAlias);
    }

    public static URI identityProviderRetrieveToken(URI baseUri, String providerAlias, String realmName) {
        return realmBase(baseUri).path(RealmsResource.class, "getBrokerService")
                .path(IdentityBrokerService.class, "retrieveToken")
                .build(realmName, providerAlias);
    }

    public static URI identityProviderAuthnRequest(URI baseURI, String providerAlias, String realmName) {
        return identityProviderAuthnRequest(baseURI, providerAlias, realmName, null, null, null, null, null);
    }

    public static URI identityProviderAfterFirstBrokerLogin(URI baseUri, String realmName, String accessCode, String clientId, String tabId, String clientData) {
        return realmBase(baseUri).path(RealmsResource.class, "getBrokerService")
                .path(IdentityBrokerService.class, "afterFirstBrokerLogin")
                .replaceQueryParam(LoginActionsService.SESSION_CODE, accessCode)
                .replaceQueryParam(Constants.CLIENT_ID, clientId)
                .replaceQueryParam(Constants.TAB_ID, tabId)
                .replaceQueryParam(Constants.CLIENT_DATA, clientData)
                .build(realmName);
    }

    public static URI identityProviderAfterPostBrokerLogin(URI baseUri, String realmName, String accessCode, String clientId, String tabId, String clientData) {
        return realmBase(baseUri).path(RealmsResource.class, "getBrokerService")
                .path(IdentityBrokerService.class, "afterPostBrokerLoginFlow")
                .replaceQueryParam(LoginActionsService.SESSION_CODE, accessCode)
                .replaceQueryParam(Constants.CLIENT_ID, clientId)
                .replaceQueryParam(Constants.CLIENT_DATA, clientData)
                .replaceQueryParam(Constants.TAB_ID, tabId)
                .build(realmName);
    }

    public static URI logoutConfirm(URI baseUri, String realmName) {
        return realmLogout(baseUri).path(LogoutEndpoint.class, "logoutConfirmAction").build(realmName);
    }

    public static URI loginActionsDetachedInfo(URI baseUri, String realmName) {
        return loginActionsBase(baseUri).path(LoginActionsService.class, "detachedInfo").build(realmName);
    }

    public static UriBuilder requiredActionBase(URI baseUri) {
        return loginActionsBase(baseUri).path(LoginActionsService.class, "requiredAction");
    }

    public static URI loginResetCredentials(URI baseUri, String realmName) {
        return loginResetCredentialsBuilder(baseUri).build(realmName);
    }

    public static UriBuilder actionTokenBuilder(URI baseUri, String tokenString, String clientId, String tabId, String clientData) {
        UriBuilder res = loginActionsBase(baseUri).path(LoginActionsService.class, "executeActionToken")
          .queryParam(Constants.KEY, tokenString)
          .queryParam(Constants.CLIENT_ID, clientId)
          .queryParam(Constants.TAB_ID, tabId);
        if (StringUtil.isNotBlank(clientData)) {
            res = res.queryParam(Constants.CLIENT_DATA, clientData);
        }
        return res;

    }

    public static UriBuilder loginResetCredentialsBuilder(URI baseUri) {
        return loginActionsBase(baseUri).path(LoginActionsService.RESET_CREDENTIALS_PATH);
    }

    public static URI loginUsernameReminder(URI baseUri, String realmName) {
        return loginUsernameReminderBuilder(baseUri).build(realmName);
    }

    public static UriBuilder loginUsernameReminderBuilder(URI baseUri) {
        return loginActionsBase(baseUri).path(LoginActionsService.class, "usernameReminder");
    }

    public static String realmIssuer(URI baseUri, String realmName) {
        return realmBase(baseUri).path("{realm}").build(realmName).toString();
    }

    public static UriBuilder realmBase(URI baseUri) {
        return UriBuilder.fromUri(baseUri).path(RealmsResource.class);
    }

    public static URI realmLoginPage(URI baseUri, String realmName) {
        return loginActionsBase(baseUri).path(LoginActionsService.class, "authenticate").build(realmName);
    }

    public static URI realmLoginRestartPage(URI baseUri, String realmId, boolean skipLogout) {
        return loginActionsBase(baseUri).path(LoginActionsService.class, "restartSession")
                .queryParam(Constants.SKIP_LOGOUT, String.valueOf(skipLogout))
                .build(realmId);
    }

    private static UriBuilder realmLogout(URI baseUri) {
        return tokenBase(baseUri).path(OIDCLoginProtocolService.class, "logout");
    }

    public static URI tokenEndpoint(URI baseUri, String realmName) {
        return tokenBase(baseUri).path(OIDCLoginProtocolService.class, "token").build(realmName);
    }

    public static URI realmRegisterAction(URI baseUri, String realmName) {
        return loginActionsBase(baseUri).path(LoginActionsService.class, "processRegister").build(realmName);
    }

    public static URI realmRegisterPage(URI baseUri, String realmName) {
        return loginActionsBase(baseUri).path(LoginActionsService.class, "registerPage").build(realmName);
    }

    public static URI realmInstalledAppUrnCallback(URI baseUri, String realmName) {
        return tokenBase(baseUri).path(OIDCLoginProtocolService.class, "installedAppUrnCallback").build(realmName);
    }

    public static URI realmOauthAction(URI baseUri, String realmName) {
        return loginActionsBase(baseUri).path(LoginActionsService.class, "processConsent").build(realmName);
    }

    public static URI firstBrokerLoginProcessor(URI baseUri, String realmName) {
        return loginActionsBase(baseUri).path(LoginActionsService.class, "firstBrokerLoginGet")
                .build(realmName);
    }

    public static URI themeRoot(URI baseUri) {
        return themeBase(baseUri).path(Version.RESOURCES_VERSION).build();
    }

    public static UriBuilder loginActionsBase(URI baseUri) {
        return realmBase(baseUri).path(RealmsResource.class, "getLoginActionsService");
    }

    private static UriBuilder tokenBase(URI baseUri) {
        return realmBase(baseUri).path("{realm}/protocol/" + OIDCLoginProtocol.LOGIN_PROTOCOL);
    }

    private static UriBuilder themeBase(URI baseUri) {
        return UriBuilder.fromUri(baseUri).path(ThemeResource.class);
    }

    public static URI samlRequestEndpoint(final URI baseUri, final String realmName) {
        return realmBase(baseUri).path(RealmsResource.class, "getProtocol").build(realmName, SamlProtocol.LOGIN_PROTOCOL);
    }

    public static URI clientRegistration(URI baseUri, String realm, String protocol, String clientId) {
        return realmBase(baseUri).path(RealmsResource.class, "getClientsService").path(ClientRegistrationService.class, "provider").path(OIDCClientRegistrationProvider.class, "getOIDC").build(realm, protocol, clientId);
    }
}
