/*
 * Copyright 2019 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.protocol.saml.mappers;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.jboss.logging.Logger;
import org.keycloak.dom.saml.v2.assertion.AudienceRestrictionType;
import org.keycloak.dom.saml.v2.protocol.ResponseType;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientSessionContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.saml.SamlProtocol;
import org.keycloak.provider.ProviderConfigProperty;

/**
 * SAML audience resolve mapper. The mapper adds all client_ids of \"allowed\"
 * clients to the audience conditions in the assertion. Allowed client means
 * any SAML client for which user has at least one client role.
 *
 * @author rmartinc
 */
public class SAMLAudienceResolveProtocolMapper extends AbstractSAMLProtocolMapper implements SAMLLoginResponseMapper {

    protected static final Logger logger = Logger.getLogger(SAMLAudienceResolveProtocolMapper.class);

    public static final String PROVIDER_ID = "saml-audience-resolve-mapper";

    private static final List<ProviderConfigProperty> configProperties = new ArrayList<ProviderConfigProperty>();

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return configProperties;
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String getDisplayType() {
        return "Audience Resolve";
    }

    @Override
    public String getDisplayCategory() {
        return SAMLAudienceProtocolMapper.AUDIENCE_CATEGORY;
    }

    @Override
    public String getHelpText() {
        return "Adds all client_ids of \"allowed\" clients to the audience conditions in the assertion. " +
                "Allowed client means any SAML client for which user has at least one client role";
    }

    @Override
    public ResponseType transformLoginResponse(ResponseType response,
            ProtocolMapperModel mappingModel, KeycloakSession session,
            UserSessionModel userSession, ClientSessionContext clientSessionCtx) {
        // get the audience restriction
        AudienceRestrictionType aud = SAMLAudienceProtocolMapper.locateAudienceRestriction(response);
        if (aud != null) {
            // get all the roles the user has and calculate the clientIds to add
            Set<RoleModel> roles = clientSessionCtx.getRoles();
            Set<String> audiences = new HashSet<>();
            // add as audience any SAML clientId with role included (same as OIDC)
            for (RoleModel role : roles) {
                logger.tracef("Managing role: %s", role.getName());
                if (role.isClientRole()) {
                    ClientModel app = (ClientModel) role.getContainer();
                    // only adding SAML clients that are not this clientId (which is added by default)
                    if (SamlProtocol.LOGIN_PROTOCOL.equals(app.getProtocol()) &&
                            !app.getClientId().equals(clientSessionCtx.getClientSession().getClient().getClientId())) {
                        audiences.add(app.getClientId());
                    }
                }
            }
            logger.debugf("Calculated audiences to add: %s", audiences);
            // add the audiences
            for (String audience : audiences) {
                try {
                    aud.addAudience(URI.create(audience));
                } catch (IllegalArgumentException e) {
                    logger.warnf(e, "Invalid URI syntax for audience: %s", audience);
                }
            }
        }
        return response;
    }
}
