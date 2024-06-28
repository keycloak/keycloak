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

import org.keycloak.saml.common.parsers.AbstractStaxParser;

import org.keycloak.saml.common.util.StaxParserUtil;
import org.keycloak.saml.processing.core.parsers.util.QNameEnumLookup;
import java.util.Collections;
import java.util.Set;
import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import javax.xml.stream.events.StartElement;

/**
 * 
 */
public abstract class AbstractKeycloakSamlAdapterV1Parser<T> extends AbstractStaxParser<T, KeycloakSamlAdapterV1QNames> {

    protected static final QNameEnumLookup<KeycloakSamlAdapterV1QNames> LOOKUP = new QNameEnumLookup(KeycloakSamlAdapterV1QNames.values());

    private static final Set<String> ALTERNATE_NAMESPACES = Collections.singleton(XMLConstants.NULL_NS_URI);

    public AbstractKeycloakSamlAdapterV1Parser(KeycloakSamlAdapterV1QNames expectedStartElement) {
        super(expectedStartElement.getQName(), KeycloakSamlAdapterV1QNames.UNKNOWN_ELEMENT);
    }

    @Override
    protected KeycloakSamlAdapterV1QNames getElementFromName(QName name) {
        return (ALTERNATE_NAMESPACES.contains(name.getNamespaceURI()))
          ? LOOKUP.from(new QName(KeycloakSamlAdapterV1QNames.NS_URI, name.getLocalPart()))
          : LOOKUP.from(name);
    }

    @Override
    protected void validateStartElement(StartElement startElement) {
        QName name = startElement.getName();
        QName validatedQName = ALTERNATE_NAMESPACES.contains(name.getNamespaceURI())
          ? new QName(name.getNamespaceURI(), expectedStartElement.getLocalPart())
          : expectedStartElement;
        StaxParserUtil.validate(startElement, validatedQName);
    }

}
