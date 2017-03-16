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

import org.keycloak.OAuth2Constants;
import org.keycloak.common.Version;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.OIDCLoginProtocolService;
import org.keycloak.services.resources.account.DeprecatedAccountFormService;
import org.keycloak.services.resources.IdentityBrokerService;
import org.keycloak.services.resources.LoginActionsService;
import org.keycloak.services.resources.RealmsResource;
import org.keycloak.services.resources.ThemeResource;

import javax.ws.rs.core.UriBuilder;
import java.net.URI;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class Urls {

    public static URI accountApplicationsPage(URI baseUri, String realmId) {
        return accountBase(baseUri).path(DeprecatedAccountFormService.class, "applicationsPage").build(realmId);
    }

    public static UriBuilder accountBase(URI baseUri) {
        return realmBase(baseUri).path(RealmsResource.class, "getAccountService");
    }

    public static URI accountPage(URI baseUri, String realmId) {
        return accountPageBuilder(baseUri).build(realmId);
    }

    public static UriBuilder accountPageBuilder(URI baseUri) {
        return accountBase(baseUri).path(DeprecatedAccountFormService.class, "accountPage");
    }

    public static URI accountPasswordPage(URI baseUri, String realmId) {
        return accountBase(baseUri).path(DeprecatedAccountFormService.class, "passwordPage").build(realmId);
    }

    public static URI accountFederatedIdentityPage(URI baseUri, String realmId) {
        return accountBase(baseUri).path(DeprecatedAccountFormService.class, "federatedIdentityPage").build(realmId);
    }

    public static URI accountFederatedIdentityUpdate(URI baseUri, String realmName) {
        return accountBase(baseUri).path(DeprecatedAccountFormService.class, "processFederatedIdentityUpdate").build(realmName);
    }

    public static URI identityProviderAuthnResponse(URI baseUri, String providerId, String realmName) {
        return realmBase(baseUri).path(RealmsResource.class, "getBrokerService")
                .path(IdentityBrokerService.class, "getEndpoint")
                .build(realmName, providerId);
    }

    public static URI identityProviderAuthnRequest(URI baseUri, String providerId, String realmName, String accessCode) {
        UriBuilder uriBuilder = realmBase(baseUri).path(RealmsResource.class, "getBrokerService")
                .path(IdentityBrokerService.class, "performLogin");

        if (accessCode != null) {
            uriBuilder.replaceQueryParam(OAuth2Constants.CODE, accessCode);
        }

        return uriBuilder.build(realmName, providerId);
    }

    public static URI identityProviderLinkRequest(URI baseUri, String providerId, String realmName) {
        UriBuilder uriBuilder = realmBase(baseUri).path(RealmsResource.class, "getBrokerService")
                .replaceQuery(null)
                .path(IdentityBrokerService.class, "clientInitiatedAccountLinking");

        return uriBuilder.build(realmName, providerId);
    }

    public static URI identityProviderRetrieveToken(URI baseUri, String providerId, String realmName) {
        return realmBase(baseUri).path(RealmsResource.class, "getBrokerService")
                .path(IdentityBrokerService.class, "retrieveToken")
                .build(realmName, providerId);
    }

    public static URI identityProviderAuthnRequest(URI baseURI, String providerId, String realmName) {
        return identityProviderAuthnRequest(baseURI, providerId, realmName, null);
    }

    public static URI identityProviderAfterFirstBrokerLogin(URI baseUri, String realmName, String accessCode) {
        return realmBase(baseUri).path(RealmsResource.class, "getBrokerService")
                .path(IdentityBrokerService.class, "afterFirstBrokerLogin")
                .replaceQueryParam(OAuth2Constants.CODE, accessCode)
                .build(realmName);
    }

    public static URI identityProviderAfterPostBrokerLogin(URI baseUri, String realmName, String accessCode) {
        return realmBase(baseUri).path(RealmsResource.class, "getBrokerService")
                .path(IdentityBrokerService.class, "afterPostBrokerLoginFlow")
                .replaceQueryParam(OAuth2Constants.CODE, accessCode)
                .build(realmName);
    }

    public static URI accountTotpPage(URI baseUri, String realmId) {
        return accountBase(baseUri).path(DeprecatedAccountFormService.class, "totpPage").build(realmId);
    }

    public static URI accountTotpRemove(URI baseUri, String realmId, String stateChecker) {
        return accountBase(baseUri).path(DeprecatedAccountFormService.class, "processTotpRemove")
                .queryParam("stateChecker", stateChecker)
                .build(realmId);
    }

    public static URI accountLogPage(URI baseUri, String realmId) {
        return accountBase(baseUri).path(DeprecatedAccountFormService.class, "logPage").build(realmId);
    }

    public static URI accountSessionsPage(URI baseUri, String realmId) {
        return accountBase(baseUri).path(DeprecatedAccountFormService.class, "sessionsPage").build(realmId);
    }

    public static URI accountSessionsLogoutPage(URI baseUri, String realmId, String stateChecker) {
        return accountBase(baseUri).path(DeprecatedAccountFormService.class, "processSessionsLogout")
                .queryParam("stateChecker", stateChecker)
                .build(realmId);
    }

    public static URI accountRevokeClientPage(URI baseUri, String realmId) {
        return accountBase(baseUri).path(DeprecatedAccountFormService.class, "processRevokeGrant")
                .build(realmId);
    }

    public static URI accountLogout(URI baseUri, URI redirectUri, String realmId) {
        return realmLogout(baseUri).queryParam("redirect_uri", redirectUri).build(realmId);
    }

    public static URI loginActionUpdatePassword(URI baseUri, String realmId) {
        return loginActionsBase(baseUri).path(LoginActionsService.class, "updatePassword").build(realmId);
    }

    public static URI loginActionUpdateTotp(URI baseUri, String realmId) {
        return loginActionsBase(baseUri).path(LoginActionsService.class, "updateTotp").build(realmId);
    }

    public static UriBuilder requiredActionBase(URI baseUri) {
        return loginActionsBase(baseUri).path(LoginActionsService.class, "requiredAction");
    }


    public static URI loginActionUpdateProfile(URI baseUri, String realmId) {
        return loginActionsBase(baseUri).path(LoginActionsService.class, "updateProfile").build(realmId);
    }

    public static URI loginActionEmailVerification(URI baseUri, String realmId) {
        return loginActionEmailVerificationBuilder(baseUri).build(realmId);
    }

    public static UriBuilder loginActionEmailVerificationBuilder(URI baseUri) {
        return loginActionsBase(baseUri).path(LoginActionsService.class, "emailVerification");
    }

    public static URI loginResetCredentials(URI baseUri, String realmId) {
        return loginResetCredentialsBuilder(baseUri).build(realmId);
    }

    public static UriBuilder executeActionsBuilder(URI baseUri) {
        return loginActionsBase(baseUri).path(LoginActionsService.class, "executeActions");
    }

    public static UriBuilder loginResetCredentialsBuilder(URI baseUri) {
        return loginActionsBase(baseUri).path(LoginActionsService.RESET_CREDENTIALS_PATH);
    }

    public static URI loginUsernameReminder(URI baseUri, String realmId) {
        return loginUsernameReminderBuilder(baseUri).build(realmId);
    }

    public static UriBuilder loginUsernameReminderBuilder(URI baseUri) {
        return loginActionsBase(baseUri).path(LoginActionsService.class, "usernameReminder");
    }

    public static String realmIssuer(URI baseUri, String realmId) {
        return realmBase(baseUri).path("{realm}").build(realmId).toString();
    }

    public static UriBuilder realmBase(URI baseUri) {
        return UriBuilder.fromUri(baseUri).path(RealmsResource.class);
    }

    public static URI realmLoginPage(URI baseUri, String realmId) {
        return loginActionsBase(baseUri).path(LoginActionsService.class, "authenticate").build(realmId);
    }

    private static UriBuilder realmLogout(URI baseUri) {
        return tokenBase(baseUri).path(OIDCLoginProtocolService.class, "logout");
    }

    public static URI realmRegisterAction(URI baseUri, String realmId) {
        return loginActionsBase(baseUri).path(LoginActionsService.class, "processRegister").build(realmId);
    }

    public static URI realmRegisterPage(URI baseUri, String realmId) {
        return loginActionsBase(baseUri).path(LoginActionsService.class, "registerPage").build(realmId);
    }

    public static URI realmInstalledAppUrnCallback(URI baseUri, String realmId) {
        return tokenBase(baseUri).path(OIDCLoginProtocolService.class, "installedAppUrnCallback").build(realmId);
    }

    public static URI realmOauthAction(URI baseUri, String realmId) {
        return loginActionsBase(baseUri).path(LoginActionsService.class, "processConsent").build(realmId);
    }

    public static URI firstBrokerLoginProcessor(URI baseUri, String realmName) {
        return loginActionsBase(baseUri).path(LoginActionsService.class, "firstBrokerLoginGet")
                .build(realmName);
    }

    public static String localeCookiePath(URI baseUri, String realmName){
        return realmBase(baseUri).path(realmName).build().getRawPath();
    }

    public static URI themeRoot(URI baseUri) {
        return themeBase(baseUri).path(Version.RESOURCES_VERSION).build();
    }

    private static UriBuilder loginActionsBase(URI baseUri) {
        return realmBase(baseUri).path(RealmsResource.class, "getLoginActionsService");
    }

    private static UriBuilder tokenBase(URI baseUri) {
        return realmBase(baseUri).path("{realm}/protocol/" + OIDCLoginProtocol.LOGIN_PROTOCOL);
    }

    private static UriBuilder themeBase(URI baseUri) {
        return UriBuilder.fromUri(baseUri).path(ThemeResource.class);
    }
}
