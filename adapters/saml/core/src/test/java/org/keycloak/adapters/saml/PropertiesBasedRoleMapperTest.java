/*
 * Copyright 2019 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.adapters.saml;

import java.io.InputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.keycloak.adapters.saml.config.parsers.DeploymentBuilder;
import org.keycloak.adapters.saml.config.parsers.ResourceLoader;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Tests for the {@link PropertiesBasedRoleMapper} implementation.
 *
 * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
 */
public class PropertiesBasedRoleMapperTest {

    @Test
    public void testPropertiesBasedRoleMapper() throws Exception {
        InputStream is = getClass().getResourceAsStream("config/parsers/keycloak-saml-with-role-mappings-provider.xml");
        SamlDeployment deployment = new DeploymentBuilder().build(is, new ResourceLoader() {
            @Override
            public InputStream getResourceAsStream(String resource) {
                return this.getClass().getClassLoader().getResourceAsStream(resource);
            }
        });
        // retrieve the configured role mappings provider - in this case we know it is the properties-based implementation.
        RoleMappingsProvider provider = deployment.getRoleMappingsProvider();

        // if provider was properly configured we should be able to see the mappings as specified in the properties file.
        final Set<String> samlRoles = new HashSet<>(Arrays.asList(new String[]{"samlRoleA", "samlRoleB", "samlRoleC"}));
        final Set<String> mappedRoles = provider.map("kc-user", samlRoles);

        // we expect samlRoleB to be removed, samlRoleA to be mapped into two roles (jeeRoleX, jeeRoleY) and also the principal should
        // be granted an extra role (jeeRoleZ).
        assertNotNull(mappedRoles);
        assertEquals(4, mappedRoles.size());
        Set<String> expectedRoles = new HashSet<>(Arrays.asList(new String[]{"samlRoleC", "jeeRoleX", "jeeRoleY", "jeeRoleZ"}));
        assertEquals(expectedRoles, mappedRoles);
    }
}
