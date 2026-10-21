package org.keycloak.dom.saml.v2.mdui;

import java.util.List;

/**
 * <p>
 * Java class for localizedURIType complex type.
 *
 * <p>
 * The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 *  &lt;complexType name="KeywordsType">
 *   &lt;simpleContent>
 *     &lt;extension base="mdui:listOfStrings">
 *       &lt;attribute ref="{http://www.w3.org/XML/1998/namespace}lang  use="required""/>
 *     &lt;/extension>
 *   &lt;/simpleContent>
 * &lt;/complexType>
 * &lt;simpleType name="listOfStrings">
 *   &lt;list itemType="string"/>
 * &lt;/simpleType>
 * </pre>
 */
public class KeywordsType {

    protected List<String> values;
    protected String lang;

    public KeywordsType(String lang) {
        this.lang = lang;
    }

    public List<String> getValues() {
        return values;
    }

    public void setValues(List<String> values) {
        this.values = values;
    }

    public String getLang() {
        return lang;
    }



}