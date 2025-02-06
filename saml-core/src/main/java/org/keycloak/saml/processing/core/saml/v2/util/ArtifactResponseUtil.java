package org.keycloak.saml.processing.core.saml.v2.util;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringWriter;
import java.util.Optional;

/**
 * Utility class to manipulate SAML ArtifactResponse and embedded Response.
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
            // Transform stuff are not thread sage and shall be instantiated each time
            final TransformerFactory tf = TransformerFactory.newInstance();
            // Secure processing is enabled to avoid XXE attacks
            tf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            final Transformer transformer = tf.newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            final StringWriter writer = new StringWriter();
            transformer.transform(new DOMSource(node), new StreamResult(writer));
            return writer.getBuffer().toString();
        } catch (TransformerException e) {
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
                "urn:oasis:names:tc:SAML:2.0:protocol",
                "Response"
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
