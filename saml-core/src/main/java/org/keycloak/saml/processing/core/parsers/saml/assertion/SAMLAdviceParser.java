package org.keycloak.saml.processing.core.parsers.saml.assertion;

import org.keycloak.dom.saml.v2.assertion.XacmlResourceType;
import org.keycloak.dom.saml.v2.assertion.AttributeType;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import java.util.HashSet;
import java.util.Set;

public class SAMLAdviceParser {

    public Set<XacmlResourceType> processSubElement(XMLEventReader xmlEventReader) {
        try {
            return traverseAssertion(xmlEventReader);
        } catch (XMLStreamException e) {
            throw new RuntimeException(e);
        }
    }

    private Set<XacmlResourceType> traverseAssertion(XMLEventReader xmlEventReader) throws XMLStreamException {
        boolean insideResource = false; // Flag to track if we're inside <xacml-context:Resource>
        Set<AttributeType> attributeTypes = new HashSet<>();
        Set<XacmlResourceType> resourceTypes = new HashSet<>();
        int adviceDepth = 0;

        while (xmlEventReader.hasNext()) {
            XMLEvent event = xmlEventReader.nextEvent();

            // Handle start elements
            if (event.isStartElement()) {
                StartElement startElement = event.asStartElement();
                QName elementName = startElement.getName();

                // Track nested <Advice> tags
                if ("Advice".equalsIgnoreCase(elementName.getLocalPart())) {
                    adviceDepth++;
                }

                // Entering <xacml-context:Resource>
                if ("Resource".equals(elementName.getLocalPart())) {
                    insideResource = true;
                }

                // Process <xacml-context:Attribute>
                if (insideResource && "Attribute".equals(elementName.getLocalPart())) {
                    String attributeId = startElement.getAttributeByName(new QName("AttributeId")).getValue();
                    if (attributeId != null) {
                        // Advance to <xacml-context:AttributeValue>
                        event = xmlEventReader.nextTag();
                        if (event.isStartElement() && "AttributeValue".equals(event.asStartElement().getName().getLocalPart())) {
                            String attributeValue = xmlEventReader.getElementText();
                            AttributeType attributeType = new AttributeType(attributeId);
                            attributeType.addAttributeValue(attributeValue);
                            attributeTypes.add(attributeType);
                        }
                    }
                }
            }

            // Handle end elements
            if (event.isEndElement()) {
                EndElement endElement = event.asEndElement();
                QName elementName = endElement.getName();

                // Exiting <xacml-context:Resource>
                if ("Resource".equals(elementName.getLocalPart())) {
                    insideResource = false;

                    // Create a new XacmlResourceType from collected attributes
                    if (!attributeTypes.isEmpty()) {
                        XacmlResourceType xacmlResourceType = new XacmlResourceType();
                        xacmlResourceType.addAttributes(attributeTypes);
                        resourceTypes.add(xacmlResourceType);
                        attributeTypes.clear(); // Clear for the next <Resource>
                    }
                }

                // Exiting <Advice> and matching depth
                if ("Advice".equalsIgnoreCase(elementName.getLocalPart())) {
                    adviceDepth--;
                    if (adviceDepth == 0) {
                        // Return when exiting the outermost <Advice>
                        return resourceTypes;
                    }
                }
            }
        }

        return resourceTypes;
    }
}
