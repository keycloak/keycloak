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

import org.keycloak.adapters.saml.config.Key;
import org.keycloak.adapters.saml.config.SP;
import org.keycloak.saml.common.exceptions.ParsingException;
import org.keycloak.saml.common.util.StaxParserUtil;

import java.util.LinkedList;
import java.util.List;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.events.StartElement;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class KeysParser extends AbstractKeycloakSamlAdapterV1Parser<List<Key>> {

    private static final KeysParser INSTANCE = new KeysParser();

    private KeysParser() {
        super(KeycloakSamlAdapterV1QNames.KEYS);
    }

    public static KeysParser getInstance() {
        return INSTANCE;
    }

    @Override
    protected List<Key> instantiateElement(XMLEventReader xmlEventReader, StartElement element) throws ParsingException {
        return new LinkedList<>();
    }

    @Override
    protected void processSubElement(XMLEventReader xmlEventReader, List<Key> target, KeycloakSamlAdapterV1QNames element, StartElement elementDetail) throws ParsingException {
        switch (element) {
            case KEY:
                target.add(KeyParser.getInstance().parse(xmlEventReader));
                break;
        }
    }
}
