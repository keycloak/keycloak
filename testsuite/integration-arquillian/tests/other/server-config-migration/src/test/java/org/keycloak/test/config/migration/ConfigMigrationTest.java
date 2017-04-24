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
import org.jboss.dmr.ModelNode;
import org.junit.Assert;
import org.junit.Test;

/**
 * Compare outputs from jboss-cli read-resource operations.  This compare the total
 * configuration of all subsystems to make sure that the version in master
 * matches the migrated version.
 *
 * @author Stan Silvert ssilvert@redhat.com (C) 2016 Red Hat Inc.
 */
public class ConfigMigrationTest {
    
    private static final File TARGET_DIR = new File("./target");
    private final String migratedVersion = System.getProperty("migrated.version");

    @Test
    public void testStandalone() throws IOException {
        compareConfigs("master-standalone.txt", "migrated-standalone-" + migratedVersion + ".txt");
    }
    
    @Test
    public void testStandaloneHA() throws IOException {
        compareConfigs("master-standalone-ha.txt", "migrated-standalone-ha-" + migratedVersion + ".txt");
    }
    
    @Test
    public void testDomain() throws IOException {
        compareConfigs("master-domain-standalone.txt", "migrated-domain-standalone-" + migratedVersion + ".txt");
        compareConfigs("master-domain-clustered.txt", "migrated-domain-clustered-" + migratedVersion + ".txt");
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
            Assert.assertEquals(master, migrated);
        }
    }
}
