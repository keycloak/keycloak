/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
 *  and other contributors as indicated by the @author tags.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.keycloak.adapters.saml;

import java.io.InputStream;

import org.keycloak.adapters.saml.config.parsers.DeploymentBuilder;
import org.keycloak.adapters.saml.config.parsers.ResourceLoader;
import org.keycloak.common.enums.SslRequired;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class DeploymentBuilderTest {

    @Test
    public void testPropertiesBasedRoleMapper() throws Exception {
        SamlDeployment deployment = loadDeployment("config/parsers/keycloak-saml-pem-keys.xml");
        Assert.assertNotNull(deployment);
        Assert.assertNotNull(deployment.getSigningKeyPair().getPrivate());
        Assert.assertNotNull(deployment.getSigningKeyPair().getPublic());
    }

    @Test
    public void testDeploymentBuilderMapsConfiguration() throws Exception {
        SamlDeployment deployment = loadDeployment("config/parsers/keycloak-saml-deployment-builder-mapping.xml");

        Assert.assertNotNull(deployment);
        Assert.assertEquals("http://localhost:8081/sales-post-sig/", deployment.getEntityID());
        Assert.assertEquals(SslRequired.ALL, deployment.getSslRequired());
        Assert.assertEquals("urn:oasis:names:tc:SAML:1.1:nameid-format:emailAddress", deployment.getNameIDPolicyFormat());
        Assert.assertEquals("/logout.jsp", deployment.getLogoutPage());
        Assert.assertTrue(deployment.isForceAuthentication());
        Assert.assertTrue(deployment.isIsPassive());
        Assert.assertTrue(deployment.turnOffChangeSessionIdOnLogin());
    }

    private SamlDeployment loadDeployment(String resource) throws Exception {
        InputStream is = getClass().getResourceAsStream(resource);
        return new DeploymentBuilder().build(is, new ResourceLoader() {
            @Override
            public InputStream getResourceAsStream(String resource) {
                return this.getClass().getClassLoader().getResourceAsStream(resource);
            }
        });
    }
}
