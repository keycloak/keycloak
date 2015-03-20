package org.keycloak.protocol.oidc;

import org.keycloak.OAuth2Constants;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.oidc.representations.OIDCConfigurationRepresentation;
import org.keycloak.services.resources.RealmsResource;
import org.keycloak.wellknown.WellKnownProvider;

import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class OIDCWellKnownProvider implements WellKnownProvider {

    public static final List<String> DEFAULT_ID_TOKEN_SIGNING_ALG_VALUES_SUPPORTED = list("RS256");

    public static final List<String> DEFAULT_GRANT_TYPES_SUPPORTED = list(OAuth2Constants.AUTHORIZATION_CODE, OAuth2Constants.REFRESH_TOKEN);

    public static final List<String> DEFAULT_RESPONSE_TYPES_SUPPORTED = list(OAuth2Constants.CODE);

    public static final List<String> DEFAULT_SUBJECT_TYPES_SUPPORTED = list("public");

    public static final List<String> DEFAULT_RESPONSE_MODES_SUPPORTED = list("query");

    @Override
    public Object getConfig(RealmModel realm, UriInfo uriInfo) {
        UriBuilder uriBuilder = RealmsResource.protocolUrl(uriInfo);

        OIDCConfigurationRepresentation config = new OIDCConfigurationRepresentation();
        config.setIssuer(realm.getName());
        config.setAuthorizationEndpoint(uriBuilder.clone().path(OIDCLoginProtocolService.class, "auth").build(realm.getName(), OIDCLoginProtocol.LOGIN_PROTOCOL).toString());
        config.setTokenEndpoint(uriBuilder.clone().path(OIDCLoginProtocolService.class, "token").build(realm.getName(), OIDCLoginProtocol.LOGIN_PROTOCOL).toString());
        config.setUserinfoEndpoint(uriBuilder.clone().path(OIDCLoginProtocolService.class, "issueUserInfo").build(realm.getName(), OIDCLoginProtocol.LOGIN_PROTOCOL).toString());
        config.setLogoutEndpoint(uriBuilder.clone().path(OIDCLoginProtocolService.class, "logout").build(realm.getName(), OIDCLoginProtocol.LOGIN_PROTOCOL).toString());
        config.setJwksUri(uriBuilder.clone().path(OIDCLoginProtocolService.class, "certs").build(realm.getName(), OIDCLoginProtocol.LOGIN_PROTOCOL).toString());

        config.setIdTokenSigningAlgValuesSupported(DEFAULT_ID_TOKEN_SIGNING_ALG_VALUES_SUPPORTED);
        config.setResponseTypesSupported(DEFAULT_RESPONSE_TYPES_SUPPORTED);
        config.setSubjectTypesSupported(DEFAULT_SUBJECT_TYPES_SUPPORTED);
        config.setResponseModesSupported(DEFAULT_RESPONSE_MODES_SUPPORTED);

        if (!realm.isPasswordCredentialGrantAllowed()) {
            config.setGrantTypesSupported(DEFAULT_GRANT_TYPES_SUPPORTED);
        } else {
            List<String> grantTypes = new LinkedList<>(DEFAULT_GRANT_TYPES_SUPPORTED);
            grantTypes.add(OAuth2Constants.PASSWORD);
            config.setGrantTypesSupported(grantTypes);
        }

        return config;
    }

    @Override
    public void close() {
    }

    private static List<String> list(String... values) {
        List<String> s = new LinkedList<>();
        for (String v : values) {
            s.add(v);
        }
        return s;
    }

}
