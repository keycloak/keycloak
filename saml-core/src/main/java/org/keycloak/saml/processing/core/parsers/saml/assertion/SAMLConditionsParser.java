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
package org.keycloak.saml.processing.core.parsers.saml.assertion;

import org.keycloak.dom.saml.v2.assertion.AudienceRestrictionType;
import org.keycloak.dom.saml.v2.assertion.ConditionsType;
import org.keycloak.dom.saml.v2.assertion.OneTimeUseType;
import org.keycloak.dom.saml.v2.assertion.ProxyRestrictionType;
import org.keycloak.saml.common.exceptions.ParsingException;
import org.keycloak.saml.common.util.StaxParserUtil;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.events.StartElement;

/**
 * Parse the <conditions> in the saml assertion
 *
 * @since Oct 14, 2010
 */
public class SAMLConditionsParser extends AbstractStaxSamlAssertionParser<ConditionsType> {

    private static final SAMLConditionsParser INSTANCE = new SAMLConditionsParser();

    private SAMLConditionsParser() {
        super(SAMLAssertionQNames.CONDITIONS);
    }

    public static SAMLConditionsParser getInstance() {
        return INSTANCE;
    }

    @Override
    protected ConditionsType instantiateElement(XMLEventReader xmlEventReader, StartElement element) throws ParsingException {
        final ConditionsType conditions = new ConditionsType();

        conditions.setNotBefore(StaxParserUtil.getXmlTimeAttributeValue(element, SAMLAssertionQNames.ATTR_NOT_BEFORE));
        conditions.setNotOnOrAfter(StaxParserUtil.getXmlTimeAttributeValue(element, SAMLAssertionQNames.ATTR_NOT_ON_OR_AFTER));

        return conditions;
    }

    @Override
    protected void processSubElement(XMLEventReader xmlEventReader, ConditionsType target, SAMLAssertionQNames element, StartElement elementDetail) throws ParsingException {
        switch (element) {
            case AUDIENCE_RESTRICTION:
                AudienceRestrictionType audienceRestriction = SAMLAudienceRestrictionParser.getInstance().parse(xmlEventReader);
                target.addCondition(audienceRestriction);
                break;

            case ONE_TIME_USE:
                OneTimeUseType oneTimeUseCondition = new OneTimeUseType();
                target.addCondition(oneTimeUseCondition);
                break;

            case PROXY_RESTRICTION:
                ProxyRestrictionType proxyRestriction = SAMLProxyRestrictionParser.getInstance().parse(xmlEventReader);
                target.addCondition(proxyRestriction);
                break;

            default:
                throw LOGGER.parserUnknownTag(StaxParserUtil.getElementName(elementDetail), elementDetail.getLocation());
        }
    }
}