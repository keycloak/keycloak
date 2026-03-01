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

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.events.StartElement;

import org.keycloak.adapters.saml.config.Key;
import org.keycloak.adapters.saml.config.Key.KeyStoreConfig;
import org.keycloak.saml.common.exceptions.ParsingException;
import org.keycloak.saml.common.util.StaxParserUtil;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class KeyStoreParser extends AbstractKeycloakSamlAdapterV1Parser<KeyStoreConfig> {

    private static final KeyStoreParser INSTANCE = new KeyStoreParser();

    private KeyStoreParser() {
        super(KeycloakSamlAdapterV1QNames.KEY_STORE);
    }

    public static KeyStoreParser getInstance() {
        return INSTANCE;
    }

    @Override
    protected KeyStoreConfig instantiateElement(XMLEventReader xmlEventReader, StartElement element) throws ParsingException {
        final KeyStoreConfig keyStore = new Key.KeyStoreConfig();
        keyStore.setType(StaxParserUtil.getAttributeValueRP(element, KeycloakSamlAdapterV1QNames.ATTR_TYPE));
        keyStore.setAlias(StaxParserUtil.getAttributeValueRP(element, KeycloakSamlAdapterV1QNames.ATTR_ALIAS));
        keyStore.setFile(StaxParserUtil.getAttributeValueRP(element, KeycloakSamlAdapterV1QNames.ATTR_FILE));
        keyStore.setResource(StaxParserUtil.getAttributeValueRP(element, KeycloakSamlAdapterV1QNames.ATTR_RESOURCE));
        keyStore.setPassword(StaxParserUtil.getRequiredAttributeValueRP(element, KeycloakSamlAdapterV1QNames.ATTR_PASSWORD));

        if (keyStore.getFile() == null && keyStore.getResource() == null) {
            throw new ParsingException("KeyStore element must have the url or classpath attribute set");
        }

        return keyStore;
    }

    @Override
    protected void processSubElement(XMLEventReader xmlEventReader, KeyStoreConfig target, KeycloakSamlAdapterV1QNames element, StartElement elementDetail) throws ParsingException {
        switch (element) {
            case CERTIFICATE:
                target.setCertificateAlias(StaxParserUtil.getRequiredAttributeValueRP(elementDetail, KeycloakSamlAdapterV1QNames.ATTR_ALIAS));
                break;

            case PRIVATE_KEY:
                target.setPrivateKeyAlias(StaxParserUtil.getRequiredAttributeValueRP(elementDetail, KeycloakSamlAdapterV1QNames.ATTR_ALIAS));
                target.setPrivateKeyPassword(StaxParserUtil.getRequiredAttributeValueRP(elementDetail, KeycloakSamlAdapterV1QNames.ATTR_PASSWORD));
                break;
        }
    }
}
