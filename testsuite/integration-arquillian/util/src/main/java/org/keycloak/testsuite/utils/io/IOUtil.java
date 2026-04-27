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
package org.keycloak.testsuite.utils.io;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.util.concurrent.TimeUnit;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.util.JsonSerialization;

import org.jboss.logging.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 *
 * @author tkyjovsk
 */
public class IOUtil {

    private static final Logger log = Logger.getLogger(IOUtil.class);

    public static final File PROJECT_BUILD_DIRECTORY = new File(System.getProperty("project.build.directory", "target"));

    public static <T> T loadJson(InputStream is, Class<T> type) {
        try {
            return JsonSerialization.readValue(is, type);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load json.", e);
        }
    }

    public static RealmRepresentation loadRealm(String realmConfig) {
        return loadRealm(IOUtil.class.getResourceAsStream(realmConfig));
    }

    public static RealmRepresentation loadRealm(File realmFile) {
        try {
            return loadRealm(new FileInputStream(realmFile));
        } catch (FileNotFoundException ex) {
            throw new IllegalStateException("Test realm file not found: " + realmFile, ex);
        }
    }

    public static RealmRepresentation loadRealm(InputStream is) {
        RealmRepresentation realm = loadJson(is, RealmRepresentation.class);
        System.out.println("Loaded realm " + realm.getRealm());
        return realm;
    }

