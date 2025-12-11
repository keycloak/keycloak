package org.keycloak.saml.processing.core.saml.v2.writers;

import java.io.ByteArrayOutputStream;

import org.keycloak.dom.saml.v2.assertion.AttributeStatementType;
import org.keycloak.dom.saml.v2.assertion.AttributeType;
import org.keycloak.saml.common.constants.GeneralConstants;
import org.keycloak.saml.common.constants.JBossSAMLURIConstants;
import org.keycloak.saml.common.exceptions.ConfigurationException;
import org.keycloak.saml.common.exceptions.ProcessingException;
import org.keycloak.saml.common.util.DocumentUtil;
import org.keycloak.saml.common.util.StaxUtil;

import org.junit.Assert;
import org.junit.Test;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class XUATokenWriterTest {

    @Test
    public void testXUAToken() throws ConfigurationException, ProcessingException {
        Document document = DocumentUtil.createDocument();

        AttributeType roleAttr = new AttributeType("urn:oasis:names:tc:xacml:2.0:subject:role");

        Element role = document.createElementNS("urn:hl7-org:v3", "Role");
        role.setAttributeNS("urn:hl7-org:v3", "code", "46255001");
        role.setAttributeNS("urn:hl7-org:v3", "codeSystem", "2.16.840.1.113883.6.96");
        role.setAttributeNS("urn:hl7-org:v3", "codeSystemName", "SNOMED_CT");
        role.setAttributeNS("urn:hl7-org:v3", "displayName", "Pharmacist");
        Attr attrCEType = document.createAttributeNS(JBossSAMLURIConstants.XSI_NSURI.get(), "type");
        attrCEType.setValue("CE");
        attrCEType.setPrefix("xsi");
        role.setAttributeNodeNS(attrCEType);

        roleAttr.addAttributeValue(role);

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        SAMLAssertionWriter samlAssertionWriter =
                new SAMLAssertionWriter(StaxUtil.getXMLStreamWriter(byteArrayOutputStream));

        AttributeStatementType attributeStatementType = new AttributeStatementType();
        attributeStatementType.addAttribute(new AttributeStatementType.ASTChoiceType(roleAttr));

        samlAssertionWriter.write(attributeStatementType);

        String serializedAssertion = new String(byteArrayOutputStream.toByteArray(), GeneralConstants.SAML_CHARSET);
        Assert.assertEquals("<saml:AttributeStatement>"
                        + "<saml:Attribute Name=\"urn:oasis:names:tc:xacml:2.0:subject:role\">"
                        + "<saml:AttributeValue>"
                        + "<Role xmlns=\"urn:hl7-org:v3\" code=\"46255001\" codeSystem=\"2.16.840.1.113883.6.96\" "
                        + "codeSystemName=\"SNOMED_CT\" displayName=\"Pharmacist\" "
                        + "xsi:type=\"CE\"></Role></saml:AttributeValue></saml:Attribute>"
                        + "</saml:AttributeStatement>",
                serializedAssertion);
    }
}
