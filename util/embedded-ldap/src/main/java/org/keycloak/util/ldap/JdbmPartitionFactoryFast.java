/*
 * Copyright 2022. Red Hat, Inc. and/or its affiliates
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

package org.keycloak.util.ldap;

import java.io.File;

import org.apache.directory.api.ldap.model.schema.SchemaManager;
import org.apache.directory.server.core.api.DnFactory;
import org.apache.directory.server.core.factory.JdbmPartitionFactory;
import org.apache.directory.server.core.partition.impl.btree.jdbm.JdbmPartition;

/**
 * @author Alexander Schwartz
 */
public class JdbmPartitionFactoryFast extends JdbmPartitionFactory {
    @Override
    public JdbmPartition createPartition(SchemaManager schemaManager, DnFactory dnFactory, String id, String suffix, int cacheSize, File workingDirectory) throws Exception {
        JdbmPartition partition = super.createPartition(schemaManager, dnFactory, id, suffix, cacheSize, workingDirectory);
        // don't write to disk on every update, thereby slightly speeding up the tests
        partition.setSyncOnWrite(false);
        return partition;
    }
}
