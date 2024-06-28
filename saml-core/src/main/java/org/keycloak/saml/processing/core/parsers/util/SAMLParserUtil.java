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
package org.keycloak.saml.processing.core.parsers.util;
import org.keycloak.dom.saml.v2.assertion.NameIDType;
import org.keycloak.saml.common.PicketLinkLogger;
import org.keycloak.saml.common.PicketLinkLoggerFactory;
import org.keycloak.saml.common.exceptions.ParsingException;
import org.keycloak.saml.common.util.StaxParserUtil;

import org.keycloak.saml.processing.core.parsers.saml.assertion.SAMLAssertionQNames;
import java.util.Objects;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.events.StartElement;

/**
 * Utility methods for SAML Parser
 *
 * @author Anil.Saldhana@redhat.com
 * @since Nov 4, 2010
 */
public class SAMLParserUtil {

    private static final PicketLinkLogger LOGGER = PicketLinkLoggerFactory.getLogger();

    /**
     * Parse a {@code NameIDType}
     *
     * @param xmlEventReader
     *
     * @return
     *
     * @throws ParsingException
     */
    public static NameIDType parseNameIDType(XMLEventReader xmlEventReader) throws ParsingException {
        StartElement nameIDElement = StaxParserUtil.getNextStartElement(xmlEventReader);

        NameIDType nameID = new NameIDType();
        nameID.setFormat(StaxParserUtil.getUriAttributeValue(nameIDElement, SAMLAssertionQNames.ATTR_FORMAT));
        nameID.setNameQualifier(StaxParserUtil.getAttributeValue(nameIDElement, SAMLAssertionQNames.ATTR_NAME_QUALIFIER));
        nameID.setSPProvidedID(StaxParserUtil.getAttributeValue(nameIDElement, SAMLAssertionQNames.ATTR_SP_PROVIDED_ID));
        nameID.setSPNameQualifier(StaxParserUtil.getAttributeValue(nameIDElement, SAMLAssertionQNames.ATTR_SP_NAME_QUALIFIER));

        String nameIDValue = StaxParserUtil.getElementText(xmlEventReader);
        nameID.setValue(nameIDValue);

        return nameID;
    }

    public static void validateAttributeValue(StartElement element, HasQName attributeName, String expectedValue) throws ParsingException {
        String value = StaxParserUtil.getRequiredAttributeValue(element, attributeName);
        if (! Objects.equals(expectedValue, value)) {
            throw LOGGER.parserException(new RuntimeException(
                    String.format("%s %s required to be \"%s\"", element.getName(), attributeName.getQName(), expectedValue)));
        }
    }
}