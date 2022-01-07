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

package org.keycloak.subsystem.server.attributes;

import org.jboss.as.controller.StringListAttributeDefinition;
import org.jboss.dmr.ModelNode;

/**
 *
 * @author Stan Silvert ssilvert@redhat.com (C) 2016 Red Hat Inc.
 */
public class ProvidersListAttributeBuilder extends StringListAttributeDefinition.Builder {
    public ProvidersListAttributeBuilder() {
        super("providers");
        ModelNode provider = new ModelNode();
        provider.add("classpath:${jboss.home.dir}/providers/*");
        setDefaultValue(provider);
        setAllowExpression(true);
        setRequired(false);
    }
    
}
