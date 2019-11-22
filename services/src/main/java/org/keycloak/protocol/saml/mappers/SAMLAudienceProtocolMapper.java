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
import java.util.List;
import org.jboss.logging.Logger;
import org.keycloak.dom.saml.v2.assertion.AudienceRestrictionType;
import org.keycloak.dom.saml.v2.protocol.ResponseType;
import org.keycloak.models.ClientSessionContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.provider.ProviderConfigProperty;

/**
 * SAML mapper to add a audience restriction into the assertion, to another
 * client (clientId) or to a custom URI. Only one URI is added, clientId
 * has preference over the custom value (the class maps OIDC behavior).
 *
 * @author rmartinc
 */
public class SAMLAudienceProtocolMapper extends AbstractSAMLProtocolMapper implements SAMLLoginResponseMapper {

    protected static final Logger logger = Logger.getLogger(SAMLAudienceProtocolMapper.class);

    public static final String PROVIDER_ID = "saml-audience-mapper";

    public static final String AUDIENCE_CATEGORY = "Audience mapper";

    private static final List<ProviderConfigProperty> configProperties = new ArrayList<ProviderConfigProperty>();

    public static final String INCLUDED_CLIENT_AUDIENCE = "included.client.audience";
    private static final String INCLUDED_CLIENT_AUDIENCE_LABEL = "included.client.audience.label";
    private static final String INCLUDED_CLIENT_AUDIENCE_HELP_TEXT = "included.client.audience.tooltip";

    public static final String INCLUDED_CUSTOM_AUDIENCE = "included.custom.audience";
    private static final String INCLUDED_CUSTOM_AUDIENCE_LABEL = "included.custom.audience.label";
    private static final String INCLUDED_CUSTOM_AUDIENCE_HELP_TEXT = "included.custom.audience.tooltip";

    static {
        ProviderConfigProperty property;
        property = new ProviderConfigProperty();
        property.setName(INCLUDED_CLIENT_AUDIENCE);
        property.setLabel(INCLUDED_CLIENT_AUDIENCE_LABEL);
        property.setHelpText(INCLUDED_CLIENT_AUDIENCE_HELP_TEXT);
        property.setType(ProviderConfigProperty.CLIENT_LIST_TYPE);
        configProperties.add(property);

        property = new ProviderConfigProperty();
        property.setName(INCLUDED_CUSTOM_AUDIENCE);
        property.setLabel(INCLUDED_CUSTOM_AUDIENCE_LABEL);
        property.setHelpText(INCLUDED_CUSTOM_AUDIENCE_HELP_TEXT);
        property.setType(ProviderConfigProperty.STRING_TYPE);
        configProperties.add(property);
    }

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
        return "Audience";
    }

    @Override
    public String getDisplayCategory() {
        return AUDIENCE_CATEGORY;
    }

    @Override
    public String getHelpText() {
        return "Add specified audience to the audience conditions in the assertion.";
    }

    protected static AudienceRestrictionType locateAudienceRestriction(ResponseType response) {
        try {
            return response.getAssertions().get(0).getAssertion().getConditions().getConditions()
                    .stream()
                    .filter(AudienceRestrictionType.class::isInstance)
                    .map(AudienceRestrictionType.class::cast)
                    .findFirst().orElse(null);
        } catch (NullPointerException | IndexOutOfBoundsException e) {
            logger.warn("Invalid SAML ResponseType to add the audience restriction", e);
            return null;
        }
    }

    @Override
    public ResponseType transformLoginResponse(ResponseType response,
            ProtocolMapperModel mappingModel, KeycloakSession session,
            UserSessionModel userSession, ClientSessionContext clientSessionCtx) {
        // read configuration as in OIDC (first clientId, then custom)
        String audience = mappingModel.getConfig().get(INCLUDED_CLIENT_AUDIENCE);
        if (audience == null || audience.isEmpty()) {
            audience = mappingModel.getConfig().get(INCLUDED_CUSTOM_AUDIENCE);
        }
        // locate the first condition that has an audience restriction
        if (audience != null && !audience.isEmpty()) {
            AudienceRestrictionType aud = locateAudienceRestriction(response);
            if (aud != null) {
                logger.debugf("adding audience: %s", audience);
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
