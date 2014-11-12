/*
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
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

package org.keycloak.subsystem.extension.authserver;

import org.jboss.as.controller.OperationDefinition;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * Operation to add a provider jar to WEB-INF/lib.
 *
 * @author Stan Silvert ssilvert@redhat.com (C) 2014 Red Hat Inc.
 */
public class AddProviderHandler extends AbstractAddOverlayHandler {

    public static final String OP = "add-provider";

    public static final AddProviderHandler INSTANCE = new AddProviderHandler();

    protected static final SimpleAttributeDefinition UPLOADED_FILE_NAME =
            new SimpleAttributeDefinitionBuilder(UPLOADED_FILE_OP_NAME, ModelType.STRING, false)
            .setAllowExpression(false)
            .setAllowNull(false)
            .setDefaultValue(new ModelNode().set("myprovider.jar"))
            .build();

    public static OperationDefinition DEFINITION = new SimpleOperationDefinitionBuilder(OP, AuthServerDefinition.rscDescriptionResolver)
            .addParameter(BYTES_TO_UPLOAD)
            .addParameter(UPLOADED_FILE_NAME)
            .addParameter(REDEPLOY_SERVER)
            .addParameter(OVERWRITE)
            .build();

    private AddProviderHandler() {}
    
    @Override
    String getOverlayPath(String fileName) {
        if (!fileName.toLowerCase().endsWith(".jar")) {
            throw new IllegalArgumentException("Uploaded file name must end with .jar");
        }
        return "/WEB-INF/lib/" + fileName;
    }

}
