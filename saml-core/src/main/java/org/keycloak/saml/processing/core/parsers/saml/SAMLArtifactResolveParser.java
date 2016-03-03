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
package org.keycloak.saml.processing.core.parsers.saml;

import org.keycloak.saml.common.ErrorCodes;
import org.keycloak.saml.common.constants.JBossSAMLConstants;
import org.keycloak.saml.common.constants.JBossSAMLURIConstants;
import org.keycloak.saml.common.exceptions.ParsingException;
import org.keycloak.saml.common.parsers.ParserNamespaceSupport;
import org.keycloak.saml.common.util.StaxParserUtil;
import org.keycloak.dom.saml.v2.protocol.ArtifactResolveType;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.events.StartElement;

/**
 * Parse the {@link ArtifactResolveType}
 *
 * @author Anil.Saldhana@redhat.com
 * @since Jul 1, 2011
 */
public class SAMLArtifactResolveParser extends SAMLRequestAbstractParser implements ParserNamespaceSupport {

    public Object parse(XMLEventReader xmlEventReader) throws ParsingException {
        // Get the startelement
        StartElement startElement = StaxParserUtil.getNextStartElement(xmlEventReader);
        StaxParserUtil.validate(startElement, JBossSAMLConstants.ARTIFACT_RESOLVE.get());

        ArtifactResolveType artifactResolve = parseBaseAttributes(startElement);

        while (xmlEventReader.hasNext()) {
            // Let us peek at the next start element
            startElement = StaxParserUtil.peekNextStartElement(xmlEventReader);
            if (startElement == null)
                break;
            super.parseCommonElements(startElement, xmlEventReader, artifactResolve);
            String elementName = StaxParserUtil.getStartElementName(startElement);

            if (JBossSAMLConstants.ARTIFACT.get().equals(elementName)) {
                startElement = StaxParserUtil.getNextStartElement(xmlEventReader);
                artifactResolve.setArtifact(StaxParserUtil.getElementText(xmlEventReader));
            } else if (JBossSAMLConstants.ISSUER.get().equals(elementName)) {
                continue;
            } else if (JBossSAMLConstants.SIGNATURE.get().equals(elementName)) {
                continue;
            } else
                throw new RuntimeException(ErrorCodes.UNKNOWN_START_ELEMENT + elementName + "::location="
                        + startElement.getLocation());
        }
        return artifactResolve;
    }

    public boolean supports(QName qname) {
        return JBossSAMLURIConstants.PROTOCOL_NSURI.get().equals(qname.getNamespaceURI());
    }

    /**
     * Parse the attributes at the authnrequesttype element
     *
     * @param startElement
     *
     * @return
     *
     * @throws ParsingException
     */
    private ArtifactResolveType parseBaseAttributes(StartElement startElement) throws ParsingException {
        super.parseRequiredAttributes(startElement);
        ArtifactResolveType authnRequest = new ArtifactResolveType(id, issueInstant);
        // Let us get the attributes
        super.parseBaseAttributes(startElement, authnRequest);

        return authnRequest;
    }
}