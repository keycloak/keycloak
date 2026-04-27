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
package org.keycloak.saml.common.parsers;

import java.util.LinkedList;
import java.util.List;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.events.StartElement;

import org.keycloak.saml.common.exceptions.ParsingException;
import org.keycloak.saml.common.util.StaxParserUtil;

import org.w3c.dom.Element;

/**
 * Parses any DOM tree to a list of DOM representations.
 */
public class AnyDomParser extends AbstractStaxParser<List<Element>, AnyDomParser.Dom> {

    public static enum Dom { ANY_DOM };

    public AnyDomParser(QName name) {
        super(name, Dom.ANY_DOM);
    }

    public static AnyDomParser getInstance(QName name) {
        return new AnyDomParser(name);
    }

    @Override
    protected List<Element> instantiateElement(XMLEventReader xmlEventReader, StartElement element) throws ParsingException {
        return new LinkedList<>();
    }

    @Override
    protected void processSubElement(XMLEventReader xmlEventReader, List<Element> target, Dom element, StartElement elementDetail) throws ParsingException {
        target.add(StaxParserUtil.getDOMElement(xmlEventReader));
    }

    @Override
    protected boolean isUnknownElement(Dom token) {
        return true;
    }

    @Override
    protected Dom getElementFromName(QName name) {
        return Dom.ANY_DOM;
    }

}