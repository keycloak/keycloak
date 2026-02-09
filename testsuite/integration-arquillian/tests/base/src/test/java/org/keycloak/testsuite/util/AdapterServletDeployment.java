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
package org.keycloak.testsuite.util;

import java.net.URL;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;

/**
 * Expects a structure like adapter-test directory
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class AdapterServletDeployment {

    public static final String JBOSS_DEPLOYMENT_STRUCTURE_XML = "jboss-deployment-structure.xml";

    public static WebArchive oidcDeployment(String name, String configRoot, Class... servletClasses) {
        return oidcDeployment(name, configRoot, "keycloak.json");

    }


    public static WebArchive oidcDeployment(String name, String configRoot, String adapterConfigFilename, Class... servletClasses) {
        String configPath = configRoot + "/" + name;
        String webInfPath = configPath + "/WEB-INF/";

        URL keycloakJSON = AdapterServletDeployment.class.getResource(webInfPath + adapterConfigFilename);
        URL webXML = AdapterServletDeployment.class.getResource(webInfPath + "web.xml");

        WebArchive deployment = ShrinkWrap.create(WebArchive.class, name + ".war")
                .addClasses(servletClasses)
                .addAsWebInfResource(webXML, "web.xml");

        URL keystore = AdapterServletDeployment.class.getResource(webInfPath + "keystore.jks");
        if (keystore != null) {
            deployment.addAsWebInfResource(keystore, "classes/keystore.jks");
        }

        if (keycloakJSON != null) {
            deployment.addAsWebInfResource(keycloakJSON, "keycloak.json");
        }

        URL jbossDeploymentStructure = AdapterServletDeployment.class.getResource(webInfPath + JBOSS_DEPLOYMENT_STRUCTURE_XML);
        if (jbossDeploymentStructure == null) {
            jbossDeploymentStructure = AdapterServletDeployment.class.getResource(configRoot + "/" + JBOSS_DEPLOYMENT_STRUCTURE_XML);
        }
        if (jbossDeploymentStructure != null) deployment.addAsWebInfResource(jbossDeploymentStructure, JBOSS_DEPLOYMENT_STRUCTURE_XML);

        return deployment;
    }
}
