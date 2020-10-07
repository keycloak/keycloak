/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.keycloak.subsystem.as7;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.parsing.ParseUtils;
import org.jboss.as.controller.persistence.SubsystemMarshallingContext;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLElementWriter;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The subsystem parser, which uses stax to read and write to and from xml
 */
class KeycloakSubsystemParser implements XMLStreamConstants, XMLElementReader<List<ModelNode>>, XMLElementWriter<SubsystemMarshallingContext> {

    /**
     * {@inheritDoc}
     */
    @Override
    public void readElement(final XMLExtendedStreamReader reader, final List<ModelNode> list) throws XMLStreamException {
        // Require no attributes
        ParseUtils.requireNoAttributes(reader);
        ModelNode addKeycloakSub = Util.createAddOperation(PathAddress.pathAddress(KeycloakExtension.PATH_SUBSYSTEM));
        list.add(addKeycloakSub);

        while (reader.hasNext() && nextTag(reader) != END_ELEMENT) {
            if (reader.getLocalName().equals(RealmDefinition.TAG_NAME)) {
                readRealm(reader, list);
            }
            else if (reader.getLocalName().equals(SecureDeploymentDefinition.TAG_NAME)) {
                readDeployment(reader, list);
            }
        }
    }

    // used for debugging
    private int nextTag(XMLExtendedStreamReader reader) throws XMLStreamException {
        return reader.nextTag();
    }

    private void readRealm(XMLExtendedStreamReader reader, List<ModelNode> list) throws XMLStreamException {
        String realmName = readNameAttribute(reader);
        ModelNode addRealm = new ModelNode();
        addRealm.get(ModelDescriptionConstants.OP).set(ModelDescriptionConstants.ADD);
        PathAddress addr = PathAddress.pathAddress(PathElement.pathElement(ModelDescriptionConstants.SUBSYSTEM, KeycloakExtension.SUBSYSTEM_NAME),
                                                   PathElement.pathElement(RealmDefinition.TAG_NAME, realmName));
        addRealm.get(ModelDescriptionConstants.OP_ADDR).set(addr.toModelNode());

        while (reader.hasNext() && nextTag(reader) != END_ELEMENT) {
            String tagName = reader.getLocalName();
            SimpleAttributeDefinition def = RealmDefinition.lookup(tagName);
            if (def == null) throw new XMLStreamException("Unknown realm tag " + tagName);
            def.parseAndSetParameter(reader.getElementText(), addRealm, reader);
        }

        if (!SharedAttributeDefinitons.validateTruststoreSetIfRequired(addRealm)) {
            //TODO: externalize the message
            throw new XMLStreamException("truststore and truststore-password must be set if ssl-required is not none and disable-trust-manager is false.");
        }

        list.add(addRealm);
    }

    private void readDeployment(XMLExtendedStreamReader reader, List<ModelNode> resourcesToAdd) throws XMLStreamException {
        String name = readNameAttribute(reader);
        ModelNode addSecureDeployment = new ModelNode();
        addSecureDeployment.get(ModelDescriptionConstants.OP).set(ModelDescriptionConstants.ADD);
        PathAddress addr = PathAddress.pathAddress(PathElement.pathElement(ModelDescriptionConstants.SUBSYSTEM, KeycloakExtension.SUBSYSTEM_NAME),
                PathElement.pathElement(SecureDeploymentDefinition.TAG_NAME, name));
        addSecureDeployment.get(ModelDescriptionConstants.OP_ADDR).set(addr.toModelNode());
        List<ModelNode> credentialsToAdd = new ArrayList<>();
        while (reader.hasNext() && nextTag(reader) != END_ELEMENT) {
            String tagName = reader.getLocalName();
            if (tagName.equals(CredentialDefinition.TAG_NAME)) {
                readCredential(reader, addr, credentialsToAdd);
                continue;
            }

            SimpleAttributeDefinition def = SecureDeploymentDefinition.lookup(tagName);
            if (def == null) throw new XMLStreamException("Unknown secure-deployment tag " + tagName);
            def.parseAndSetParameter(reader.getElementText(), addSecureDeployment, reader);
        }


        /**
         * TODO need to check realm-ref first.
        if (!SharedAttributeDefinitons.validateTruststoreSetIfRequired(addSecureDeployment)) {
            //TODO: externalize the message
            throw new XMLStreamException("truststore and truststore-password must be set if ssl-required is not none and disable-trust-manager is false.");
        }
         */

        // Must add credentials after the deployment is added.
        resourcesToAdd.add(addSecureDeployment);
        resourcesToAdd.addAll(credentialsToAdd);
    }

    public void readCredential(XMLExtendedStreamReader reader, PathAddress parent, List<ModelNode> credentialsToAdd) throws XMLStreamException {
        String name = readNameAttribute(reader);

        Map<String, String> values = new HashMap<>();
        String textValue = null;
        while (reader.hasNext()) {
            int next = reader.next();
            if (next == CHARACTERS) {
                // text value of credential element (like for "secret" )
                String text = reader.getText();
                if (text == null || text.trim().isEmpty()) {
                    continue;
                }
                textValue = text;
            } else if (next == START_ELEMENT) {
                String key = reader.getLocalName();
                reader.next();
                String value = reader.getText();
                reader.next();

                values.put(key, value);
            } else if (next == END_ELEMENT) {
                break;
            }
        }

        if (textValue != null) {
            ModelNode addCredential = getCredentialToAdd(parent, name, textValue);
            credentialsToAdd.add(addCredential);
        } else {
            for (Map.Entry<String, String> entry : values.entrySet()) {
                ModelNode addCredential = getCredentialToAdd(parent, name + "." + entry.getKey(), entry.getValue());
                credentialsToAdd.add(addCredential);
            }
        }
    }

