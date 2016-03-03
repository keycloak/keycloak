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

package org.keycloak.protocol.oidc.mappers;

import org.keycloak.models.ClientSessionModel;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.IDToken;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Maps user group membership
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class GroupMembershipMapper extends AbstractOIDCProtocolMapper implements OIDCAccessTokenMapper, OIDCIDTokenMapper {

    private static final List<ProviderConfigProperty> configProperties = new ArrayList<ProviderConfigProperty>();

    static {
        ProviderConfigProperty property;
        ProviderConfigProperty property1;
        property1 = new ProviderConfigProperty();
        property1.setName(OIDCAttributeMapperHelper.TOKEN_CLAIM_NAME);
        property1.setLabel(OIDCAttributeMapperHelper.TOKEN_CLAIM_NAME_LABEL);
        property1.setType(ProviderConfigProperty.STRING_TYPE);
        property1.setDefaultValue("groups");
        property1.setHelpText(OIDCAttributeMapperHelper.TOKEN_CLAIM_NAME_TOOLTIP);
        configProperties.add(property1);
        property1 = new ProviderConfigProperty();
        property1.setName("full.path");
        property1.setLabel("Full group path");
        property1.setType(ProviderConfigProperty.BOOLEAN_TYPE);
        property1.setDefaultValue("true");
        property1.setHelpText("Include full path to group i.e. /top/level1/level2, false will just specify the group name");
        configProperties.add(property1);

        property1 = new ProviderConfigProperty();
        property1.setName(OIDCAttributeMapperHelper.INCLUDE_IN_ID_TOKEN);
        property1.setLabel(OIDCAttributeMapperHelper.INCLUDE_IN_ID_TOKEN_LABEL);
        property1.setType(ProviderConfigProperty.BOOLEAN_TYPE);
        property1.setDefaultValue("true");
        property1.setHelpText(OIDCAttributeMapperHelper.INCLUDE_IN_ID_TOKEN_HELP_TEXT);
        configProperties.add(property1);
        property1 = new ProviderConfigProperty();
        property1.setName(OIDCAttributeMapperHelper.INCLUDE_IN_ACCESS_TOKEN);
        property1.setLabel(OIDCAttributeMapperHelper.INCLUDE_IN_ACCESS_TOKEN_LABEL);
        property1.setType(ProviderConfigProperty.BOOLEAN_TYPE);
        property1.setDefaultValue("true");
        property1.setHelpText(OIDCAttributeMapperHelper.INCLUDE_IN_ACCESS_TOKEN_HELP_TEXT);
        configProperties.add(property1);



    }

    public static final String PROVIDER_ID = "oidc-group-membership-mapper";


    public List<ProviderConfigProperty> getConfigProperties() {
        return configProperties;
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String getDisplayType() {
        return "Group Membership";
    }

    @Override
    public String getDisplayCategory() {
        return TOKEN_MAPPER_CATEGORY;
    }

    @Override
    public String getHelpText() {
        return "Map user group membership";
    }

    public static boolean useFullPath(ProtocolMapperModel mappingModel) {
        return "true".equals(mappingModel.getConfig().get("full.path"));
    }


    @Override
    public AccessToken transformAccessToken(AccessToken token, ProtocolMapperModel mappingModel, KeycloakSession session,
                                            UserSessionModel userSession, ClientSessionModel clientSession) {
        if (!OIDCAttributeMapperHelper.includeInAccessToken(mappingModel)) return token;
        buildMembership(token, mappingModel, userSession);
        return token;
    }

    public void buildMembership(IDToken token, ProtocolMapperModel mappingModel, UserSessionModel userSession) {
        List<String> membership = new LinkedList<>();
        boolean fullPath = useFullPath(mappingModel);
        for (GroupModel group : userSession.getUser().getGroups()) {
            if (fullPath) {
                membership.add(ModelToRepresentation.buildGroupPath(group));
            } else {
                membership.add(group.getName());
            }
        }
        String protocolClaim = mappingModel.getConfig().get(OIDCAttributeMapperHelper.TOKEN_CLAIM_NAME);

        token.getOtherClaims().put(protocolClaim, membership);
    }

    @Override
    public IDToken transformIDToken(IDToken token, ProtocolMapperModel mappingModel, KeycloakSession session, UserSessionModel userSession, ClientSessionModel clientSession) {
        if (!OIDCAttributeMapperHelper.includeInIDToken(mappingModel)) return token;
        buildMembership(token, mappingModel, userSession);
        return token;
    }

    public static ProtocolMapperModel create(String name,
                                      String tokenClaimName,
                                      boolean consentRequired, String consentText,
                                      boolean accessToken, boolean idToken) {
        ProtocolMapperModel mapper = new ProtocolMapperModel();
        mapper.setName(name);
        mapper.setProtocolMapper(PROVIDER_ID);
        mapper.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        mapper.setConsentRequired(consentRequired);
        mapper.setConsentText(consentText);
        Map<String, String> config = new HashMap<String, String>();
        config.put(OIDCAttributeMapperHelper.TOKEN_CLAIM_NAME, tokenClaimName);
        if (accessToken) config.put(OIDCAttributeMapperHelper.INCLUDE_IN_ACCESS_TOKEN, "true");
        if (idToken) config.put(OIDCAttributeMapperHelper.INCLUDE_IN_ID_TOKEN, "true");
        mapper.setConfig(config);
        
        return mapper;
    }


}
