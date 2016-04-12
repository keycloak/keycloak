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

package org.keycloak.saml.processing.core.parsers.saml.metadata;

import org.keycloak.saml.common.exceptions.ParsingException;
import org.keycloak.saml.common.parsers.AbstractParser;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;

/**
 * <p>Abstract entity descriptor parser, which provides common parser functionality</p>
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public abstract class AbstractDescriptorParser extends AbstractParser {

    protected XMLEventReader filterWhiteSpaceCharacters(XMLEventReader xmlEventReader) throws ParsingException {
        try {
            return filterWhitespaces(xmlEventReader);
        } catch (XMLStreamException e) {
            throw new ParsingException(e);
        }
    }

}
