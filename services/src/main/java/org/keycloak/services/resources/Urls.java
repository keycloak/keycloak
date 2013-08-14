package org.keycloak.services.resources;

import java.net.URI;

import javax.ws.rs.core.UriInfo;

public class Urls {

    public static URI realmLoginAction(UriInfo uriInfo, String realmId) {
        return TokenService.processLoginUrl(uriInfo).build(realmId);
    }

    public static URI realmLoginPage(UriInfo uriInfo, String realmId) {
        return uriInfo.getBaseUriBuilder().path(SaasService.class).path(SaasService.class, "processLogin").build();
    }

    public static URI realmRegisterAction(UriInfo uriInfo, String realmId) {
        return URI.create("not-implemented-yet");
    }

    public static URI realmRegisterPage(UriInfo uriInfo, String realmId) {
        return URI.create("not-implemented-yet");
    }

    public static URI saasLoginAction(UriInfo uriInfo) {
        return uriInfo.getBaseUriBuilder().path(SaasService.class).path(SaasService.class, "processLogin").build();
    }

    public static URI saasLoginPage(UriInfo uriInfo) {
        return uriInfo.getBaseUriBuilder().path(SaasService.class).path(SaasService.class, "loginPage").build();
    }

    public static URI saasRegisterAction(UriInfo uriInfo) {
        return uriInfo.getBaseUriBuilder().path(SaasService.class).path(SaasService.class, "processRegister").build();
    }

    public static URI saasRegisterPage(UriInfo uriInfo) {
        return uriInfo.getBaseUriBuilder().path(SaasService.class).path(SaasService.class, "registerPage").build();
    }

    public static URI socialRedirectToProviderAuth(UriInfo uriInfo, String realmId) {
        return SocialResource.redirectToProviderAuthUrl(uriInfo).build(realmId);
    }

}
