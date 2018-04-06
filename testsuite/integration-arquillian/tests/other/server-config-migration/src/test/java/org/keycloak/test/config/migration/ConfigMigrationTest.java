/*
 * Copyright 2016 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @author tags. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.keycloak.test.config.migration;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.junit.Assert;
import org.junit.Test;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

/**
 * Compare outputs from jboss-cli read-resource operations.  This compare the total
 * configuration of all subsystems to make sure that the version in master
 * matches the migrated version.
 *
 * @author Stan Silvert ssilvert@redhat.com (C) 2016 Red Hat Inc.
 */
public class ConfigMigrationTest {
    
    private static final File TARGET_DIR = new File("./target");
    private final Logger log = Logger.getLogger(ConfigMigrationTest.class);
    private final Deque<String> nav = new LinkedList<>();

    @Test
    public void testStandalone() throws IOException {
        compareConfigs("master-standalone.txt", "migrated-standalone.txt");
    }
    
    @Test
    public void testStandaloneHA() throws IOException {
        compareConfigs("master-standalone-ha.txt", "migrated-standalone-ha.txt");
    }
    
    @Test
    public void testDomain() throws IOException {
        compareConfigs("master-domain-standalone.txt", "migrated-domain-standalone.txt");
        compareConfigs("master-domain-clustered.txt", "migrated-domain-clustered.txt");
        
        compareConfigs("master-domain-core-service.txt", "migrated-domain-core-service.txt");
        compareConfigs("master-domain-extension.txt", "migrated-domain-extension.txt");
//        compareConfigs("master-domain-interface.txt", "migrated-domain-interface.txt");
    }
    
    private void compareConfigs(String masterConfig, String migratedConfig) throws IOException {
        File masterFile = new File(TARGET_DIR, masterConfig);
        Assert.assertTrue(masterFile.exists());
        File migratedFile = new File(TARGET_DIR, migratedConfig);
        Assert.assertTrue(migratedFile.exists());
        
        try (
            FileInputStream masterStream = new FileInputStream(masterFile);
            FileInputStream migratedStream = new FileInputStream(migratedFile);
        ) {
            // Convert to ModelNode to test equality.
            // A textual diff might have things out of order.
            ModelNode master = ModelNode.fromStream(masterStream);
            ModelNode migrated = ModelNode.fromStream(migratedStream);

            if (master.equals(migrated)) {
                // ok
            } else {
                if (Boolean.parseBoolean(System.getProperty("get.simple.full.comparison"))) {
                    assertThat(migrated, is(equalTo(master)));
                }
                compareConfigsDeeply("root", master, migrated);
            }
        } 
    }
    
    private void compareConfigsDeeply(String id, ModelNode master, ModelNode migrated) {
        nav.add(id);
        
        master.protect();
        migrated.protect();

        assertEquals(getMessage(), master.getType(), migrated.getType());
        
        switch (master.getType()) {
            case OBJECT:
                //check nodes are equal
                if (master.equals(migrated)) {
                    break;
                }
                //check keys are equal
                assertThat(getMessage(), migrated.keys(), is(equalTo(master.keys())));

                for (String key : master.keys()) {
                    compareConfigsDeeply(key, master.get(key), migrated.get(key));
                }
                break;
            case LIST:
                List<ModelNode> masterAsList = new ArrayList<>(master.asList());
                List<ModelNode> migratedAsList = new ArrayList<>(migrated.asList());
                
                if (masterAsList.equals(migratedAsList)) {
                    break;
                }
                
                masterAsList.sort(nodeStringComparator);
                migratedAsList.sort(nodeStringComparator);
                
                if (masterAsList.toString().contains("subsystem")) {
                    assertEquals("Subsystem names are not equal.", 
                            getSubsystemNames(masterAsList).toString(), 
                            getSubsystemNames(migratedAsList).toString());
                }

                //remove equaled nodes and keep just different ones
                List<ModelNode> diffNodesInMaster = new ArrayList<>(masterAsList);
                diffNodesInMaster.removeAll(migratedAsList);
                for (ModelNode diffNodeInMaster : diffNodesInMaster) {
                    String navigation = diffNodeInMaster.getType().toString();
                    if (diffNodeInMaster.toString().contains("subsystem")) {
                        navigation = getSubsystemNames(Arrays.asList(diffNodeInMaster)).toString();
                    } 
                    compareConfigsDeeply(navigation, 
                            diffNodeInMaster, 
                            migratedAsList.get(masterAsList.indexOf(diffNodeInMaster)));
                }
                break;
            case BOOLEAN:
                assertEquals(getMessage(), master.asBoolean(), migrated.asBoolean());
                break;
            case STRING:
                assertEquals(getMessage(), master.asString(), migrated.asString());
                break;
            case UNDEFINED:
                //nothing to test
                break;
            case LONG:
                assertEquals(getMessage(), master.asLong(), migrated.asLong());
                break;
            case EXPRESSION:
                assertEquals(getMessage(), master.asExpression(), migrated.asExpression());
                break;
            case INT:
                assertEquals(getMessage(), master.asInt(), migrated.asInt());
                break;
            case DOUBLE:
                assertEquals(getMessage(), master.asDouble(), migrated.asDouble(), new Double("0.0"));
                break;
            default:
                assertThat(getMessage(), migrated, is(equalTo(master)));
                throw new UnsupportedOperationException(getMessage() + ". There is missing case " + master.getType().name());
        }
        nav.pollLast();
    }

    private static final Comparator<ModelNode> nodeStringComparator = (n1, n2) -> {
        //ascending order
        return n1.toString().compareTo(n2.toString());
    };
    
    private String getMessage() {
        return "* navigation -> " + nav.toString() + " * ";
    }

    private List<String> getSubsystemNames(List<ModelNode> modelNodes) {
        int index;
        if (modelNodes.toString().contains("profile")) {
            index = 9; //domain
        } else {
            index = 5; //standalone
        }
        return modelNodes
                .stream()
                .map(ModelNode::toString)
                .map(s -> s.split("\"")[index])
                .collect(Collectors.toList());
    }
}
