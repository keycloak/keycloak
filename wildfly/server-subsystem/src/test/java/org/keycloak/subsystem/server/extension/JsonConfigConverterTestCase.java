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

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.dmr.ModelNode;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADDRESS;
import static org.junit.Assert.assertEquals;

/**
 *
 * @author Stan Silvert ssilvert@redhat.com (C) 2016 Red Hat Inc.
 */
public class JsonConfigConverterTestCase {

    private final PathElement domainRoot = PathElement.pathElement("profile", "auth-server-clustered");
    private final PathAddress domainAddress = PathAddress.pathAddress(domainRoot)
                                                         .append(KeycloakExtension.PATH_SUBSYSTEM);
    private final PathAddress standaloneAddress = PathAddress.pathAddress(KeycloakExtension.PATH_SUBSYSTEM);

    @Test
    public void testConvertJsonStandaloneWithModules() throws Exception {
        String json = basicJsonConfig(true);
        List<ModelNode> expResult = expectedOperations(true, false);
        
        List<ModelNode> result = JsonConfigConverter.convertJsonConfig(json, standaloneAddress);
        assertEquals(expResult, result);
    }
    
    @Test
    public void testConvertJsonStandaloneWithoutModules() throws Exception {
        String json = basicJsonConfig(false);
        List<ModelNode> expResult = expectedOperations(false, false);
        
        List<ModelNode> result = JsonConfigConverter.convertJsonConfig(json, standaloneAddress);
        assertEquals(expResult, result);
    }
    
    @Test
    public void testConvertJsonDomainWithModules() throws Exception {
        String json = basicJsonConfig(true);
        List<ModelNode> expResult = expectedOperations(true, true);
        
        List<ModelNode> result = JsonConfigConverter.convertJsonConfig(json, domainAddress);
        assertEquals(expResult, result);
    }
    
    @Test
    public void testConvertJsonDomainWithoutModules() throws Exception {
        String json = basicJsonConfig(false);
        List<ModelNode> expResult = expectedOperations(false, true);
        
        List<ModelNode> result = JsonConfigConverter.convertJsonConfig(json, domainAddress);
        assertEquals(expResult, result);
    }
    
    private String basicJsonConfig(boolean includeModules) {
        String basicConfig = 
              "{\n"
            + "    \"providers\": [\n"
            + "        \"classpath:${jboss.home.dir}/providers/*\"\n"
            + "    ],\n"
            + "\n"
            + "    \"admin\": {\n"
            + "        \"realm\": \"master\"\n"
            + "    },\n"
            + "\n"
            + "    \"eventsStore\": {\n"
            + "        \"provider\": \"jpa\",\n"
            + "        \"jpa\": {\n"
            + "            \"exclude-events\": [ \"REFRESH_TOKEN\" ]\n"
            + "        }\n"
            + "    },\n"
            + "\n"
            + "    \"realm\": {\n"
            + "        \"provider\": \"jpa\"\n"
            + "    },\n"
            + "\n"
            + "    \"user\": {\n"
            + "        \"provider\": \"jpa\"\n"
            + "    },\n"
            + "\n"
            + "    \"userCache\": {\n"
            + "        \"default\" : {\n"
            + "            \"enabled\": true\n"
            + "        }\n"
            + "    },\n"
            + "\n"
            + "    \"userSessionPersister\": {\n"
            + "        \"provider\": \"jpa\"\n"
            + "    },\n"
            + "\n"
            + "    \"authorizationPersister\": {\n"
            + "        \"provider\": \"jpa\"\n"
            + "    },\n"
            + "\n"
            + "    \"timer\": {\n"
            + "        \"provider\": \"basic\"\n"
            + "    },\n"
            + "\n"
            + "    \"theme\": {\n"
            + "        \"staticMaxAge\": 2592001,\n"
            + "        \"cacheTemplates\": false,\n"
            + "        \"cacheThemes\": false,\n"
            + "        \"welcomeTheme\": \"welcome\",\n"
            + "        \"default\": \"default\",\n"
            + "        \"folder\": {\n"
            + "          \"dir\": \"${jboss.home.dir}/themes\"\n";
            
        
        if (includeModules) {
            basicConfig +=
              "        },\n"
            + "        \"module\": {\n"
            + "          \"modules\": [ \"org.keycloak.example.themes\" ]\n"
            + "         }\n";
        } else {
            basicConfig +=
              "        }\n";
        }
        
        basicConfig +=
              "     },\n"
            + "\n"
            + "    \"scheduled\": {\n"
            + "        \"interval\": 900\n"
            + "    },\n"
            + "\n"
            + "    \"connectionsHttpClient\": {\n"
            + "        \"default\": {}\n"
            + "    },\n"
            + "\n"
            + "    \"connectionsJpa\": {\n"
            + "        \"default\": {\n"
            + "            \"dataSource\": \"java:jboss/datasources/KeycloakDS\",\n"
            + "            \"databaseSchema\": \"update\"\n"
            + "        }\n"
            + "    },\n"
            + "\n"
            + "    \"realmCache\": {\n"
            + "        \"default\" : {\n"
            + "            \"enabled\": true\n"
            + "        }\n"
            + "    },\n"
            + "\n"
            + "    \"connectionsInfinispan\": {\n"
            + "        \"provider\": \"default\",\n"
            + "        \"default\": {\n"
            + "            \"cacheContainer\" : \"java:jboss/infinispan/container/keycloak\"\n"
            + "        }\n"
            + "    }\n"
            + "}";
        
        return basicConfig;
    }
    
