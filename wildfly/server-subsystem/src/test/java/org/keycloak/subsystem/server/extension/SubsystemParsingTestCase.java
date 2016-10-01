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

import org.jboss.as.subsystem.test.AbstractSubsystemBaseTest;

import java.io.IOException;
import java.util.Properties;

/**
 * Tests all management expects for subsystem, parsing, marshaling, model definition and other
 * Here is an example that allows you a fine grained controller over what is tested and how. So it can give you ideas what can be done and tested.
 * If you have no need for advanced testing of subsystem you look at {@link AbstractSubsystemBaseTest} that testes same stuff but most of the code
 * is hidden inside of test harness
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @author Tomaz Cerar
 * @author Marko Strukelj
 */
public class SubsystemParsingTestCase extends AbstractSubsystemBaseTest {

    public SubsystemParsingTestCase() {
        super(KeycloakExtension.SUBSYSTEM_NAME, new KeycloakExtension());
    }

    @Override
    protected Properties getResolvedProperties() {
        Properties properties = new Properties();
        properties.put("jboss.home.dir", System.getProperty("java.io.tmpdir"));
        properties.put("keycloak.jta.lookup.provider", "jboss");
        return properties;
    }
    
    @Override
    protected String getSubsystemXml() throws IOException {
        return readResource("keycloak-server-1.1.xml");
    }

    @Override
    protected String getSubsystemXsdPath() throws Exception {
        return "schema/wildfly-keycloak-server_1_1.xsd";
    }

    @Override
    protected String[] getSubsystemTemplatePaths() throws IOException {
        return new String[]{
            "/subsystem-templates/keycloak-server.xml"
        };
    }
}
