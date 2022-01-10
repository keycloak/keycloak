package org.keycloak.dom.saml.v2.mdattr;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.keycloak.dom.saml.v2.assertion.AssertionType;
import org.keycloak.dom.saml.v2.assertion.AttributeType;

/**
 *
 * *
 * <p>
 * Java class for EntityAttributes complex type.
 *
 * <p>
 * The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
* 	&lt;element name="EntityAttributes" type="mdattr:EntityAttributesType"/>
* 	&lt;complexType name="EntityAttributesType">
* 		&lt;choice maxOccurs="unbounded">
* 			&lt;element ref="saml:Attribute"/>
* 			&lt;element ref="saml:Assertion"/>
* 		&lt;/sequence>
* 	&lt;/complexType>
 *
 * </pre>
 *
 */

public class EntityAttributes implements Serializable {

    protected List<AttributeType> attribute = new ArrayList<>();
    protected List<AssertionType> assertion = new ArrayList<>();

    public List<AttributeType> getAttribute() {
        return attribute;
    }

    public void addAttribute(AttributeType attributeType) {
        attribute.add(attributeType);
    }

    public void removeAttribute(AttributeType attributeType) {
        attribute.remove(attributeType);
    }

    public List<AssertionType> getAssertion() {
        return assertion;
    }

    public void addAssertion(AssertionType attributeType) {
        assertion.add(attributeType);
    }

    public void removeAssertion(AttributeType attributeType) {
        assertion.remove(attributeType);
    }

}
