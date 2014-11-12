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
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;

/**
 * Operation to overlay keycloak-server.json.
 *
 * @author Stan Silvert ssilvert@redhat.com (C) 2014 Red Hat Inc.
 */
public class OverlayKeycloakServerJsonHandler extends AbstractAddOverlayHandler {

    public static final String OP = "update-server-config";

    public static final OverlayKeycloakServerJsonHandler INSTANCE = new OverlayKeycloakServerJsonHandler();

    public static OperationDefinition DEFINITION = new SimpleOperationDefinitionBuilder(OP, AuthServerDefinition.rscDescriptionResolver)
            .addParameter(BYTES_TO_UPLOAD)
            .addParameter(REDEPLOY_SERVER)
            .addParameter(OVERWRITE)
            .build();

    private OverlayKeycloakServerJsonHandler() {}
    
    @Override
    String getOverlayPath(String fileName) {
        return "/WEB-INF/classes/META-INF/keycloak-server.json";
    }

}
