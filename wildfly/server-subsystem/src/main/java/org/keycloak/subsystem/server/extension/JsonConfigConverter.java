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
package org.keycloak.subsystem.server.extension;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.dmr.ModelNode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION;
import static org.keycloak.subsystem.server.extension.KeycloakSubsystemDefinition.MASTER_REALM_NAME;
import static org.keycloak.subsystem.server.extension.KeycloakSubsystemDefinition.PROVIDERS;
import static org.keycloak.subsystem.server.extension.KeycloakSubsystemDefinition.SCHEDULED_TASK_INTERVAL;
import static org.keycloak.subsystem.server.extension.ThemeResourceDefinition.CACHE_TEMPLATES;
import static org.keycloak.subsystem.server.extension.ThemeResourceDefinition.CACHE_THEMES;
import static org.keycloak.subsystem.server.extension.ThemeResourceDefinition.DEFAULT;
import static org.keycloak.subsystem.server.extension.ThemeResourceDefinition.DIR;
import static org.keycloak.subsystem.server.extension.ThemeResourceDefinition.MODULES;
import static org.keycloak.subsystem.server.extension.ThemeResourceDefinition.STATIC_MAX_AGE;
import static org.keycloak.subsystem.server.extension.ThemeResourceDefinition.WELCOME_THEME;

/**
 * Converts json representation of Keycloak config to DMR operations.
 *
 * @author Stan Silvert ssilvert@redhat.com (C) 2016 Red Hat Inc.
 */
public class JsonConfigConverter {

    private static final List<String> NON_SPI_LIST = new ArrayList<>();
    
    static {
        NON_SPI_LIST.add("providers");
        NON_SPI_LIST.add("admin");
        NON_SPI_LIST.add("theme");
        NON_SPI_LIST.add("scheduled");
    }

    /**
     * Convert keycloak-server.json to DMR operations that write to standalone.xml
     * or domain.xml.
     * 
     * @param json The json representation of the config.
     * @param subsysAddress The management model address of the keycloak-server subsystem.
     * @return A list of DMR operations.
     * @throws IOException If the json can not be parsed.
     */
    public static List<ModelNode> convertJsonConfig(String json, PathAddress subsysAddress) throws IOException {
        List<ModelNode> list = new ArrayList<>();

        JsonNode root = new ObjectMapper().readTree(json);

        list.add(masterRealmName(root, subsysAddress));
        list.add(scheduledTaskInterval(root, subsysAddress));
        list.add(providers(root, subsysAddress));
        list.add(theme(root, subsysAddress.append(ThemeResourceDefinition.TAG_NAME, 
                                                ThemeResourceDefinition.RESOURCE_NAME)));
        list.addAll(spis(root, subsysAddress));

        return list;
    }

    private static ModelNode masterRealmName(JsonNode root, PathAddress addr) {
        JsonNode targetNode = getNode(root, "admin", "realm");
        String value = MASTER_REALM_NAME.getDefaultValue().asString();
        if (targetNode != null) value = targetNode.asText(value);
        
        ModelNode op = Util.createOperation(WRITE_ATTRIBUTE_OPERATION, addr);
        op.get("name").set(MASTER_REALM_NAME.getName());
        op.get("value").set(value);
        return op;
    }
    
    private static ModelNode scheduledTaskInterval(JsonNode root, PathAddress addr) {
        JsonNode targetNode = getNode(root, "scheduled", "interval");
        Long value = SCHEDULED_TASK_INTERVAL.getDefaultValue().asLong();
        if (targetNode != null) value = targetNode.asLong(value);
        
        ModelNode op = Util.createOperation(WRITE_ATTRIBUTE_OPERATION, addr);
        op.get("name").set(SCHEDULED_TASK_INTERVAL.getName());
        op.get("value").set(value);
        return op;
    }
    
    private static ModelNode providers(JsonNode root, PathAddress addr) {
        JsonNode targetNode = getNode(root, "providers");
        ModelNode value = PROVIDERS.getDefaultValue();
        if (targetNode != null && targetNode.isArray()) {
            value = new ModelNode();
            for (JsonNode node : targetNode) {
                value.add(node.asText());
            }
        }
        
        ModelNode op = Util.createOperation(WRITE_ATTRIBUTE_OPERATION, addr);
        op.get("name").set(PROVIDERS.getName());
        op.get("value").set(value);
        return op;
    }
    
