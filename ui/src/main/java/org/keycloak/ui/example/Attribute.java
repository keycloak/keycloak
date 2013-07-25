package org.keycloak.ui.example;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class Attribute implements Serializable {

    private static final long serialVersionUID = 1L;

    private String name;

    private String value;

    public Attribute() {
    }

    public Attribute(String name, String value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setValue(String value) {
        this.value = value;
    }

}
