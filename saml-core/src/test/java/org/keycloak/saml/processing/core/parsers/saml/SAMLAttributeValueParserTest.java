package org.keycloak.saml.processing.core.parsers.saml;

import org.junit.Assert;
import org.junit.Test;
import org.keycloak.saml.common.parsers.AbstractParser;
import org.keycloak.saml.processing.core.parsers.saml.assertion.SAMLAttributeValueParser;

import javax.xml.stream.XMLEventReader;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class SAMLAttributeValueParserTest {

    private static final String XML_DOC =
        "<saml2:Attribute xmlns:saml2=\"urn:oasis:names:tc:SAML:2.0:assertion\"\n>"
        + "  <saml2:AttributeValue xmlns:myCustomType=\"http://www.whatever.de/schema/myCustomType/saml/extensions\"\n"
        + "                        xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:type=\"myCustomType:AddressType\">\n"
        + "    <myCustomType:Street>Zillestraße</myCustomType:Street>\n"
        + "    <myCustomType:HouseNumber>17</myCustomType:HouseNumber>\n"
        + "    <myCustomType:ZipCode>10585</myCustomType:ZipCode>\n"
        + "    <myCustomType:City>Berlin</myCustomType:City>\n"
        + "    <myCustomType:Country>DE</myCustomType:Country>\n"
        + "  </saml2:AttributeValue>\n"
        + "</saml2:Attribute>";

    @Test
    public void parsesAttributeValueElementWithCustomTypes() throws Exception {
        InputStream input = new ByteArrayInputStream(XML_DOC.getBytes(StandardCharsets.UTF_8));
        XMLEventReader xmlEventReader = AbstractParser.createEventReader(input);
        xmlEventReader.nextEvent();
        final Object attributeValue = SAMLAttributeValueParser.getInstance().parse(xmlEventReader);

        Assert.assertTrue(attributeValue instanceof String);
        Assert.assertEquals("<myCustomType:Street>Zillestraße</myCustomType:Street>"
            + "<myCustomType:HouseNumber>17</myCustomType:HouseNumber>"
            + "<myCustomType:ZipCode>10585</myCustomType:ZipCode>"
            + "<myCustomType:City>Berlin</myCustomType:City>"
            + "<myCustomType:Country>DE</myCustomType:Country>", attributeValue);
    }
}
