package org.keycloak.saml.processing.core.parsers.saml;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;

import org.keycloak.dom.saml.v2.assertion.AttributeType;
import org.keycloak.saml.common.parsers.AbstractParser;
import org.keycloak.saml.processing.core.parsers.saml.assertion.SAMLAttributeParser;

import org.junit.Assert;
import org.junit.Test;

public class SAMLAttributeParserTest {

    private static String XML_DOC_TEMPLATE = "<samlp:AttributeQuery\n" +
            "    xmlns:saml=\"urn:oasis:names:tc:SAML:2.0:assertion\"\n" +
            "    xmlns:samlp=\"urn:oasis:names:tc:SAML:2.0:protocol\">\n" +
            "${ATTRIBUTE_ELEMENT}\n" +
            "</samlp:AttributeQuery>";

    @Test
    public void parsesAttributeElementWithKnownAttributesCorrectly() throws Exception {

        String nameFormatValue = "urn:oasis:names:tc:SAML:2.0:attrname-format:uri";
        String nameValue = "urn:oid:2.5.4.42";
        String friendlyNameValue = "givenName";

        AttributeType attributeType = parseAttributeElement("<saml:Attribute NameFormat=\"" + nameFormatValue + "\" Name=\"" + nameValue + "\" FriendlyName=\"" + friendlyNameValue + "\"/>");

        Assert.assertEquals(nameFormatValue, attributeType.getNameFormat());
        Assert.assertEquals(nameValue, attributeType.getName());
        Assert.assertEquals(friendlyNameValue, attributeType.getFriendlyName());
        Assert.assertTrue("Other attributes should be empty", attributeType.getOtherAttributes().isEmpty());
    }

    @Test
    public void parsesAttributeElementWithKnownAndX509_ENCODINGAttributesCorrectly() throws Exception {

        String nameFormatValue = "urn:oasis:names:tc:SAML:2.0:attrname-format:uri";
        String nameValue = "urn:oid:2.5.4.42";
        String friendlyNameValue = "givenName";
        String encodingValue = "LDAP";

        String x500Namespace = "urn:oasis:names:tc:SAML:2.0:profiles:attribute:X500";
        AttributeType attributeType = parseAttributeElement(String.format("<saml:Attribute xmlns:x500=\"%s\" " + //
                        "NameFormat=\"%s\" Name=\"%s\" FriendlyName=\"%s\" x500:Encoding=\"%s\"/>", x500Namespace, //
                nameFormatValue, nameValue, friendlyNameValue, encodingValue));

        Assert.assertEquals(nameFormatValue, attributeType.getNameFormat());
        Assert.assertEquals(nameValue, attributeType.getName());
        Assert.assertEquals(friendlyNameValue, attributeType.getFriendlyName());
        Assert.assertTrue("Other attributes should not be empty", !attributeType.getOtherAttributes().isEmpty());
        Assert.assertEquals(encodingValue, attributeType.getOtherAttributes().get(new QName(x500Namespace, "Encoding")));
    }

    @Test
    public void parsesAttributeElementWithKnownAndOtherAttributesCorrectly() throws Exception {

        String nameFormatValue = "urn:oasis:names:tc:SAML:2.0:attrname-format:uri";
        String nameValue = "urn:oid:2.5.4.42";
        String friendlyNameValue = "givenName";

        String someNs = "https://www.thenamespace.ns/path";
        String someValue1 = "v1";
        String someValue2 = "v2";

        AttributeType attributeType = parseAttributeElement(String.format("<saml:Attribute xmlns:somens=\"%s\" " + //
                        "NameFormat=\"%s\" Name=\"%s\" FriendlyName=\"%s\" somens:Value1=\"%s\" somens:Value2=\"%s\"/>", someNs, //
                nameFormatValue, nameValue, friendlyNameValue, someValue1, someValue2));

        Assert.assertEquals(nameFormatValue, attributeType.getNameFormat());
        Assert.assertEquals(nameValue, attributeType.getName());
        Assert.assertEquals(friendlyNameValue, attributeType.getFriendlyName());
        Assert.assertTrue("Other attributes should not be empty", !attributeType.getOtherAttributes().isEmpty());
        Assert.assertEquals(someValue1, attributeType.getOtherAttributes().get(new QName(someNs, "Value1")));
        Assert.assertEquals(someValue2, attributeType.getOtherAttributes().get(new QName(someNs, "Value2")));
    }

    protected AttributeType parseAttributeElement(String attributeXml) throws Exception {

        String xmlDoc = XML_DOC_TEMPLATE.replace("${ATTRIBUTE_ELEMENT}", attributeXml);
        InputStream input = new ByteArrayInputStream(xmlDoc.getBytes(StandardCharsets.UTF_8));
        XMLEventReader xmlEventReader = AbstractParser.createEventReader(input);
        xmlEventReader.nextEvent();
        return SAMLAttributeParser.getInstance().parse(xmlEventReader);
    }
}
