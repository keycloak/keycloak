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

package org.keycloak.subsystem.server.extension;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationDefinition;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADDRESS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;

/**
 * This operation provides a migration path from keycloak-server.json to
 * standalone.xml or domain.xml.
 *
 * @author Stan Silvert ssilvert@redhat.com (C) 2016 Red Hat Inc.
 */
public class MigrateJsonOperation implements OperationStepHandler {
    public static final String OPERATION_NAME = "migrate-json";
    
    private static final String CONFIG_DIR = System.getProperty("jboss.server.config.dir");
    private static final Path DEFAULT_CONFIG_FILE = Paths.get(CONFIG_DIR, "keycloak-server.json");

    private static final AttributeDefinition FILE_ATTRIBUTE = SimpleAttributeDefinitionBuilder.create("file", ModelType.BYTES, true).build();
    public static final OperationDefinition DEFINITION = new SimpleOperationDefinitionBuilder(OPERATION_NAME, KeycloakExtension.getResourceDescriptionResolver())
            .setRuntimeOnly()
            .setReadOnly()
            .setReplyType(ModelType.STRING)
            .setParameters(FILE_ATTRIBUTE)
            .build();

    private String localConfig() throws IOException {
        if (Files.notExists(DEFAULT_CONFIG_FILE)) return null;
        return new String(Files.readAllBytes(DEFAULT_CONFIG_FILE));
    }
    
    private String readConfig(ModelNode operation) throws IOException {
        ModelNode file = operation.get(FILE_ATTRIBUTE.getName());
        if (file.isDefined() && file.asBytes().length > 0) {
            return new String(file.asBytes());
        }
        
        String localConfig = localConfig();
        if (localConfig != null) return localConfig;
        
        throw new IOException("Can not find json file to migrate");
    }
    
    @Override
    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
        List<ModelNode> ops = null;
        try {
            PathAddress currentAddr = context.getCurrentAddress();
            ops = JsonConfigConverter.convertJsonConfig(readConfig(operation), currentAddr);
        } catch (IOException ioe) {
            throw new OperationFailedException(ioe);
        }
         
        for (ModelNode op : ops) {
            PathAddress addr = PathAddress.pathAddress(op.get(ADDRESS));
            String opName = op.get(OP).asString();
            context.addStep(op, 
                            context.getRootResourceRegistration().getOperationHandler(addr, opName), 
                            OperationContext.Stage.MODEL);
        }
        
        context.completeStep(OperationContext.RollbackHandler.NOOP_ROLLBACK_HANDLER);
    }
    
}
