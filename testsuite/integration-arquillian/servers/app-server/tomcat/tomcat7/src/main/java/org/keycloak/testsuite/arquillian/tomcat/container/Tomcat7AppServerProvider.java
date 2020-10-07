/*
 * Copyright 2018 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.testsuite.arquillian.tomcat.container;

import java.util.ArrayList;
import java.util.List;

import org.jboss.arquillian.container.tomcat.managed.Tomcat7ManagedContainer;
import org.jboss.arquillian.core.spi.Validate;
import org.jboss.shrinkwrap.descriptor.spi.node.Node;
import org.keycloak.testsuite.arquillian.container.AppServerContainerProvider;
import org.keycloak.testsuite.utils.arquillian.tomcat.TomcatAppServerConfigurationUtils;

public class Tomcat7AppServerProvider extends AbstractTomcatAppServerProvider {


    @Override
    public String getName() {
        return "tomcat7";
    }

    @Override
    protected String getContainerClassName() {
        return Tomcat7ManagedContainer.class.getName();
    }
}
