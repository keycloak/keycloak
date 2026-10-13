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

package org.keycloak.saml.processing.core.util;

import java.util.Objects;
import javax.xml.stream.XMLStreamWriter;

import org.keycloak.saml.SamlProtocolExtensionsAwareBuilder;
import org.keycloak.saml.common.exceptions.ProcessingException;
import org.keycloak.saml.common.util.StaxUtil;

import org.w3c.dom.Element;

/**
 *
 * @author hmlnarik
 */
public class KeycloakKeySamlExtensionGenerator implements SamlProtocolExtensionsAwareBuilder.NodeGenerator {

    public static final String NS_URI = "urn:keycloak:ext:key:1.0";

    public static final String NS_PREFIX = "kckey";

    public static final String KC_KEY_INFO_ELEMENT_NAME = "KeyInfo";

    public static final String KEY_ID_ATTRIBUTE_NAME = "MessageSigningKeyId";

    private final String keyId;

    public KeycloakKeySamlExtensionGenerator(String keyId) {
        this.keyId = keyId;
    }

    @Override
    public void write(XMLStreamWriter writer) throws ProcessingException {
        StaxUtil.writeStartElement(writer, NS_PREFIX, KC_KEY_INFO_ELEMENT_NAME, NS_URI);
        StaxUtil.writeNameSpace(writer, NS_PREFIX, NS_URI);
        if (this.keyId != null) {
            StaxUtil.writeAttribute(writer, KEY_ID_ATTRIBUTE_NAME, this.keyId);
        }
        StaxUtil.writeEndElement(writer);
        StaxUtil.flush(writer);
    }

    /**
     * Checks that the given element is indeed a Keycloak extension {@code KeyInfo} element and
     * returns a content of {@code MessageSigningKeyId} attribute in the given element.
     * @param element Element to obtain the key info from.
     * @return {@code null} if the element is unknown or there is {@code MessageSigningKeyId} attribute unset,
     *   value of the {@code MessageSigningKeyId} attribute otherwise.
     */
    public static String getMessageSigningKeyIdFromElement(Element element) {
        if (Objects.equals(element.getNamespaceURI(), NS_URI) &&
          Objects.equals(element.getLocalName(), KC_KEY_INFO_ELEMENT_NAME) &&
          element.hasAttribute(KEY_ID_ATTRIBUTE_NAME)) {
            return element.getAttribute(KEY_ID_ATTRIBUTE_NAME);
        }

        return null;
    }

}
