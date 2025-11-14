package org.keycloak.test.broker.saml;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.broker.saml.SAMLEndpoint;
import org.keycloak.broker.saml.mappers.XPathAttributeMapper;
import org.keycloak.dom.saml.v2.assertion.AssertionType;
import org.keycloak.dom.saml.v2.assertion.AttributeStatementType;
import org.keycloak.dom.saml.v2.assertion.AttributeType;
import org.keycloak.dom.saml.v2.assertion.NameIDType;
import org.keycloak.models.IdentityProviderMapperModel;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.saml.common.exceptions.ParsingException;
import org.keycloak.saml.processing.core.saml.v2.util.AssertionUtil;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;

public class XPathAttributeMapperTest {

    private static final String ATTRIBUTE_NAME = "attributeName";
    private static final String USER_ATTRIBUTE_NAME_FOR_TEST = "email";
    private static final String XPATH_FOR_TEST = "//*[local-name()='Street']";
    private static final String EXPECTED_RESULT = "Zillestraße";

    private static final String XML_FRAGMENT =
            "<Street>Zillestraße</Street><HouseNumber>17</HouseNumber><ZipCode>10585</ZipCode><City>Berlin</City><Country>DE</Country>";

    private static final String XML_WITH_NAMESPACE =
            "<myPrefix:Address xmlns:myPrefix=\"http://my.custom.de/schema/saml/extensions\"><myPrefix:Street>Zillestraße</myPrefix:Street>"
                    + "<myPrefix:HouseNumber>17</myPrefix:HouseNumber><myPrefix:ZipCode>10585</myPrefix:ZipCode>"
                    + "<myPrefix:City>Berlin</myPrefix:City><myPrefix:Country>DE</myPrefix:Country></myPrefix:Address>";

    @Test
    public void testInvalidXpath() {
        assertNull(testMapping(XML_FRAGMENT, "//"));
    }

    @Test
    public void testInvalidXml() {
        RuntimeException actualException =
                assertThrows(RuntimeException.class, () -> testMapping("<Open>Foo</Close>", "//*"));
        assertThat(actualException.getCause(), instanceOf(ParsingException.class));

        // it seems additional validation is added as 'TransformerException: Prefix must resolve to a namespace: unknownPrefix'
        // is thrown before the XPath function resolver
        assertNull(testMapping(XML_WITH_NAMESPACE, "//*[local-name()=$street]"));
        assertNull(testMapping(XML_WITH_NAMESPACE, "//*[local-name()=myPrefix:add(1,2)]"));
    }

    @Test
    public void testNotFound() {
        assertNull(testMapping(XML_FRAGMENT, "//*[local-name()='Unknown']"));
        assertNull(testMapping(XML_FRAGMENT, "//unknownPrefix:Street"));
    }

    @Test
    public void testSuccess_Value() {
        assertThat(testMapping(EXPECTED_RESULT, "//*"), is(EXPECTED_RESULT));
        assertThat(testMapping(EXPECTED_RESULT, "/root"), is(EXPECTED_RESULT));
    }

    @Test
    public void testSuccess_XmlFragment() {
        assertThat(testMapping(XML_FRAGMENT, XPATH_FOR_TEST), is(EXPECTED_RESULT));
    }

    @Test
    public void testSuccess_XmlWithNamespace() {
        assertThat(testMapping(XML_WITH_NAMESPACE, XPATH_FOR_TEST), is(EXPECTED_RESULT));
        assertThat(testMapping(XML_WITH_NAMESPACE, "//myPrefix:Street"), is(EXPECTED_RESULT));
    }

    @Test
    public void testSuccess_FindAllElements() {
        assertThat(testMapping(XML_FRAGMENT, "/"), allOf(containsString(EXPECTED_RESULT), containsString("Berlin")));
        assertThat(testMapping(XML_FRAGMENT, "//*"), allOf(containsString(EXPECTED_RESULT), containsString("Berlin")));
    }

    @Test
    public void testUserAttributeNames() {
        assertThat(testMapping(XML_FRAGMENT, XPATH_FOR_TEST, "firstName"), is(EXPECTED_RESULT));
        assertThat(testMapping(XML_FRAGMENT, XPATH_FOR_TEST, "lastName"), is(EXPECTED_RESULT));
        assertThat(testMapping(XML_FRAGMENT, XPATH_FOR_TEST, "userAttribute"), is(EXPECTED_RESULT));
    }

    @Test
    public void testAttributeNames() {
        assertNull(testMapping(XML_FRAGMENT, XPATH_FOR_TEST, USER_ATTRIBUTE_NAME_FOR_TEST, ATTRIBUTE_NAME + "x"));
        assertThat(testMapping(XML_FRAGMENT, XPATH_FOR_TEST, USER_ATTRIBUTE_NAME_FOR_TEST, null), is(EXPECTED_RESULT));
    }

    private String testMapping(String attributeValue, String xpath) {
        return testMapping(attributeValue, xpath, USER_ATTRIBUTE_NAME_FOR_TEST);
    }

    private String testMapping(String attributeValue, String xpath, String attribute) {
        return testMapping(attributeValue, xpath, attribute, ATTRIBUTE_NAME);
    }

    private String testMapping(String attributeValue, String xpath, String attribute, String attributeNameToSearch) {
        IdentityProviderMapperModel mapperModel = new IdentityProviderMapperModel();
        Map<String, String> config = new HashMap<>();
        mapperModel.setConfig(config);
        config.put(XPathAttributeMapper.ATTRIBUTE_NAME, attributeNameToSearch);
        config.put(XPathAttributeMapper.USER_ATTRIBUTE, attribute);
        config.put(XPathAttributeMapper.ATTRIBUTE_XPATH, xpath);
        BrokeredIdentityContext context = new BrokeredIdentityContext("brokeredIdentityContext", new IdentityProviderModel());
        AssertionType assertion = AssertionUtil.createAssertion("assertionId", NameIDType.deserializeFromString("nameIDType"));
        AttributeStatementType statement = new AttributeStatementType();
        assertion.addStatement(statement);
        AttributeType attributeType = new AttributeType(ATTRIBUTE_NAME);
        attributeType.addAttributeValue(attributeValue);
        statement.addAttribute(new AttributeStatementType.ASTChoiceType(attributeType));
        AttributeType otherAttributeType = new AttributeType("Some other String");
        otherAttributeType.addAttributeValue("Foobar");
        statement.addAttribute(new AttributeStatementType.ASTChoiceType(otherAttributeType));
        AttributeType booleanAttributeType = new AttributeType("Some boolean");
        booleanAttributeType.addAttributeValue(true);
        statement.addAttribute(new AttributeStatementType.ASTChoiceType(booleanAttributeType));
        AttributeType longAttributeType = new AttributeType("Some long");
        longAttributeType.addAttributeValue(123L);
        statement.addAttribute(new AttributeStatementType.ASTChoiceType(longAttributeType));
        context.getContextData().put(SAMLEndpoint.SAML_ASSERTION, assertion);
        new XPathAttributeMapper().preprocessFederatedIdentity(null, null, mapperModel, context);

        Object userAttributes = context.getContextData().get("user.attributes." + attribute);
        return userAttributes == null ? null : ((List<?>) userAttributes).get(0).toString();
    }
}
