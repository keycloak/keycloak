package org.keycloak.testsuite.util.oauth;

import jakarta.ws.rs.core.UriBuilder;
import org.keycloak.protocol.oidc.OIDCLoginProtocolService;
import org.keycloak.protocol.oidc.grants.ciba.CibaGrantType;
import org.keycloak.protocol.oidc.grants.device.DeviceGrantType;
import org.keycloak.protocol.oidc.par.endpoints.ParEndpoint;

public class Endpoints {

    private final String baseUrl;
    private final String realm;

    public Endpoints(String baseUrl, String realm) {
        this.baseUrl = baseUrl;
        this.realm = realm;
    }

    public String getToken() {
        return asString(OIDCLoginProtocolService.tokenUrl(getBase()));
    }

    public String getIntrospection() {
        return asString(OIDCLoginProtocolService.tokenIntrospectionUrl(getBase()));
    }

    public String getRevocation() {
        return asString(OIDCLoginProtocolService.tokenRevocationUrl(getBase()));
    }

    public String getUserInfo() {
        return asString(OIDCLoginProtocolService.userInfoUrl(getBase()));
    }

    public String getJwks() {
        return asString(OIDCLoginProtocolService.certsUrl(getBase()));
    }

    public String getDeviceAuthorization() {
        return asString(DeviceGrantType.oauth2DeviceAuthUrl(getBase()));
    }

    public String getPushedAuthorizationRequest() {
        return asString(ParEndpoint.parUrl(getBase()));
    }

    public String getLogout() {
        return getLogoutBuilder().build();
    }

    public LogoutUrlBuilder getLogoutBuilder() {
        return new LogoutUrlBuilder(this);
    }

    public String getBackChannelLogout() {
        return asString(OIDCLoginProtocolService.logoutUrl(getBase()).path("/backchannel-logout"));
    }

    public String getBackchannelAuthentication() {
        return asString(CibaGrantType.authorizationUrl(getBase()));
    }

    public String getBackchannelAuthenticationCallback() {
        return asString(CibaGrantType.authenticationUrl(getBase()));
    }

    UriBuilder getBase() {
        return UriBuilder.fromUri(baseUrl);
    }

    String asString(UriBuilder b) {
        return b.build(realm).toString();
    }

}
