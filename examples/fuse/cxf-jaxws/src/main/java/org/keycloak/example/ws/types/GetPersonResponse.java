
package org.keycloak.example.ws.types;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for anonymous complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType>
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="personId" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="ssn" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="name" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 *
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "personId",
    "ssn",
    "name"
})
@XmlRootElement(name = "GetPersonResponse")
public class GetPersonResponse {

    @XmlElement(required = true)
    protected String personId;
    @XmlElement(required = true)
    protected String ssn;
    @XmlElement(required = true)
    protected String name;

    /**
     * Gets the value of the personId property.
     *
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    public String getPersonId() {
        return personId;
    }

    /**
     * Sets the value of the personId property.
     *
     * @param value
     *     allowed object is
     *     {@link String }
     *
     */
    public void setPersonId(String value) {
        this.personId = value;
    }

    /**
     * Gets the value of the ssn property.
     *
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    public String getSsn() {
        return ssn;
    }

    /**
     * Sets the value of the ssn property.
     *
     * @param value
     *     allowed object is
     *     {@link String }
     *
     */
    public void setSsn(String value) {
        this.ssn = value;
    }

    /**
     * Gets the value of the name property.
     *
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the value of the name property.
     *
     * @param value
     *     allowed object is
     *     {@link String }
     *
     */
    public void setName(String value) {
        this.name = value;
    }

}
