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
package org.keycloak.subsystem.adapter.saml.extension;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.jboss.as.subsystem.test.AbstractSubsystemBaseTest;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;


/**
 * Tests all management expects for subsystem, parsing, marshaling, model definition and other
 * Here is an example that allows you a fine grained controller over what is tested and how. So it can give you ideas what can be done and tested.
 * If you have no need for advanced testing of subsystem you look at {@link AbstractSubsystemBaseTest} that testes same stuff but most of the code
 * is hidden inside of test harness
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @author Tomaz Cerar
 * @author <a href="marko.strukelj@gmail.com">Marko Strukelj</a>
 */
public class SubsystemParsingTestCase extends AbstractSubsystemBaseTest {

    private String subsystemXml = null;

    private String subsystemTemplate = null;

    private Document document = null;

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    public SubsystemParsingTestCase() {
        super(KeycloakSamlExtension.SUBSYSTEM_NAME, new KeycloakSamlExtension());
    }

    @Override
    protected String getSubsystemXml() throws IOException {
        return this.subsystemXml;
    }

    @Override
    protected String getSubsystemXsdPath() throws Exception {
        return "schema/wildfly-keycloak-saml_1_4.xsd";
    }

    @Override
    protected String[] getSubsystemTemplatePaths() throws IOException {
        return new String[]{
                "/subsystem-templates/keycloak-saml-adapter.xml"
        };
    }

    @Before
    public void initialize() throws IOException {
        this.subsystemTemplate = readResource("keycloak-saml-1.4.xml");
        try {
            DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            this.document = builder.parse(new InputSource(new StringReader(this.subsystemTemplate)));
        } catch (ParserConfigurationException | SAXException e) {
            throw new IOException(e);
        }
    }

    private void buildSubsystemXml(final Element element, final String expression) throws IOException {
        if (element != null) {
            try {
                // locate the element and insert the node
                XPath xPath = XPathFactory.newInstance().newXPath();
                NodeList nodeList = (NodeList) xPath.compile(expression).evaluate(this.document, XPathConstants.NODESET);
                nodeList.item(0).appendChild(element);
                // transform again to XML
                TransformerFactory tf = TransformerFactory.newInstance();
                Transformer transformer = tf.newTransformer();
                transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
                StringWriter writer = new StringWriter();
                transformer.transform(new DOMSource(this.document), new StreamResult(writer));
                this.subsystemXml = writer.getBuffer().toString();
            } catch(TransformerException | XPathExpressionException e) {
                throw new IOException(e);
            }
        } else {
            this.subsystemXml = this.subsystemTemplate;
        }
    }

    @Override
    public void testSubsystem() throws Exception {
        this.buildSubsystemXml(null, null);
        super.testSubsystem();
    }

    @Override
    public void testSchema() throws Exception {
        this.buildSubsystemXml(null, null);
        super.testSchema();
    }

    @Test
    public void testDuplicateServiceProviders() throws Exception {
        // create a simple service provider element.
        Element spElement = this.document.createElement(Constants.XML.SERVICE_PROVIDER);
        spElement.setAttribute(Constants.XML.ENTITY_ID, "duplicate-sp");
        this.buildSubsystemXml(spElement, "/subsystem/secure-deployment[1]");

        this.exception.expect(XMLStreamException.class);
        this.exception.expectMessage("WFLYCTL0198: Unexpected element");
        super.testSubsystem();
    }

    @Test
    public void testDuplicateIdentityProviders() throws Exception {
        // create a duplicate identity provider element.
        Element idpElement = this.document.createElement(Constants.XML.IDENTITY_PROVIDER);
        idpElement.setAttribute(Constants.XML.ENTITY_ID, "test-idp");
        Element singleSignOn = this.document.createElement(Constants.XML.SINGLE_SIGN_ON);
        singleSignOn.setAttribute(Constants.XML.BINDING_URL, "https://localhost:7887");
        Element singleLogout = this.document.createElement(Constants.XML.SINGLE_LOGOUT);
        singleLogout.setAttribute(Constants.XML.POST_BINDING_URL, "httpsL//localhost:8998");
        idpElement.appendChild(singleSignOn);
        idpElement.appendChild(singleLogout);
        this.buildSubsystemXml(idpElement, "/subsystem/secure-deployment[1]/SP");

        this.exception.expect(XMLStreamException.class);
        this.exception.expectMessage("WFLYCTL0198: Unexpected element");
        super.testSubsystem();
    }

    @Test
    public void testDuplicateKeysInSP() throws Exception {
        Element keysElement = this.document.createElement(Constants.XML.KEYS);
        Element keyElement = this.document.createElement(Constants.XML.KEY);
        keyElement.setAttribute(Constants.XML.ENCRYPTION, "false");
        keyElement.setAttribute(Constants.XML.SIGNING, "false");
        keysElement.appendChild(keyElement);
        this.buildSubsystemXml(keysElement, "/subsystem/secure-deployment[1]/SP");

        this.exception.expect(XMLStreamException.class);
        this.exception.expectMessage("WFLYCTL0198: Unexpected element");
        super.testSubsystem();
    }

    @Test
    public void testDuplicateKeysInIDP() throws Exception {
        Element keysElement = this.document.createElement(Constants.XML.KEYS);
        Element keyElement = this.document.createElement(Constants.XML.KEY);
        keyElement.setAttribute(Constants.XML.ENCRYPTION, "false");
        keyElement.setAttribute(Constants.XML.SIGNING, "false");
        keysElement.appendChild(keyElement);
        this.buildSubsystemXml(keysElement, "/subsystem/secure-deployment[1]/SP/IDP");

        this.exception.expect(XMLStreamException.class);
        this.exception.expectMessage("WFLYCTL0198: Unexpected element");
        super.testSubsystem();
    }
}
