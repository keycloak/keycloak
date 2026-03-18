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

package org.keycloak.adapters.saml.config.parsers;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.events.StartElement;

import org.keycloak.adapters.saml.config.SP;
import org.keycloak.saml.common.exceptions.ParsingException;
import org.keycloak.saml.common.util.StaxParserUtil;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class SpParser extends AbstractKeycloakSamlAdapterV1Parser<SP> {

    private static final SpParser INSTANCE = new SpParser();

    private SpParser() {
        super(KeycloakSamlAdapterV1QNames.SP);
    }

    public static SpParser getInstance() {
        return INSTANCE;
    }

    @Override
    protected SP instantiateElement(XMLEventReader xmlEventReader, StartElement element) throws ParsingException {
        final SP sp = new SP();

        sp.setEntityID(StaxParserUtil.getRequiredAttributeValueRP(element, KeycloakSamlAdapterV1QNames.ATTR_ENTITY_ID));

        sp.setSslPolicy(StaxParserUtil.getAttributeValueRP(element, KeycloakSamlAdapterV1QNames.ATTR_SSL_POLICY));
        sp.setLogoutPage(StaxParserUtil.getAttributeValueRP(element, KeycloakSamlAdapterV1QNames.ATTR_LOGOUT_PAGE));
        sp.setNameIDPolicyFormat(StaxParserUtil.getAttributeValueRP(element, KeycloakSamlAdapterV1QNames.ATTR_NAME_ID_POLICY_FORMAT));
        sp.setForceAuthentication(StaxParserUtil.getBooleanAttributeValueRP(element, KeycloakSamlAdapterV1QNames.ATTR_FORCE_AUTHENTICATION));
        sp.setIsPassive(StaxParserUtil.getBooleanAttributeValueRP(element, KeycloakSamlAdapterV1QNames.ATTR_IS_PASSIVE));
        sp.setAutodetectBearerOnly(StaxParserUtil.getBooleanAttributeValueRP(element, KeycloakSamlAdapterV1QNames.ATTR_AUTODETECT_BEARER_ONLY));
        sp.setTurnOffChangeSessionIdOnLogin(StaxParserUtil.getBooleanAttributeValueRP(element, KeycloakSamlAdapterV1QNames.ATTR_TURN_OFF_CHANGE_SESSSION_ID_ON_LOGIN));
        sp.setKeepDOMAssertion(StaxParserUtil.getBooleanAttributeValueRP(element, KeycloakSamlAdapterV1QNames.ATTR_KEEP_DOM_ASSERTION));

        return sp;
    }

    @Override
    protected void processSubElement(XMLEventReader xmlEventReader, SP target, KeycloakSamlAdapterV1QNames element, StartElement elementDetail) throws ParsingException {
        switch (element) {
            case KEYS:
                target.setKeys(KeysParser.getInstance().parse(xmlEventReader));
                break;

            case PRINCIPAL_NAME_MAPPING:
                target.setPrincipalNameMapping(PrincipalNameMappingParser.getInstance().parse(xmlEventReader));
                break;

            case ROLE_IDENTIFIERS:
                target.setRoleAttributes(RoleMappingParser.getInstance().parse(xmlEventReader));
                break;

            case ROLE_MAPPINGS_PROVIDER:
                target.setRoleMappingsProviderConfig(RoleMappingsProviderParser.getInstance().parse(xmlEventReader));
                break;

            case IDP:
                target.setIdp(IdpParser.getInstance().parse(xmlEventReader));
                break;
        }
    }
}
