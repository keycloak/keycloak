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
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.StringListAttributeDefinition;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.keycloak.subsystem.server.attributes.ModulesListAttributeBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Stan Silvert ssilvert@redhat.com (C) 2016 Red Hat Inc.
 */
public class ThemeResourceDefinition extends SimpleResourceDefinition {

    public static final String TAG_NAME = "theme";
    
    // This is the internal name of the singleton resource
    public static final String RESOURCE_NAME = "defaults";
    
    // NOTE: All attributes must be SimpleAttributeDefinition.  If that needs to
    //       change then refactor starting with lookup() method below.
    static final SimpleAttributeDefinition STATIC_MAX_AGE =
        new SimpleAttributeDefinitionBuilder("staticMaxAge", ModelType.LONG, true)
            .setAllowExpression(true)
            .setDefaultValue(new ModelNode("2592000"))
            .setRestartAllServices()
            .build();
    
    static final SimpleAttributeDefinition CACHE_THEMES =
        new SimpleAttributeDefinitionBuilder("cacheThemes", ModelType.BOOLEAN, true)
                .setAllowExpression(true)
                .setDefaultValue(new ModelNode(true))
                .setAllowNull(false)
                .setRestartAllServices()
                .build();
    
    static final SimpleAttributeDefinition CACHE_TEMPLATES =
        new SimpleAttributeDefinitionBuilder("cacheTemplates", ModelType.BOOLEAN, true)
                .setAllowExpression(true)
                .setDefaultValue(new ModelNode(true))
                .setAllowNull(false)
                .setRestartAllServices()
                .build();
    
    static final SimpleAttributeDefinition WELCOME_THEME =
        new SimpleAttributeDefinitionBuilder("welcomeTheme", ModelType.STRING, true)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();
    
    static final SimpleAttributeDefinition DEFAULT =
        new SimpleAttributeDefinitionBuilder("default", ModelType.STRING, true)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();
    
    static final SimpleAttributeDefinition DIR =
        new SimpleAttributeDefinitionBuilder("dir", ModelType.STRING, true)
            .setAllowExpression(true)
            .setDefaultValue(new ModelNode("${jboss.home.dir}/themes"))
            .setRestartAllServices()
            .build();
    
    static final StringListAttributeDefinition MODULES = new ModulesListAttributeBuilder().build();
    
    static final List<AttributeDefinition> ALL_ATTRIBUTES = new ArrayList<>();

    static {
        ALL_ATTRIBUTES.add(STATIC_MAX_AGE);
        ALL_ATTRIBUTES.add(CACHE_THEMES);
        ALL_ATTRIBUTES.add(CACHE_TEMPLATES);
        ALL_ATTRIBUTES.add(WELCOME_THEME);
        ALL_ATTRIBUTES.add(DEFAULT);
        ALL_ATTRIBUTES.add(DIR);
        ALL_ATTRIBUTES.add(MODULES);
    }

    private static final Map<String, AttributeDefinition> DEFINITION_LOOKUP = new HashMap<>();
    static {
        for (AttributeDefinition def : ALL_ATTRIBUTES) {
            DEFINITION_LOOKUP.put(def.getXmlName(), def);
        }
    }
    
    protected static final ReloadRequiredWriteAttributeHandler WRITE_ATTR_HANDLER = new ReloadRequiredWriteAttributeHandler(ALL_ATTRIBUTES);
    
    protected ThemeResourceDefinition() {
        super(PathElement.pathElement(TAG_NAME),
            KeycloakExtension.getResourceDescriptionResolver(TAG_NAME),
            ThemeResourceAddHandler.INSTANCE,
            ThemeResourceRemoveHandler.INSTANCE
        );
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        super.registerAttributes(resourceRegistration);
        
        for (AttributeDefinition def : ALL_ATTRIBUTES) {
            resourceRegistration.registerReadWriteAttribute(def, null, WRITE_ATTR_HANDLER);
        }
    }

    public static SimpleAttributeDefinition lookup(String name) {
        return (SimpleAttributeDefinition)DEFINITION_LOOKUP.get(name);
    }
    
}
