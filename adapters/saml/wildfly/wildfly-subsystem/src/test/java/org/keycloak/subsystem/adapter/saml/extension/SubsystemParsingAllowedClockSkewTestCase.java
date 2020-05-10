/*
 * Copyright 2019 Red Hat, Inc. and/or its affiliates
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

import org.jboss.as.subsystem.test.AbstractSubsystemBaseTest;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Properties;
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
import org.jboss.as.controller.ExpressionResolver;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.model.test.ModelTestUtils;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.dmr.ModelNode;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * Test case for AllowedClockSkew subsystem configuration.
 *
 * @author rmartinc
 */
public class SubsystemParsingAllowedClockSkewTestCase extends AbstractSubsystemBaseTest {

    private String subsystemXml = null;

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    public SubsystemParsingAllowedClockSkewTestCase() {
        super(KeycloakSamlExtension.SUBSYSTEM_NAME, new KeycloakSamlExtension());
    }

    @Override
    protected String getSubsystemXml() throws IOException {
        return subsystemXml;
    }

    @Override
    protected String getSubsystemXsdPath() throws Exception {
        return "schema/wildfly-keycloak-saml_1_3.xsd";
    }

    @Override
    protected String[] getSubsystemTemplatePaths() throws IOException {
        return new String[]{
                "/subsystem-templates/keycloak-saml-adapter.xml"
        };
    }

    @Override
    protected Properties getResolvedProperties() {
        return System.getProperties();
    }

    private void setSubsystemXml(String value, String unit) throws IOException {
        try {
            String template = readResource("keycloak-saml-1.3.xml");
            if (value != null) {
                // assign the AllowedClockSkew element using DOM
                DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
                Document doc = db.parse(new InputSource(new StringReader(template)));
                // create the skew element
                Element allowedClockSkew = doc.createElement(Constants.XML.ALLOWED_CLOCK_SKEW);
                if (unit != null) {
                    allowedClockSkew.setAttribute(Constants.XML.ALLOWED_CLOCK_SKEW_UNIT, unit);
                }
                allowedClockSkew.setTextContent(value);
                // locate the IDP and insert the node
                XPath xPath = XPathFactory.newInstance().newXPath();
                NodeList nodeList = (NodeList) xPath.compile("/subsystem/secure-deployment[1]/SP/IDP").evaluate(doc, XPathConstants.NODESET);
                nodeList.item(0).appendChild(allowedClockSkew);
                // transform again to XML
                TransformerFactory tf = TransformerFactory.newInstance();
                Transformer transformer = tf.newTransformer();
                transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
                StringWriter writer = new StringWriter();
                transformer.transform(new DOMSource(doc), new StreamResult(writer));
                subsystemXml = writer.getBuffer().toString();
            } else {
                subsystemXml = template;
            }
        } catch (DOMException | ParserConfigurationException | SAXException | TransformerException | XPathExpressionException e) {
            throw new IOException(e);
        }
    }

    private PathAddress getIdpPath() {
        return PathAddress.EMPTY_ADDRESS
                .append(PathElement.pathElement(ModelDescriptionConstants.SUBSYSTEM, KeycloakSamlExtension.SUBSYSTEM_NAME))
                .append(PathElement.pathElement(Constants.Model.SECURE_DEPLOYMENT, "my-app.war"))
                .append(PathElement.pathElement(Constants.Model.SERVICE_PROVIDER, "http://localhost:8080/sales-post-enc/"))
                .append(PathElement.pathElement(Constants.Model.IDENTITY_PROVIDER, "idp"));
    }

    private void testSubsystem(String value, String unit, int realValue, String realUnit) throws Exception {
        setSubsystemXml(value, unit);
        // perform the common test
        KernelServices s = super.standardSubsystemTest(null, true);
        // get the values for the AllowedClockSkew parameters
        ModelNode idp = ModelTestUtils.getSubModel(s.readWholeModel(), getIdpPath());
        ModelNode allowedClockSkew = idp.get(Constants.Model.ALLOWED_CLOCK_SKEW);
        if (value != null) {
            Assert.assertTrue(allowedClockSkew.isDefined());
            ModelNode allowedClockSkewValue = allowedClockSkew.get(Constants.Model.ALLOWED_CLOCK_SKEW_VALUE);
            ModelNode allowedClockSkewUnit = allowedClockSkew.get(Constants.Model.ALLOWED_CLOCK_SKEW_UNIT);
            allowedClockSkewValue = ExpressionResolver.TEST_RESOLVER.resolveExpressions(allowedClockSkewValue);
            allowedClockSkewUnit = ExpressionResolver.TEST_RESOLVER.resolveExpressions(allowedClockSkewUnit);
            Assert.assertEquals(realValue, allowedClockSkewValue.asInt());
            if (unit != null) {
                Assert.assertEquals(realUnit, allowedClockSkewUnit.asString());
            } else {
                Assert.assertFalse(allowedClockSkewUnit.isDefined());
            }
        } else {
            Assert.assertFalse(allowedClockSkew.isDefined());
        }
    }

    private void testSubsystem(String value, String unit) throws Exception {
        testSubsystem(value, unit, value == null? -1 : Integer.parseInt(value.trim()), unit);
    }

    private void testSchema(String value, String unit) throws Exception {
        setSubsystemXml(value, unit);
        super.testSchema();
    }

    @Test
    @Override
    public void testSubsystem() throws Exception {
        testSubsystem(null, null);
    }

    @Test
    @Override
    public void testSchema() throws Exception {
        testSchema(null, null);
    }

    @Test
    public void testSubsystemAllowedClockSkewWithUnit() throws Exception {
        testSubsystem("3500", "MILLISECONDS");
    }

    @Test
    public void testSchemaAllowedClockSkewWithUnit() throws Exception {
        testSchema("3500", "MILLISECONDS");
    }

    @Test
    public void testSubsystemAllowedClockSkewWithoutUnit() throws Exception {
        testSubsystem("1", null);
    }

    @Test
    public void testSchemaAllowedClockSkewWithoutUnit() throws Exception {
        testSchema("1", null);
    }

    @Test
    public void testSubsystemAllowedClockSkewWithSpaces() throws Exception {
        testSubsystem("\n  20  \n  ", null);
    }

    @Test
    public void testErrorOnNonInteger() throws Exception {
        exception.expect(XMLStreamException.class);
        exception.expectMessage("WFLYCTL0097");
        testSubsystem("invalid-value", null, -1, null);
    }

    @Test
    public void testErrorOnNonPositiveInteger() throws Exception {
        exception.expect(XMLStreamException.class);
        exception.expectMessage("WFLYCTL0117");
        testSubsystem("0", null);
    }

    @Test
    public void testErrorNoValidUnit() throws Exception {
        exception.expect(XMLStreamException.class);
        exception.expectMessage("WFLYCTL0248");
        testSubsystem("30", "invalid-unit");
    }

    @Test
    public void testExpression() throws Exception {
        System.setProperty("test.prop.SKEW_TIME", "30");
        System.setProperty("test.prop.SKEW_UNIT", "MILLISECONDS");
        try {
            testSubsystem("${test.prop.SKEW_TIME}", "${test.prop.SKEW_UNIT}", 30, "MILLISECONDS");
        } finally {
            System.clearProperty("test.prop.SKEW_TIME");
            System.clearProperty("test.prop.SKEW_UNIT");
        }
    }
}
