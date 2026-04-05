/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.saml.processing.core.saml.v2.util;

import java.io.StringWriter;
import java.util.Optional;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.keycloak.saml.common.constants.JBossSAMLConstants;
import org.keycloak.saml.common.exceptions.ConfigurationException;
import org.keycloak.saml.common.util.TransformerUtil;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Utility class to manipulate SAML ArtifactResponse and embedded Response.
 * @author Thibault Morin (https://tmorin.github.io)
 */
public final class ArtifactResponseUtil {

    private ArtifactResponseUtil() {
    }

    /**
     * Convert the Document to a string.
     * <p>
     * The Response shall match the namespace "urn:oasis:names:tc:SAML:2.0:protocol" and the element "Response".
     *
     * @param document the Document to convert
     * @return the Document as a string
     */
    public static Optional<String> convertResponseToString(Document document) {
        return extractResponseElement(document).map(ArtifactResponseUtil::nodeToString);
    }

    /**
     * Convert a Node to a string.
     *
     * @param node the Node to convert
     * @return the Node as a string
     */
    static String nodeToString(Node node) {
        try {
            final StringWriter writer = new StringWriter();
            TransformerUtil.getTransformer().transform(new DOMSource(node), new StreamResult(writer));
            return writer.getBuffer().toString();
        } catch (ConfigurationException | TransformerException e) {
            throw new IllegalStateException("Error converting node to string", e);
        }
    }

    /**
     * Extract the Response element from the Document.
     *
     * @param document the Document to extract the Response element from
     * @return the Response element
     */
    static Optional<Element> extractResponseElement(Document document) {
        // extract from the ArtifactResponse the embedded Response
        final NodeList responseNodeList = document.getElementsByTagNameNS(
                JBossSAMLConstants.RESPONSE__PROTOCOL.getNsUri().get(),
                JBossSAMLConstants.RESPONSE__PROTOCOL.get()
        );

        // leave early if there is no embedded Response
        if (responseNodeList.getLength() != 1) {
            return Optional.empty();
        }

        // convert the embedded Response to a string and then to a base64 serialized string
        final Node responseNode = responseNodeList.item(0);

        // leave early if the response node is not an Element
        if (responseNode.getNodeType() != Node.ELEMENT_NODE) {
            return Optional.empty();
        }

        // return the response node as an Element
        return Optional.of((Element) responseNode);
    }

}
