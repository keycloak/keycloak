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
public class SingleLogoutServiceParser extends AbstractKeycloakSamlAdapterV1Parser<IDP.SingleLogoutService> {

    private static final SingleLogoutServiceParser INSTANCE = new SingleLogoutServiceParser();

    private SingleLogoutServiceParser() {
        super(KeycloakSamlAdapterV1QNames.SINGLE_LOGOUT_SERVICE);
    }

    public static SingleLogoutServiceParser getInstance() {
        return INSTANCE;
    }

    @Override
    protected IDP.SingleLogoutService instantiateElement(XMLEventReader xmlEventReader, StartElement element) throws ParsingException {
        final IDP.SingleLogoutService slo = new IDP.SingleLogoutService();

        slo.setSignRequest(StaxParserUtil.getBooleanAttributeValueRP(element, KeycloakSamlAdapterV1QNames.ATTR_SIGN_REQUEST));
        slo.setValidateResponseSignature(StaxParserUtil.getBooleanAttributeValueRP(element, KeycloakSamlAdapterV1QNames.ATTR_VALIDATE_RESPONSE_SIGNATURE));
        slo.setValidateRequestSignature(StaxParserUtil.getBooleanAttributeValueRP(element, KeycloakSamlAdapterV1QNames.ATTR_VALIDATE_REQUEST_SIGNATURE));
        slo.setRequestBinding(StaxParserUtil.getAttributeValueRP(element, KeycloakSamlAdapterV1QNames.ATTR_REQUEST_BINDING));
        slo.setResponseBinding(StaxParserUtil.getAttributeValueRP(element, KeycloakSamlAdapterV1QNames.ATTR_RESPONSE_BINDING));
        slo.setSignResponse(StaxParserUtil.getBooleanAttributeValueRP(element, KeycloakSamlAdapterV1QNames.ATTR_SIGN_RESPONSE));
        slo.setPostBindingUrl(StaxParserUtil.getAttributeValueRP(element, KeycloakSamlAdapterV1QNames.ATTR_POST_BINDING_URL));
        slo.setRedirectBindingUrl(StaxParserUtil.getAttributeValueRP(element, KeycloakSamlAdapterV1QNames.ATTR_REDIRECT_BINDING_URL));

        return slo;
    }

    @Override
    protected void processSubElement(XMLEventReader xmlEventReader, IDP.SingleLogoutService target, KeycloakSamlAdapterV1QNames element, StartElement elementDetail) throws ParsingException {
    }
}
