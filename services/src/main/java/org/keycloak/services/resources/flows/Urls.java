/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.keycloak.services.resources.flows;

import org.keycloak.OAuth2Constants;
import org.keycloak.Version;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.OIDCLoginProtocolService;
import org.keycloak.services.resources.AccountService;
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

    public static URI accountAccessPage(URI baseUri, String realmId) {
        return accountBase(baseUri).path(AccountService.class, "accessPage").build(realmId);
    }

    public static UriBuilder accountBase(URI baseUri) {
        return realmBase(baseUri).path(RealmsResource.class, "getAccountService");
    }

    public static URI accountPage(URI baseUri, String realmId) {
        return accountPageBuilder(baseUri).build(realmId);
    }

    public static UriBuilder accountPageBuilder(URI baseUri) {
        return accountBase(baseUri).path(AccountService.class, "accountPage");
    }

    public static URI accountPasswordPage(URI baseUri, String realmId) {
        return accountBase(baseUri).path(AccountService.class, "passwordPage").build(realmId);
    }

    public static URI accountFederatedIdentityPage(URI baseUri, String realmId) {
        return accountBase(baseUri).path(AccountService.class, "federatedIdentityPage").build(realmId);
    }

    public static URI accountFederatedIdentityUpdate(URI baseUri, String realmName) {
        return accountBase(baseUri).path(AccountService.class, "processFederatedIdentityUpdate").build(realmName);
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

    public static URI identityProviderRetrieveToken(URI baseUri, String providerId, String realmName) {
        return realmBase(baseUri).path(RealmsResource.class, "getBrokerService")
                .path(IdentityBrokerService.class, "retrieveToken")
                .build(realmName, providerId);
    }

    public static URI identityProviderAuthnRequest(URI baseURI, String providerId, String realmName) {
        return identityProviderAuthnRequest(baseURI, providerId, realmName, null);
    }

    public static URI accountTotpPage(URI baseUri, String realmId) {
        return accountBase(baseUri).path(AccountService.class, "totpPage").build(realmId);
    }

    public static URI accountTotpRemove(URI baseUri, String realmId, String stateChecker) {
        return accountBase(baseUri).path(AccountService.class, "processTotpRemove")
                .queryParam("stateChecker", stateChecker)
                .build(realmId);
    }

    public static URI accountLogPage(URI baseUri, String realmId) {
        return accountBase(baseUri).path(AccountService.class, "logPage").build(realmId);
    }

    public static URI accountSessionsPage(URI baseUri, String realmId) {
        return accountBase(baseUri).path(AccountService.class, "sessionsPage").build(realmId);
    }

    public static URI accountSessionsLogoutPage(URI baseUri, String realmId, String stateChecker) {
        return accountBase(baseUri).path(AccountService.class, "processSessionsLogout")
                .queryParam("stateChecker", stateChecker)
                .build(realmId);
    }

    public static URI accountLogout(URI baseUri, URI redirectUri, String realmId) {
        return realmLogout(baseUri).queryParam("redirect_uri", redirectUri).build(realmId);
    }

    public static URI loginActionUpdatePassword(URI baseUri, String realmId) {
        return requiredActionsBase(baseUri).path(LoginActionsService.class, "updatePassword").build(realmId);
    }

    public static URI loginActionUpdateTotp(URI baseUri, String realmId) {
        return requiredActionsBase(baseUri).path(LoginActionsService.class, "updateTotp").build(realmId);
    }

    public static URI loginActionUpdateProfile(URI baseUri, String realmId) {
        return requiredActionsBase(baseUri).path(LoginActionsService.class, "updateProfile").build(realmId);
    }

    public static URI loginActionEmailVerification(URI baseUri, String realmId) {
        return loginActionEmailVerificationBuilder(baseUri).build(realmId);
    }

    public static UriBuilder loginActionEmailVerificationBuilder(URI baseUri) {
        return requiredActionsBase(baseUri).path(LoginActionsService.class, "emailVerification");
    }

    public static URI loginPasswordReset(URI baseUri, String realmId) {
        return loginPasswordResetBuilder(baseUri).build(realmId);
    }

    public static UriBuilder loginPasswordResetBuilder(URI baseUri) {
        return requiredActionsBase(baseUri).path(LoginActionsService.class, "passwordReset");
    }

    public static URI loginUsernameReminder(URI baseUri, String realmId) {
        return loginUsernameReminderBuilder(baseUri).build(realmId);
    }

    public static UriBuilder loginUsernameReminderBuilder(URI baseUri) {
        return requiredActionsBase(baseUri).path(LoginActionsService.class, "usernameReminder");
    }

    private static UriBuilder realmBase(URI baseUri) {
        return UriBuilder.fromUri(baseUri).path(RealmsResource.class);
    }

    public static URI realmLoginAction(URI baseUri, String realmId) {
        return requiredActionsBase(baseUri).path(LoginActionsService.class, "processLogin").build(realmId);
    }

    public static URI realmLoginPage(URI baseUri, String realmId) {
        return requiredActionsBase(baseUri).path(LoginActionsService.class, "loginPage").build(realmId);
    }

    private static UriBuilder realmLogout(URI baseUri) {
        return tokenBase(baseUri).path(OIDCLoginProtocolService.class, "logout");
    }

    public static URI realmRegisterAction(URI baseUri, String realmId) {
        return requiredActionsBase(baseUri).path(LoginActionsService.class, "processRegister").build(realmId);
    }

    public static URI realmRegisterPage(URI baseUri, String realmId) {
        return requiredActionsBase(baseUri).path(LoginActionsService.class, "registerPage").build(realmId);
    }

    public static URI realmInstalledAppUrnCallback(URI baseUri, String realmId) {
        return tokenBase(baseUri).path(OIDCLoginProtocolService.class, "installedAppUrnCallback").build(realmId);
    }

    public static URI realmOauthAction(URI baseUri, String realmId) {
        return requiredActionsBase(baseUri).path(LoginActionsService.class, "processConsent").build(realmId);
    }

    public static String localeCookiePath(URI baseUri, String realmName){
        return realmBase(baseUri).path(realmName).build().getRawPath();
    }

    public static URI themeRoot(URI baseUri) {
        return themeBase(baseUri).path(Version.RESOURCES_VERSION).build();
    }

    private static UriBuilder requiredActionsBase(URI baseUri) {
        return realmBase(baseUri).path(RealmsResource.class, "getLoginActionsService");
    }

    private static UriBuilder tokenBase(URI baseUri) {
        return realmBase(baseUri).path("{realm}/protocol/" + OIDCLoginProtocol.LOGIN_PROTOCOL);
    }

    private static UriBuilder themeBase(URI baseUri) {
        return UriBuilder.fromUri(baseUri).path(ThemeResource.class);
    }
}
