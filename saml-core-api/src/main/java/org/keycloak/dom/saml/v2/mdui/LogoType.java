package org.keycloak.dom.saml.v2.mdui;

import java.net.URI;

/**
 * <p>
 * Java class for localizedURIType complex type.
 *
 * <p>
 * The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 *  &lt;complexType name="LogoType">
 *   &lt;simpleContent>
 *     &lt;extension base="&lt;http://www.w3.org/2001/XMLSchema>anyURI">
 *       &lt;attribute name="height" type="positiveInteger" use="required""/>
 *       &lt;attribute name="width" type="positiveInteger" use="required""/>
 *       &lt;attribute ref="{http://www.w3.org/XML/1998/namespace}lang "/>
 *     &lt;/extension>
 *   &lt;/simpleContent>
 * &lt;/complexType>
 * </pre>
 */
public class LogoType {

    protected URI value;
    protected int height;
    protected int width;
    protected String lang;

    public LogoType(int height, int width) {
        this.height = height;
        this.width = width;
    }

    /**
     * Gets the value of the value property.
     *
     * @return possible object is {@link String }
     */
    public URI getValue() {
        return value;
    }

    /**
     * Sets the value of the value property.
     *
     * @param value allowed object is {@link String }
     */
    public void setValue(URI value) {
        this.value = value;
    }

    public String getLang() {
        return lang;
    }

    public void setLang(String lang) {
        this.lang = lang;
    }

    public int getHeight() {
        return height;
    }

    public int getWidth() {
        return width;
    }

}