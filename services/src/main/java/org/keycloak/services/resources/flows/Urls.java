package org.keycloak.services.resources.flows;

import java.net.URI;

import javax.ws.rs.core.UriBuilder;

import org.keycloak.services.resources.RealmsResource;
import org.keycloak.services.resources.SaasService;
import org.keycloak.services.resources.SocialResource;
import org.keycloak.services.resources.TokenService;

public class Urls {

    private static UriBuilder realmBase(URI baseUri) {
        return UriBuilder.fromUri(baseUri).path(RealmsResource.class);
    }

    private static UriBuilder tokenBase(URI baseUri) {
        return realmBase(baseUri).path(RealmsResource.class, "getTokenService");
    }

    public static URI realmLoginAction(URI baseUri, String realmId) {
        return tokenBase(baseUri).path(TokenService.class, "processLogin").build(realmId);
    }

    public static URI realmLoginPage(URI baseUri, String realmId) {
        return tokenBase(baseUri).path(TokenService.class, "loginPage").build(realmId);
    }

    public static URI realmRegisterAction(URI baseUri, String realmId) {
        return tokenBase(baseUri).path(TokenService.class, "processRegister").build(realmId);
    }

    public static URI realmRegisterPage(URI baseUri, String realmId) {
        return tokenBase(baseUri).path(TokenService.class, "registerPage").build(realmId);
    }

    private static UriBuilder saasBase(URI baseUri) {
        return UriBuilder.fromUri(baseUri).path(SaasService.class);
    }

    public static URI saasLoginAction(URI baseUri) {
        return saasBase(baseUri).path(SaasService.class, "processLogin").build();
    }

    public static URI saasLoginPage(URI baseUri) {
        return saasBase(baseUri).path(SaasService.class, "loginPage").build();
    }

    public static URI saasRegisterAction(URI baseUri) {
        return saasBase(baseUri).path(SaasService.class, "processRegister").build();
    }

    public static URI saasRegisterPage(URI baseUri) {
        return saasBase(baseUri).path(SaasService.class, "registerPage").build();
    }

    private static UriBuilder socialBase(URI baseUri) {
        return UriBuilder.fromUri(baseUri).path(SocialResource.class);
    }

    public static URI socialCallback(URI baseUri) {
        return socialBase(baseUri).path(SocialResource.class, "callback").build();
    }

    public static URI socialRedirectToProviderAuth(URI baseUri, String realmId) {
        return socialBase(baseUri).path(SocialResource.class, "redirectToProviderAuth")
                .build(realmId);
    }

}
