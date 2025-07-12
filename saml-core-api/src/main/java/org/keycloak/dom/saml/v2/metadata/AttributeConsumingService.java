package org.keycloak.dom.saml.v2.metadata;

public class AttributeConsumingService {

    private  String serviceName;
    private  String friendlyName;
    private  String attributeName;
    private  String attributeValue;

    // Constructor
    public AttributeConsumingService() {
        // Default constructor
    }
    public AttributeConsumingService(int index, String serviceName, String friendlyName, String attributeName, String attributeValue) {
        this.serviceName = serviceName;
        this.friendlyName = friendlyName;
        this.attributeName = attributeName;
        this.attributeValue = attributeValue;

    }

    // Getters and Setters
    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getFriendlyName() {
        return friendlyName;
    }

    public void setFriendlyName(String friendlyName) {
        this.friendlyName = friendlyName;
    }

    public String getAttributeName() {
        return attributeName;
    }

    public void setAttributeName(String attributeName) {
        this.attributeName = attributeName;
    }

    public String getAttributeValue() {
        return attributeValue;
    }

    public void setAttributeValue(String attributeValue) {
        this.attributeValue = attributeValue;
    }


}
