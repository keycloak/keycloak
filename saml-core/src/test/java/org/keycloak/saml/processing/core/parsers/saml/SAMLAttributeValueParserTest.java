package org.keycloak.saml.processing.core.parsers.saml;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.events.XMLEvent;

import org.keycloak.saml.common.parsers.AbstractParser;
import org.keycloak.saml.processing.core.parsers.saml.assertion.SAMLAttributeValueParser;

import org.junit.Assert;
import org.junit.Test;

public class SAMLAttributeValueParserTest {

    private static final String XML_DOC = "Some Text";

    private static final String XML_DOC_NESTED_ELEMENTS =
            "<%1$sStreet%2$s>Zillestraße</%1$sStreet><%1$sHouseNumber%2$s>17</%1$sHouseNumber>"
            + "<%1$sZipCode%2$s>10585</%1$sZipCode><%1$sCity%2$s>Berlin</%1$sCity><%1$sCountry%2$s>DE</%1$sCountry>";

    private static final String XML_DOC_SINGLE_ELEMENT =
            "<%1$sAddressType%2$s>" + String.format(XML_DOC_NESTED_ELEMENTS, "%1$s", "") + "</%1$sAddressType>";

    private static final String XML_DOC_NESTED_WITHOUT_PREFIX_AND_NAMESPACE = String.format(XML_DOC_NESTED_ELEMENTS, "", "");

    @Test
    public void parsesAttributeValueUserType() throws Exception {
        Object actualAttributeValue = parseAttributeValue("xsi:type=\"myCustomType:Something\"", "\n    " + XML_DOC + "\n   ");
        Assert.assertEquals(XML_DOC, actualAttributeValue);
    }

    @Test
    public void parsesAttributeValueUserTypeWithNamespace() throws Exception {
        Object actualAttributeValue = parseAttributeValue(
                "xmlns:myCustomType=\"http://my.custom.de/schema/saml/extensions\" xsi:type=\"myCustomType:Something\"",
                "\n    " + XML_DOC + "\n   ");
        Assert.assertEquals(XML_DOC, actualAttributeValue);
    }

    @Test
    public void parseAttributeValueAnyType() throws Exception {
        Object actualAttributeValue = parseAttributeValue("xsi:type=\"xs:anyType\"", XML_DOC_NESTED_WITHOUT_PREFIX_AND_NAMESPACE);
        Assert.assertEquals(XML_DOC_NESTED_WITHOUT_PREFIX_AND_NAMESPACE, actualAttributeValue);
    }

    @Test
    public void parsesAttributeValueUserTypeWithSingleElements() throws Exception {
        final String xmlDoc = String.format(XML_DOC_SINGLE_ELEMENT, "", "");
        Object actualAttributeValue = parseAttributeValue("xsi:type=\"AddressType\"", xmlDoc);
        Assert.assertEquals(xmlDoc, actualAttributeValue);
    }

    @Test
    public void parsesAttributeValueUserTypeWithSingleElementsAndNamespace() throws Exception {
        Object actualAttributeValue = parseAttributeValue(
                "xmlns:myCustomType=\"http://my.custom.de/schema/saml/extensions\" xsi:type=\"myCustomType:AddressType\"",
                String.format(XML_DOC_SINGLE_ELEMENT, "myCustomType:", ""));
        Assert.assertEquals(String.format(XML_DOC_SINGLE_ELEMENT, "myCustomType:",
                        " xmlns:myCustomType=\"http://my.custom.de/schema/saml/extensions\""),
                actualAttributeValue);
    }

    @Test
    public void parsesAttributeValueUserTypeWithNestedElements() throws Exception {
        Object actualAttributeValue = parseAttributeValue("xsi:type=\"AddressType\"", XML_DOC_NESTED_WITHOUT_PREFIX_AND_NAMESPACE);
        Assert.assertEquals(XML_DOC_NESTED_WITHOUT_PREFIX_AND_NAMESPACE, actualAttributeValue);
    }

    @Test
    public void parsesAttributeValueUserTypeWithNestedElementsAndNamespace() throws Exception {
        Object actualAttributeValue = parseAttributeValue(
                "xmlns:myCustomType=\"http://my.custom.de/schema/saml/extensions\" xsi:type=\"myCustomType:AddressType\"",
                String.format(XML_DOC_NESTED_ELEMENTS, "myCustomType:", ""));
        Assert.assertEquals(String.format(XML_DOC_NESTED_ELEMENTS, "myCustomType:",
                        " xmlns:myCustomType=\"http://my.custom.de/schema/saml/extensions\""),
                actualAttributeValue);
    }

    @Test
    public void parsesAttributeValueUserTypeWithAttributeAndInnerNamespace() throws Exception {
        String xmlDocPayload = "<%1$sAddress myCustomType3:restriction=\"one-way\"%3$s><%1$sStreet>Zillestraße</%1$sStreet><%2$sHouseNumber%4$s>17"
                + "</%2$sHouseNumber><myCustomType4:ZipCode xmlns:myCustomType4=\"http://my.custom4.de/schema/saml/extensions\">10585"
                + "</myCustomType4:ZipCode><City xmlns=\"http://my.custom4.de/schema/saml/extensions\">Berlin</City></%1$sAddress>";
        String namespace1 = "xmlns:myCustomType1=\"http://my.custom1.de/schema/saml/extensions\"";
        String namespace2 = "xmlns:myCustomType2=\"http://my.custom2.de/schema/saml/extensions\"";
        String namespace3 = "xmlns:myCustomType3=\"http://my.custom3.de/schema/saml/extensions\"";
        String namespace4 = "xmlns:myCustomType4=\"http://my.custom4.de/schema/saml/extensions\"";
        Object actualAttributeValue = parseAttributeValue(
                namespace1 + " " + namespace2 + " " + namespace3 + " " + namespace4 + " xsi:type=\"myCustomType1:AddressType\"",
                String.format(xmlDocPayload, "myCustomType1:", "myCustomType2:", "", ""));
        Assert.assertEquals(String.format(xmlDocPayload, "myCustomType1:", "myCustomType2:", " " + namespace3 + " " + namespace1, " " + namespace2),
                actualAttributeValue);
    }

    private Object parseAttributeValue(String namespaceAndType, String payload) throws Exception {
        InputStream input = new ByteArrayInputStream(asAttribute(namespaceAndType, payload).getBytes(StandardCharsets.UTF_8));
        XMLEventReader xmlEventReader = AbstractParser.createEventReader(input);
        xmlEventReader.nextEvent();
        Object attributeValue = SAMLAttributeValueParser.getInstance().parse(xmlEventReader);

        XMLEvent nextXmlEvent = xmlEventReader.nextEvent();
        Assert.assertTrue(nextXmlEvent.isEndElement());
        final String nextName = nextXmlEvent.asEndElement().getName().getLocalPart();
        Assert.assertTrue("Attribute".equals(nextName) || "AttributeValue".equals(nextName)); // both are valid

        return attributeValue;
    }

    private String asAttribute(String namespaceAndType, String payload) {
        return "<saml2:Attribute xmlns:saml2=\"urn:oasis:names:tc:SAML:2.0:assertion\"><saml2:AttributeValue xmlns:xsi=\"http://www.w3"
                + ".org/2001/XMLSchema-instance\" " + namespaceAndType + ">" + payload + "</saml2:AttributeValue></saml2:Attribute>";
    }
}