    private ModelNode getCredentialToAdd(PathAddress parent, String name, String value) {
        ModelNode addCredential = new ModelNode();
        addCredential.get(ModelDescriptionConstants.OP).set(ModelDescriptionConstants.ADD);
        PathAddress addr = PathAddress.pathAddress(parent, PathElement.pathElement(CredentialDefinition.TAG_NAME, name));
        addCredential.get(ModelDescriptionConstants.OP_ADDR).set(addr.toModelNode());
        addCredential.get(CredentialDefinition.VALUE.getName()).set(value);
        return addCredential;
    }

    // expects that the current tag will have one single attribute called "name"
    private String readNameAttribute(XMLExtendedStreamReader reader) throws XMLStreamException {
        String name = null;
        for (int i = 0; i < reader.getAttributeCount(); i++) {
            String attr = reader.getAttributeLocalName(i);
            if (attr.equals("name")) {
                name = reader.getAttributeValue(i);
                continue;
            }
            throw ParseUtils.unexpectedAttribute(reader, i);
        }
        if (name == null) {
            throw ParseUtils.missingRequired(reader, Collections.singleton("name"));
        }
        return name;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeContent(final XMLExtendedStreamWriter writer, final SubsystemMarshallingContext context) throws XMLStreamException {
        context.startSubsystemElement(KeycloakExtension.NAMESPACE, false);
        writeRealms(writer, context);
        writeSecureDeployments(writer, context);
        writer.writeEndElement();
    }

    private void writeRealms(XMLExtendedStreamWriter writer, SubsystemMarshallingContext context) throws XMLStreamException {
        if (!context.getModelNode().get(RealmDefinition.TAG_NAME).isDefined()) {
            return;
        }
        for (Property realm : context.getModelNode().get(RealmDefinition.TAG_NAME).asPropertyList()) {
            writer.writeStartElement(RealmDefinition.TAG_NAME);
            writer.writeAttribute("name", realm.getName());
            ModelNode realmElements = realm.getValue();
            for (AttributeDefinition element : RealmDefinition.ALL_ATTRIBUTES) {
                element.marshallAsElement(realmElements, writer);
            }

            writer.writeEndElement();
        }
    }

    private void writeSecureDeployments(XMLExtendedStreamWriter writer, SubsystemMarshallingContext context) throws XMLStreamException {
        if (!context.getModelNode().get(SecureDeploymentDefinition.TAG_NAME).isDefined()) {
            return;
        }
        for (Property deployment : context.getModelNode().get(SecureDeploymentDefinition.TAG_NAME).asPropertyList()) {
            writer.writeStartElement(SecureDeploymentDefinition.TAG_NAME);
            writer.writeAttribute("name", deployment.getName());
            ModelNode deploymentElements = deployment.getValue();
            for (AttributeDefinition element : SecureDeploymentDefinition.ALL_ATTRIBUTES) {
                element.marshallAsElement(deploymentElements, writer);
            }

            ModelNode credentials = deploymentElements.get(CredentialDefinition.TAG_NAME);
            if (credentials.isDefined()) {
                writeCredentials(writer, credentials);
            }

            writer.writeEndElement();
        }
    }

    private void writeCredentials(XMLExtendedStreamWriter writer, ModelNode credentials) throws XMLStreamException {
        Map<String, Object> parsed = new LinkedHashMap<>();
        for (Property credential : credentials.asPropertyList()) {
            String credName = credential.getName();
            String credValue = credential.getValue().get(CredentialDefinition.VALUE.getName()).asString();

            if (credName.indexOf('.') > -1) {
                String[] parts = credName.split("\\.");
                String provider = parts[0];
                String propKey = parts[1];

                Map<String, String> currentProviderMap = (Map<String, String>) parsed.get(provider);
                if (currentProviderMap == null) {
                    currentProviderMap = new LinkedHashMap<>();
                    parsed.put(provider, currentProviderMap);
                }
                currentProviderMap.put(propKey, credValue);
            } else {
                parsed.put(credName, credValue);
            }
        }

        for (Map.Entry<String, Object> entry : parsed.entrySet()) {
            writer.writeStartElement(CredentialDefinition.TAG_NAME);
            writer.writeAttribute("name", entry.getKey());

            Object value = entry.getValue();
            if (value instanceof String) {
                writeCharacters(writer, (String) value);
            } else {
                Map<String, String> credentialProps = (Map<String, String>) value;
                for (Map.Entry<String, String> prop : credentialProps.entrySet()) {
                    writer.writeStartElement(prop.getKey());
                    writeCharacters(writer, prop.getValue());
                    writer.writeEndElement();
                }
            }

            writer.writeEndElement();
        }
    }

    // code taken from org.jboss.as.controller.AttributeMarshaller
    private void writeCharacters(XMLExtendedStreamWriter writer, String content) throws XMLStreamException {
        if (content.indexOf('\n') > -1) {
            // Multiline content. Use the overloaded variant that staxmapper will format
            writer.writeCharacters(content);
        } else {
            // Staxmapper will just output the chars without adding newlines if this is used
            char[] chars = content.toCharArray();
            writer.writeCharacters(chars, 0, chars.length);
        }
    }

}