    public static Document loadXML(InputStream is) {
        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            return dBuilder.parse(is);
        } catch (ParserConfigurationException | SAXException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String documentToString(Document newDoc) {
        try {
            DOMSource domSource = new DOMSource(newDoc);
            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            StringWriter sw = new StringWriter();
            StreamResult sr = new StreamResult(sw);
            transformer.transform(domSource, sr);
            return sw.toString();
        } catch (TransformerException e) {
            log.error("Can't transform document to String");
            throw new RuntimeException(e);
        }
    }

    public static InputStream documentToInputStream(Document doc) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            Source xmlSource = new DOMSource(doc);
            Result outputTarget = new StreamResult(outputStream);
            TransformerFactory.newInstance().newTransformer().transform(xmlSource, outputTarget);
            return new ByteArrayInputStream(outputStream.toByteArray());
        } catch (TransformerException e) {
            log.error("Can't transform document to InputStream");
            throw new RuntimeException(e);
        }
    }

    /**
     * Modifies attribute value according to the given regex (first occurrence) iff 
     * there are following conditions accomplished:
     * 
     *  - exactly one node is found within the document
     *  - the attribute of the node exists
     *  - the regex is found in the value of the attribute
     * 
     * Otherwise there is nothing changed.
     * 
     * @param doc
     * @param tagName
     * @param attributeName
     * @param regex
     * @param replacement
     */
    public static void modifyDocElementAttribute(Document doc, String tagName, String attributeName, String regex, String replacement) {
        NodeList nodes = doc.getElementsByTagName(tagName);
        if (nodes.getLength() != 1) {
            log.warn("Not able or ambiguous to find element: " + tagName);
            return;
        }

        Node node = nodes.item(0).getAttributes().getNamedItem(attributeName);
        if (node == null || node.getTextContent() == null) {
            log.warn("Not able to find attribute " + attributeName + " within element: " + tagName);
            return;
        }
        node.setTextContent(node.getTextContent().replaceFirst(regex, replacement));
    }

    public static void removeNodeByAttributeValue(Document doc, String parentTag, String tagName, String attributeName, String value){
        NodeList parentNodes = doc.getElementsByTagName(parentTag);
        if (parentNodes.getLength() != 1) {
            log.warn("Not able or ambiguous to find element: " + parentTag);
            return;
        }

        Element parentElement = (Element) parentNodes.item(0);
        if (parentElement == null) {
            log.warn("Not able to find element: " + parentTag);
            return;
        }

        NodeList nodes = doc.getElementsByTagName(tagName);
        for (int i = 0; i < nodes.getLength(); i++){
            Node node = nodes.item(i).getAttributes().getNamedItem(attributeName);
            if (node.getTextContent().equals(value)){
                parentElement.removeChild(nodes.item(i));
                return;
            }
        }
    }

    /**
     * Modifies element text value according to the given regex (first occurrence) iff 
     * there are following conditions accomplished:
     * 
     *  - exactly one node is found within the document
     *  - the regex is found in the text content of the element
     * 
     * Otherwise there is nothing changed.
     * 
     * @param doc
     * @param tagName
     * @param regex
     * @param replacement 
     */
    public static void modifyDocElementValue(Document doc, String tagName, String regex, String replacement) {
        NodeList nodes = doc.getElementsByTagName(tagName);
        if (nodes.getLength() != 1) {
            log.warn("Not able or ambiguous to find element: " + tagName);
            return;
        }

        Node node = nodes.item(0);
        if (node == null) {
            log.warn("Not able to find element: " + tagName);
            return;
        }

        node.setTextContent(node.getTextContent().replaceFirst(regex, replacement));
    }

    public static void setDocElementAttributeValue(Document doc, String tagName, String attributeName, String value) {
        NodeList nodes = doc.getElementsByTagName(tagName);
        if (nodes.getLength() != 1) {
            log.warn("Not able or ambiguous to find element: " + tagName);
            return;
        }

        Element node = (Element) nodes.item(0);
        if (node == null) {
            log.warn("Not able to find element: " + tagName);
            return;
        }

        node.setAttribute(attributeName, value);
    }

    public static void removeElementsFromDoc(Document doc, String parentTag, String removeNode) {
        NodeList nodes = doc.getElementsByTagName(parentTag);
        if (nodes.getLength() != 1) {
            log.warn("Not able or ambiguous to find element: " + parentTag);
            return;
        }

        Element parentElement = (Element) nodes.item(0);
        if (parentElement == null) {
            log.warn("Not able to find element: " + parentTag);
            return;
        }

        NodeList removeNodes = parentElement.getElementsByTagName(removeNode);
        if (removeNodes == null) {
            log.warn("Not able to find element: " + removeNode + " within node " + parentTag);
            return;
        }

        for (int i = 0; i < removeNodes.getLength();){
            Element removeElement = (Element) removeNodes.item(i);
            if (removeElement == null) {
                log.warn("Not able to find element: " + removeNode + " within node " + parentTag);
                return;
            }

            log.trace("Removing node " + removeNode);
            parentElement.removeChild(removeElement);

        }

    }

    public static String getElementTextContent(Document doc, String path) {
        String[] pathSegments = path.split("/");

        Element currentElement = (Element) doc.getElementsByTagName(pathSegments[0]).item(0);
        if (currentElement == null) {
            log.warn("Not able to find element: " + pathSegments[0] + " in document");
            return null;
        }

        for (int i = 1; i < pathSegments.length; i++) {
            currentElement = (Element) currentElement.getElementsByTagName(pathSegments[i]).item(0);

            if (currentElement == null) {
                log.warn("Not able to find element: " + pathSegments[i] + " in " + pathSegments[i - 1]);
                return null;
            }
        }

        return currentElement.getTextContent();
    }

    public static void appendChildInDocument(Document doc, String parentPath, Element node) {
        String[] pathSegments = parentPath.split("/");

        Element currentElement = (Element) doc.getElementsByTagName(pathSegments[0]).item(0);
        if (currentElement == null) {
            log.warn("Not able to find element: " + pathSegments[0] + " in document");
            return;
        }

        for (int i = 1; i < pathSegments.length; i++) {
            currentElement = (Element) currentElement.getElementsByTagName(pathSegments[i]).item(0);

            if (currentElement == null) {
                log.warn("Not able to find element: " + pathSegments[i] + " in " + pathSegments[i - 1]);
                return;
            }
        }

        currentElement.appendChild(node);
    }

    public static void removeElementFromDoc(Document doc, String path) {
        String[] pathSegments = path.split("/");

        Element currentElement = (Element) doc.getElementsByTagName(pathSegments[0]).item(0);
        if (currentElement == null) {
            log.warn("Not able to find element: " + pathSegments[0] + " in document");
            return;
        }

        for (int i = 1; i < pathSegments.length; i++) {
            currentElement = (Element) currentElement.getElementsByTagName(pathSegments[i]).item(0);

            if (currentElement == null) {
                log.warn("Not able to find element: " + pathSegments[i] + " in " + pathSegments[i - 1]);
                return;
            }
        }

        currentElement.getParentNode().removeChild(currentElement);
    }

    public static void execCommand(String command, File dir) throws IOException, InterruptedException {
        Process process = Runtime.getRuntime().exec(command, null, dir);
        if (process.waitFor(10, TimeUnit.SECONDS)) {
            if (process.exitValue() != 0) {
                getOutput("ERROR", process.getErrorStream());
                throw new RuntimeException("Adapter installation failed. Process exitValue: "
                        + process.exitValue());
            }
            getOutput("OUTPUT", process.getInputStream());
            log.debug("process.isAlive(): " + process.isAlive());
        } else {
            if (process.isAlive()) {
                process.destroyForcibly();
            }
            throw new RuntimeException("Timeout after 10 seconds.");
        }
    }

    public static void getOutput(String type, InputStream is) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder builder = new StringBuilder();
        builder.append("<").append(type).append(">");
        System.out.println(builder);
        builder = new StringBuilder();
        while (reader.ready()) {
            System.out.println(reader.readLine());
        }
        builder.append("</").append(type).append(">");
        System.out.println(builder);
    }

}
