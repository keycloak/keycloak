package org.freedesktop.dbus.utils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.freedesktop.dbus.utils.XmlErrorHandlers.XmlErrorHandlerQuiet;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.ErrorHandler;

/**
 * Assorted static XML utility methods.
 *
 * @author hypfvieh
 */
public final class XmlUtil {

    private XmlUtil() {
    }

    /**
     * Shortcut for checking if given node is of type {@link Element}.
     *
     * @param _node node
     * @return true if {@link Element}, false otherwise
     */
    public static boolean isElementType(Node _node) {
        return _node instanceof Element;
    }

    /**
     * Checks and converts given {@link Node} to {@link Element} if possible.
     * @param _node node
     * @return {@link Element} or null if given {@link Node} is not {@link Element} subtype
     */
    public static Element toElement(Node _node) {
        if (isElementType(_node)) {
            return (Element) _node;
        }
        return null;
    }

    /**
     * Applies a xpathExpression to a xml-Document and return a {@link NodeList} with the results.
     *
     * @param _xpathExpression xpath expression
     * @param _xmlDocumentOrNode document or node
     * @return {@link NodeList}
     * @throws IOException on error
     */
    public static NodeList applyXpathExpressionToDocument(String _xpathExpression, Node _xmlDocumentOrNode)
            throws IOException {

        XPathFactory xfactory = XPathFactory.newInstance();
        XPath xpath = xfactory.newXPath();
        XPathExpression expr = null;
        try {
            expr = xpath.compile(_xpathExpression);
        } catch (XPathExpressionException _ex) {
            throw new IOException(_ex);
        }

        Object result = null;
        try {
            result = expr.evaluate(_xmlDocumentOrNode, XPathConstants.NODESET);
        } catch (Exception _ex) {
            throw new IOException(_ex);
        }

        return (NodeList) result;
    }

    /**
     * Read the given string as XML document.
     *
     * @param _xmlStr xml string
     * @param _validating boolean
     * @param _namespaceAware boolean
     * @return {@link org.w3c.dom.Document}
     * @throws IOException on error
     */
    public static Document parseXmlString(String _xmlStr, boolean _validating, boolean _namespaceAware) throws IOException {

        DocumentBuilderFactory dbFac = DocumentBuilderFactory.newInstance();
        dbFac.setNamespaceAware(_namespaceAware);
        dbFac.setValidating(_validating);

        try {
            return dbFac.newDocumentBuilder().parse(new ByteArrayInputStream(_xmlStr.getBytes(StandardCharsets.UTF_8)));

        } catch (IOException _ex) {
            throw _ex;
        } catch (Exception _ex) {
            throw new IOException("Failed to parse " + Util.abbreviate(_xmlStr, 500), _ex);
        }

    }

    /**
     * Convert a {@link NodeList} to a Java {@link List} of {@link Element}s.
     * @param _nodeList collection of nodes
     * @return list of elements
     */
    public static List<Element> convertToElementList(NodeList _nodeList) {
        List<Element> elemList = new ArrayList<>();
        for (int i = 0; i < _nodeList.getLength(); i++) {
            Element elem = (Element) _nodeList.item(i);
            elemList.add(elem);
        }
        return elemList;
    }

    /**
     * Converts {@link NamedNodeMap} to a {@link LinkedHashMap}&lt;String,String&gt;.
     * @param _nodeMap node map
     * @return {@link LinkedHashMap}, maybe empty but never null
     */
    public static Map<String, String> convertToAttributeMap(NamedNodeMap _nodeMap) {
        Map<String, String> map = new LinkedHashMap<>();
        for (int i = 0; i < _nodeMap.getLength(); i++) {
            Node node = _nodeMap.item(i);
            map.put(node.getNodeName(), node.getNodeValue());
        }
        return map;
    }

    /**
     * Loads XML from string and uses referenced XSD to validate the content.
     *
     *
     * @param _xmlStr string to validate
     * @param _namespaceAware take care of namespace
     * @param _errorHandler e.g. {@link XmlErrorHandlers.XmlErrorHandlerQuiet} or {@link XmlErrorHandlers.XmlErrorHandlerRuntimeException}
     * @return Document
     * @throws IOException on error
     */
    public static Document parseXmlStringWithXsdValidation(String _xmlStr, boolean _namespaceAware, ErrorHandler _errorHandler) throws IOException  {
        ErrorHandler handler = _errorHandler;

        if (_errorHandler == null) {
            handler = new XmlErrorHandlers.XmlErrorHandlerQuiet();
        }

        DocumentBuilderFactory dbFac = DocumentBuilderFactory.newInstance();
        dbFac.setValidating(true);
        dbFac.setNamespaceAware(_namespaceAware);
        dbFac.setAttribute("http://java.sun.com/xml/jaxp/properties/schemaLanguage",
                     "http://www.w3.org/2001/XMLSchema");
        try {
            DocumentBuilder builder = dbFac.newDocumentBuilder();
            builder.setErrorHandler(handler);
            return builder.parse(new ByteArrayInputStream(_xmlStr.getBytes(StandardCharsets.UTF_8)));

        } catch (IOException _ex) {
            throw _ex;
        } catch (Exception _ex) {
            throw new IOException("Failed to parse " + Util.abbreviate(_xmlStr, 500), _ex);
        }
    }

    /**
     * Loads XML from string and uses referenced XSD to validate the content.
     * This method will use {@link XmlErrorHandlerQuiet} to suppress all errors/warnings when validating.
     *
     * @param _xmlStr string to validate
     * @param _namespaceAware take care of namespace
     * @return Document
     * @throws IOException on error
     */
    public static Document parseXmlStringWithXsdValidation(String _xmlStr, boolean _namespaceAware) throws IOException  {
        return parseXmlStringWithXsdValidation(_xmlStr, _namespaceAware, null);
    }

    /**
     * Dump a {@link Document} or {@link Node}-compatible object to the given {@link OutputStream} (e.g. System.out).
     *
     * @param _docOrNode {@link Document} or {@link Node} object
     * @param _outStream {@link OutputStream} to print on
     * @throws IOException on error
     */
    public static void printDocument(Node _docOrNode, OutputStream _outStream) throws IOException {
        if (_docOrNode == null || _outStream == null) {
            throw new IOException("Cannot print (on) 'null' object");
        }

        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer;
        try {
            transformer = tf.newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
            transformer.setOutputProperty(OutputKeys.METHOD, "xml");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");

            transformer.transform(new DOMSource(_docOrNode),
                 new StreamResult(new OutputStreamWriter(_outStream, StandardCharsets.UTF_8)));
        } catch (TransformerException _ex) {
            throw new IOException("Could not print Document or Node.", _ex);
        }

    }
}
