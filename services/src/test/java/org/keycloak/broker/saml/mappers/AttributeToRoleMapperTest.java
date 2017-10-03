package org.keycloak.broker.saml.mappers;

import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.broker.saml.SAMLEndpoint;
import org.keycloak.dom.saml.v2.assertion.AssertionType;
import org.keycloak.dom.saml.v2.assertion.AttributeStatementType;
import org.keycloak.dom.saml.v2.assertion.AttributeType;
import org.keycloak.dom.saml.v2.assertion.NameIDType;
import org.keycloak.models.IdentityProviderMapperModel;
import org.keycloak.saml.processing.core.saml.v2.util.AssertionUtil;

public class AttributeToRoleMapperTest {

    @Test
    public void testMapByFriendlyNameAndValue() {
        IdentityProviderMapperModel model = new IdentityProviderMapperModel();
        Map<String, String> config = new HashMap<>();
        config.put(AttributeToRoleMapper.ATTRIBUTE_FRIENDLY_NAME, "b");
        config.put(AttributeToRoleMapper.ATTRIBUTE_VALUE, "c");
        model.setConfig(config);
        
        // create SAML assertion with a single attribute
        AttributeType attributeType = new AttributeType("a");
        attributeType.setFriendlyName("b");
        attributeType.addAttributeValue("c");

        AssertionType assertionType = AssertionUtil.createAssertion("myId", new NameIDType());
        AttributeStatementType attributeStatementType = new AttributeStatementType();
        attributeStatementType.addAttribute(new AttributeStatementType.ASTChoiceType(attributeType));
        assertionType.addStatement(attributeStatementType);
        
        BrokeredIdentityContext context = new BrokeredIdentityContext("myContext");
        context.getContextData().put(SAMLEndpoint.SAML_ASSERTION, assertionType);
        
        AttributeToRoleMapper mapper = new AttributeToRoleMapper();
        Assert.assertTrue(mapper.isAttributePresent(model, context));
    }
}
