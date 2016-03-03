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

package org.keycloak.protocol.oidc;

import org.keycloak.OAuth2Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.oidc.endpoints.TokenEndpoint;
import org.keycloak.protocol.oidc.representations.OIDCConfigurationRepresentation;
import org.keycloak.protocol.oidc.utils.OIDCResponseType;
import org.keycloak.services.clientregistration.ClientRegistrationService;
import org.keycloak.services.clientregistration.oidc.OIDCClientRegistrationProviderFactory;
import org.keycloak.services.resources.RealmsResource;
import org.keycloak.services.Urls;
import org.keycloak.wellknown.WellKnownProvider;

import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.util.LinkedList;
import java.util.List;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class OIDCWellKnownProvider implements WellKnownProvider {

    public static final List<String> DEFAULT_ID_TOKEN_SIGNING_ALG_VALUES_SUPPORTED = list("RS256");

    public static final List<String> DEFAULT_GRANT_TYPES_SUPPORTED = list(OAuth2Constants.AUTHORIZATION_CODE, OAuth2Constants.IMPLICIT, OAuth2Constants.REFRESH_TOKEN, OAuth2Constants.PASSWORD, OAuth2Constants.CLIENT_CREDENTIALS);

    public static final List<String> DEFAULT_RESPONSE_TYPES_SUPPORTED = list(OAuth2Constants.CODE, OIDCResponseType.NONE, OIDCResponseType.ID_TOKEN, OIDCResponseType.TOKEN, "id_token token", "code id_token", "code token", "code id_token token");

    public static final List<String> DEFAULT_SUBJECT_TYPES_SUPPORTED = list("public");

    public static final List<String> DEFAULT_RESPONSE_MODES_SUPPORTED = list("query", "fragment", "form_post");

    private KeycloakSession session;

    public OIDCWellKnownProvider(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public Object getConfig() {
        UriInfo uriInfo = session.getContext().getUri();
        RealmModel realm = session.getContext().getRealm();

        UriBuilder uriBuilder = RealmsResource.protocolUrl(uriInfo);

        OIDCConfigurationRepresentation config = new OIDCConfigurationRepresentation();
        config.setIssuer(Urls.realmIssuer(uriInfo.getBaseUri(), realm.getName()));
        config.setAuthorizationEndpoint(uriBuilder.clone().path(OIDCLoginProtocolService.class, "auth").build(realm.getName(), OIDCLoginProtocol.LOGIN_PROTOCOL).toString());
        config.setTokenEndpoint(uriBuilder.clone().path(OIDCLoginProtocolService.class, "token").build(realm.getName(), OIDCLoginProtocol.LOGIN_PROTOCOL).toString());
        config.setTokenIntrospectionEndpoint(uriBuilder.clone().path(OIDCLoginProtocolService.class, "token").path(TokenEndpoint.class, "introspect").build(realm.getName(), OIDCLoginProtocol.LOGIN_PROTOCOL).toString());
        config.setUserinfoEndpoint(uriBuilder.clone().path(OIDCLoginProtocolService.class, "issueUserInfo").build(realm.getName(), OIDCLoginProtocol.LOGIN_PROTOCOL).toString());
        config.setLogoutEndpoint(uriBuilder.clone().path(OIDCLoginProtocolService.class, "logout").build(realm.getName(), OIDCLoginProtocol.LOGIN_PROTOCOL).toString());
        config.setJwksUri(uriBuilder.clone().path(OIDCLoginProtocolService.class, "certs").build(realm.getName(), OIDCLoginProtocol.LOGIN_PROTOCOL).toString());
        config.setRegistrationEndpoint(RealmsResource.clientRegistrationUrl(uriInfo).path(ClientRegistrationService.class, "provider").build(realm.getName(), OIDCClientRegistrationProviderFactory.ID).toString());

        config.setIdTokenSigningAlgValuesSupported(DEFAULT_ID_TOKEN_SIGNING_ALG_VALUES_SUPPORTED);
        config.setResponseTypesSupported(DEFAULT_RESPONSE_TYPES_SUPPORTED);
        config.setSubjectTypesSupported(DEFAULT_SUBJECT_TYPES_SUPPORTED);
        config.setResponseModesSupported(DEFAULT_RESPONSE_MODES_SUPPORTED);
        config.setGrantTypesSupported(DEFAULT_GRANT_TYPES_SUPPORTED);

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
