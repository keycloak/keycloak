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
import org.keycloak.common.util.StringPropertyReplacer;
import org.keycloak.saml.common.exceptions.ParsingException;
import org.keycloak.saml.common.util.StaxParserUtil;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.events.StartElement;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class KeyParser extends AbstractKeycloakSamlAdapterV1Parser<Key> {

    private static final KeyParser INSTANCE = new KeyParser();

    private KeyParser() {
        super(KeycloakSamlAdapterV1QNames.KEY);
    }

    public static KeyParser getInstance() {
        return INSTANCE;
    }

    @Override
    protected Key instantiateElement(XMLEventReader xmlEventReader, StartElement element) throws ParsingException {
        Key key = new Key();
        key.setSigning(StaxParserUtil.getBooleanAttributeValueRP(element, KeycloakSamlAdapterV1QNames.ATTR_SIGNING));
        key.setEncryption(StaxParserUtil.getBooleanAttributeValueRP(element, KeycloakSamlAdapterV1QNames.ATTR_ENCRYPTION));
        return key;
    }

    @Override
    protected void processSubElement(XMLEventReader xmlEventReader, Key target, KeycloakSamlAdapterV1QNames element, StartElement elementDetail) throws ParsingException {
        String value;
        switch (element) {
            case KEY_STORE:
                target.setKeystore(KeyStoreParser.getInstance().parse(xmlEventReader));
                break;

            case CERTIFICATE_PEM:
                StaxParserUtil.advance(xmlEventReader);
                value = StaxParserUtil.getElementText(xmlEventReader);
                target.setCertificatePem(StringPropertyReplacer.replaceProperties(value));
                break;

            case PUBLIC_KEY_PEM:
                StaxParserUtil.advance(xmlEventReader);
                value = StaxParserUtil.getElementText(xmlEventReader);
                target.setPublicKeyPem(StringPropertyReplacer.replaceProperties(value));
                break;

            case PRIVATE_KEY_PEM:
                StaxParserUtil.advance(xmlEventReader);
                value = StaxParserUtil.getElementText(xmlEventReader);
                target.setPrivateKeyPem(StringPropertyReplacer.replaceProperties(value));
                break;
        }
    }
}