    private List<ModelNode> expectedOperations(boolean includeModules, boolean isDomain) {
        List<ModelNode> ops = new ArrayList<>();
        
        ops.add(ModelNode.fromString(
            "{\n" +
            "    \"operation\" => \"write-attribute\",\n" +
            "    \"address\" => [(\"subsystem\" => \"keycloak-server\")],\n" +
            "    \"name\" => \"master-realm-name\",\n" +
            "    \"value\" => \"master\"\n" +
            "}"
        ));
        
        ops.add(ModelNode.fromString(
            "{\n" +
            "    \"operation\" => \"write-attribute\",\n" +
            "    \"address\" => [(\"subsystem\" => \"keycloak-server\")],\n" +
            "    \"name\" => \"scheduled-task-interval\",\n" +
            "    \"value\" => 900L\n" +
            "}"
        ));
        
        ops.add(ModelNode.fromString(
            "{\n" +
            "    \"operation\" => \"write-attribute\",\n" +
            "    \"address\" => [(\"subsystem\" => \"keycloak-server\")],\n" +
            "    \"name\" => \"providers\",\n" +
            "    \"value\" => [\"classpath:${jboss.home.dir}/providers/*\"]\n" +
            "}"
        ));
        
        if (includeModules) {
            ops.add(ModelNode.fromString(
                "{\n" +
                "    \"operation\" => \"add\",\n" +
                "    \"address\" => [\n" +
                "        (\"subsystem\" => \"keycloak-server\"),\n" +
                "        (\"theme\" => \"defaults\")\n" +
                "    ],\n" +
                "    \"staticMaxAge\" => 2592001L,\n" +
                "    \"cacheTemplates\" => false,\n" +
                "    \"cacheThemes\" => false,\n" +
                "    \"dir\" => \"${jboss.home.dir}/themes\",\n" +
                "    \"welcomeTheme\" => \"welcome\",\n" +
                "    \"default\" => \"default\",\n" +
                "    \"modules\" => [\"org.keycloak.example.themes\"]\n" +
                "}"
            ));
        } else {
            ops.add(ModelNode.fromString(
                "{\n" +
                "    \"operation\" => \"add\",\n" +
                "    \"address\" => [\n" +
                "        (\"subsystem\" => \"keycloak-server\"),\n" +
                "        (\"theme\" => \"defaults\")\n" +
                "    ],\n" +
                "    \"staticMaxAge\" => 2592001L,\n" +
                "    \"cacheTemplates\" => false,\n" +
                "    \"cacheThemes\" => false,\n" +
                "    \"dir\" => \"${jboss.home.dir}/themes\",\n" +
                "    \"welcomeTheme\" => \"welcome\",\n" +
                "    \"default\" => \"default\",\n" +
                "}"
            ));
        }
        
        ops.add(ModelNode.fromString(
            "{\n" +
            "    \"operation\" => \"add\",\n" +
            "    \"address\" => [\n" +
            "        (\"subsystem\" => \"keycloak-server\"),\n" +
            "        (\"spi\" => \"eventsStore\")\n" +
            "    ],\n" +
            "    \"default-provider\" => \"jpa\"\n" +
            "}"
        ));
        
        ops.add(ModelNode.fromString(
            "{\n" +
            "    \"operation\" => \"add\",\n" +
            "    \"address\" => [\n" +
            "        (\"subsystem\" => \"keycloak-server\"),\n" +
            "        (\"spi\" => \"eventsStore\"),\n" +
            "        (\"provider\" => \"jpa\")\n" +
            "    ],\n" +
            "    \"properties\" => {\"exclude-events\" => \"[\\\"REFRESH_TOKEN\\\"]\"},\n" +
            "    \"enabled\" => true\n" +
            "}"
        ));
        
        ops.add(ModelNode.fromString(
            "{\n" +
            "    \"operation\" => \"add\",\n" +
            "    \"address\" => [\n" +
            "        (\"subsystem\" => \"keycloak-server\"),\n" +
            "        (\"spi\" => \"realm\")\n" +
            "    ],\n" +
            "    \"default-provider\" => \"jpa\"\n" +
            "}"
        ));
        
        ops.add(ModelNode.fromString(
            "{\n" +
            "    \"operation\" => \"add\",\n" +
            "    \"address\" => [\n" +
            "        (\"subsystem\" => \"keycloak-server\"),\n" +
            "        (\"spi\" => \"user\")\n" +
            "    ],\n" +
            "    \"default-provider\" => \"jpa\"\n" +
            "}"
        ));
        
        ops.add(ModelNode.fromString(
            "{\n" +
            "    \"operation\" => \"add\",\n" +
            "    \"address\" => [\n" +
            "        (\"subsystem\" => \"keycloak-server\"),\n" +
            "        (\"spi\" => \"userCache\")\n" +
            "    ]\n" +
            "}"
        ));
        
        ops.add(ModelNode.fromString(
            "{\n" +
            "    \"operation\" => \"add\",\n" +
            "    \"address\" => [\n" +
            "        (\"subsystem\" => \"keycloak-server\"),\n" +
            "        (\"spi\" => \"userCache\"),\n" +
            "        (\"provider\" => \"default\")\n" +
            "    ],\n" +
            "    \"enabled\" => true\n" +
            "}"
        ));
        
        ops.add(ModelNode.fromString(
            "{\n" +
            "    \"operation\" => \"add\",\n" +
            "    \"address\" => [\n" +
            "        (\"subsystem\" => \"keycloak-server\"),\n" +
            "        (\"spi\" => \"userSessionPersister\")\n" +
            "    ],\n" +
            "    \"default-provider\" => \"jpa\"\n" +
            "}"
        ));
        
        ops.add(ModelNode.fromString(
            "{\n" +
            "    \"operation\" => \"add\",\n" +
            "    \"address\" => [\n" +
            "        (\"subsystem\" => \"keycloak-server\"),\n" +
            "        (\"spi\" => \"authorizationPersister\")\n" +
            "    ],\n" +
            "    \"default-provider\" => \"jpa\"\n" +
            "}"
        ));
        
        ops.add(ModelNode.fromString(
            "{\n" +
            "    \"operation\" => \"add\",\n" +
            "    \"address\" => [\n" +
            "        (\"subsystem\" => \"keycloak-server\"),\n" +
            "        (\"spi\" => \"timer\")\n" +
            "    ],\n" +
            "    \"default-provider\" => \"basic\"\n" +
            "}"
        ));
        
        ops.add(ModelNode.fromString(
            "{\n" +
            "    \"operation\" => \"add\",\n" +
            "    \"address\" => [\n" +
            "        (\"subsystem\" => \"keycloak-server\"),\n" +
            "        (\"spi\" => \"connectionsHttpClient\")\n" +
            "    ]\n" +
            "}"
        ));
        
        ops.add(ModelNode.fromString(
            "{\n" +
            "    \"operation\" => \"add\",\n" +
            "    \"address\" => [\n" +
            "        (\"subsystem\" => \"keycloak-server\"),\n" +
            "        (\"spi\" => \"connectionsHttpClient\"),\n" +
            "        (\"provider\" => \"default\")\n" +
            "    ],\n" +
            "    \"enabled\" => true\n" +
            "}"
        ));
        
        ops.add(ModelNode.fromString(
            "{\n" +
            "    \"operation\" => \"add\",\n" +
            "    \"address\" => [\n" +
            "        (\"subsystem\" => \"keycloak-server\"),\n" +
            "        (\"spi\" => \"connectionsJpa\")\n" +
            "    ]\n" +
            "}"
        ));
        
        ops.add(ModelNode.fromString(
            "{\n" +
            "    \"operation\" => \"add\",\n" +
            "    \"address\" => [\n" +
            "        (\"subsystem\" => \"keycloak-server\"),\n" +
            "        (\"spi\" => \"connectionsJpa\"),\n" +
            "        (\"provider\" => \"default\")\n" +
            "    ],\n" +
            "    \"properties\" => {\n" +
            "        \"dataSource\" => \"java:jboss/datasources/KeycloakDS\",\n" +
            "        \"databaseSchema\" => \"update\"\n" +
            "    },\n" +
            "    \"enabled\" => true\n" +
            "}"
        ));
        
        ops.add(ModelNode.fromString(
            "{\n" +
            "    \"operation\" => \"add\",\n" +
            "    \"address\" => [\n" +
            "        (\"subsystem\" => \"keycloak-server\"),\n" +
            "        (\"spi\" => \"realmCache\")\n" +
            "    ]\n" +
            "}"
        ));
        
        ops.add(ModelNode.fromString(
            "{\n" +
            "    \"operation\" => \"add\",\n" +
            "    \"address\" => [\n" +
            "        (\"subsystem\" => \"keycloak-server\"),\n" +
            "        (\"spi\" => \"realmCache\"),\n" +
            "        (\"provider\" => \"default\")\n" +
            "    ],\n" +
            "    \"enabled\" => true\n" +
            "}"
        ));
        
        ops.add(ModelNode.fromString(
            "{\n" +
            "    \"operation\" => \"add\",\n" +
            "    \"address\" => [\n" +
            "        (\"subsystem\" => \"keycloak-server\"),\n" +
            "        (\"spi\" => \"connectionsInfinispan\")\n" +
            "    ],\n" +
            "    \"default-provider\" => \"default\"\n" +
            "}"
        ));
        
        ops.add(ModelNode.fromString(
            "{\n" +
            "    \"operation\" => \"add\",\n" +
            "    \"address\" => [\n" +
            "        (\"subsystem\" => \"keycloak-server\"),\n" +
            "        (\"spi\" => \"connectionsInfinispan\"),\n" +
            "        (\"provider\" => \"default\")\n" +
            "    ],\n" +
            "    \"properties\" => {\"cacheContainer\" => \"java:jboss/infinispan/container/keycloak\"},\n" +
            "    \"enabled\" => true\n" +
            "}"
        ));

        if (isDomain) { // prepend the domain root
            for (ModelNode op : ops) {
                PathAddress addr = PathAddress.pathAddress(op.get(ADDRESS));
                PathAddress domainAddr = PathAddress.pathAddress(domainRoot).append(addr);
                op.get(ADDRESS).set(domainAddr.toModelNode());
            }
        }
        
        return ops;
    }
}