    private static ModelNode theme(JsonNode root, PathAddress addr) {
        JsonNode themeNode = getNode(root, "theme");
        ModelNode op = Util.createAddOperation(addr);
        
        JsonNode targetNode = getNode(themeNode, "staticMaxAge");
        Long lValue = STATIC_MAX_AGE.getDefaultValue().asLong();
        if (targetNode != null) lValue = targetNode.asLong(lValue);
        op.get(STATIC_MAX_AGE.getName()).set(lValue);

        targetNode = getNode(themeNode, "cacheTemplates");
        Boolean bValue = CACHE_TEMPLATES.getDefaultValue().asBoolean();
        if (targetNode != null) bValue = targetNode.asBoolean(bValue);
        op.get(CACHE_TEMPLATES.getName()).set(bValue);
        
        targetNode = getNode(themeNode, "cacheThemes");
        bValue = CACHE_THEMES.getDefaultValue().asBoolean();
        if (targetNode != null) bValue = targetNode.asBoolean(bValue);
        op.get(CACHE_THEMES.getName()).set(bValue);
        
        targetNode = getNode(themeNode, "folder", "dir");
        String sValue = DIR.getDefaultValue().asString();
        if (targetNode != null) sValue = targetNode.asText(sValue);
        op.get(DIR.getName()).set(sValue);
        
        targetNode = getNode(themeNode, "welcomeTheme");
        if (targetNode != null) op.get(WELCOME_THEME.getName()).set(targetNode.asText());
        
        targetNode = getNode(themeNode, "default");
        if (targetNode != null) op.get(DEFAULT.getName()).set(targetNode.asText());
        
        targetNode = getNode(themeNode, "module", "modules");
        if (targetNode != null && targetNode.isArray()) {
            op.get(MODULES.getName()).set(themeModules(targetNode));
        }
        
        return op;
    }
    
    private static ModelNode themeModules(JsonNode modulesNode) {
        ModelNode modules = new ModelNode();
        for (JsonNode node : modulesNode) {
            modules.add(node.asText());
        }
        return modules;
    }
    
    private static Collection<ModelNode> spis(JsonNode root, PathAddress addr) {
        List<ModelNode> spis = new ArrayList<>();
        
        Iterator<String> spiIterator = root.fieldNames();
        while (spiIterator.hasNext()) {
            String spiName = spiIterator.next();
            if (NON_SPI_LIST.contains(spiName)) continue;
            
            PathAddress spiAddr = addr.append("spi", spiName);
            spis.addAll(spi(root, spiAddr, spiName));
        }
        
        return spis;
    }
    
    private static List<ModelNode> spi(JsonNode root, PathAddress spiAddr, String spiName) {
        List<ModelNode> spiAndProviders = new ArrayList<>();
        ModelNode op = Util.createAddOperation(spiAddr);
        spiAndProviders.add(op);
        
        Iterator<String> providerIterator = root.get(spiName).fieldNames();
        while (providerIterator.hasNext()) {
            String providerName = providerIterator.next();
            if ("provider".equals(providerName)) {
                op.get(SpiResourceDefinition.DEFAULT_PROVIDER.getName()).set(getNode(root, spiName, "provider").asText());
            } else {
                PathAddress providerAddr = spiAddr.append("provider", providerName);
                spiAndProviders.add(spiProvider(getNode(root, spiName, providerName), providerAddr));
            }
        }
        
        return spiAndProviders;
    }
    
    private static ModelNode spiProvider(JsonNode providerNode, PathAddress providerAddr) {
        ModelNode op = Util.createAddOperation(providerAddr);
        
        ModelNode properties = new ModelNode();
        
        Iterator<String> propNames = providerNode.fieldNames();
        while (propNames.hasNext()) {
            String propName = propNames.next();
            
            if ("enabled".equals(propName)) {
                op.get(ProviderResourceDefinition.ENABLED.getName()).set(providerNode.get(propName).asBoolean());
            } else {
                if (providerNode.get(propName).isArray()) {
                    properties.get(propName).set(makeArrayText(providerNode.get(propName)));
                } else {
                    properties.get(propName).set(providerNode.get(propName).asText());
                }
            }
        }
        
        if (properties.isDefined() && !properties.asPropertyList().isEmpty()) {
            op.get("properties").set(properties);
        }
        
        if (!op.hasDefined(ProviderResourceDefinition.ENABLED.getName())) {
            op.get(ProviderResourceDefinition.ENABLED.getName()).set(ProviderResourceDefinition.ENABLED.getDefaultValue());
        }
        
        return op;
    }
    
    private static String makeArrayText(JsonNode arrayNode) {
        StringBuilder builder = new StringBuilder("[");
        
        Iterator<JsonNode> nodes = arrayNode.iterator();
        while (nodes.hasNext()) {
            JsonNode node = nodes.next();
            builder.append("\"");
            builder.append(node.asText());
            builder.append("\"");
            if (nodes.hasNext()) builder.append(",");
        }
        builder.append("]");
        
        return builder.toString();
    }
    
    private static JsonNode getNode(JsonNode root, String... path) {
        if (root == null) {
            return null;
        }
        JsonNode n = root;
        for (String p : path) {
            n = n.get(p);
            if (n == null) {
                return null;
            }
        }
        return n;
    }

}
