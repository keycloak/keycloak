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

import org.keycloak.adapters.saml.config.IDP;
import org.keycloak.saml.common.exceptions.ParsingException;
import org.keycloak.saml.common.util.StaxParserUtil;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.events.StartElement;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class IdpParser extends AbstractKeycloakSamlAdapterV1Parser<IDP> {

    private static final IdpParser INSTANCE = new IdpParser();

    private IdpParser() {
        super(KeycloakSamlAdapterV1QNames.IDP);
    }

    public static IdpParser getInstance() {
        return INSTANCE;
    }

    @Override
    protected IDP instantiateElement(XMLEventReader xmlEventReader, StartElement element) throws ParsingException {
        final IDP idp = new IDP();

        idp.setEntityID(StaxParserUtil.getRequiredAttributeValueRP(element, KeycloakSamlAdapterV1QNames.ATTR_ENTITY_ID));

        Boolean signaturesRequired = StaxParserUtil.getBooleanAttributeValueRP(element, KeycloakSamlAdapterV1QNames.ATTR_SIGNATURES_REQUIRED);
        idp.setSignaturesRequired(signaturesRequired == null ? false : signaturesRequired);
        idp.setSignatureCanonicalizationMethod(StaxParserUtil.getAttributeValueRP(element, KeycloakSamlAdapterV1QNames.ATTR_SIGNATURE_CANONICALIZATION_METHOD));
        idp.setSignatureAlgorithm(StaxParserUtil.getAttributeValueRP(element, KeycloakSamlAdapterV1QNames.ATTR_SIGNATURE_ALGORITHM));
        idp.setMetadataUrl(StaxParserUtil.getAttributeValueRP(element, KeycloakSamlAdapterV1QNames.ATTR_METADATA_URL));
        return idp;
    }

    @Override
    protected void processSubElement(XMLEventReader xmlEventReader, IDP target, KeycloakSamlAdapterV1QNames element, StartElement elementDetail) throws ParsingException {
        switch (element) {
            case HTTP_CLIENT:
                target.setHttpClientConfig(HttpClientParser.getInstance().parse(xmlEventReader));
                break;

            case KEYS:
                target.setKeys(KeysParser.getInstance().parse(xmlEventReader));
                break;

            case SINGLE_SIGN_ON_SERVICE:
                target.setSingleSignOnService(SingleSignOnServiceParser.getInstance().parse(xmlEventReader));
                break;

            case SINGLE_LOGOUT_SERVICE:
                target.setSingleLogoutService(SingleLogoutServiceParser.getInstance().parse(xmlEventReader));
                break;
        }
    }
}
