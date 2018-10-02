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
package org.keycloak.testsuite.arquillian.undertow.container;

import org.jboss.arquillian.container.test.spi.client.deployment.ApplicationArchiveProcessor;
import org.jboss.arquillian.test.spi.TestClass;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.Archive;
import org.keycloak.testsuite.utils.annotation.UseServletFilter;
import org.keycloak.testsuite.utils.arquillian.DeploymentArchiveProcessorUtils;

/**
 *
 * @author <a href="mailto:vramik@redhat.com">Vlasta Ramik</a>
 */
public class UndertowDeploymentArchiveProcessor implements ApplicationArchiveProcessor {

    private final Logger log = Logger.getLogger(UndertowDeploymentArchiveProcessor.class);

    @Override
    public void process(Archive<?> archive, TestClass testClass) {
        if (DeploymentArchiveProcessorUtils.checkRunOnServerDeployment(archive)) return;
        if (!System.getProperty("app.server", "undertow").equals("undertow")) return;

        modifyWebXML(archive, testClass);

        modifyOIDCAdapterConfig(archive, DeploymentArchiveProcessorUtils.ADAPTER_CONFIG_PATH);

        modifySAMLAdapterConfig(archive, DeploymentArchiveProcessorUtils.SAML_ADAPTER_CONFIG_PATH);
        modifySAMLAdapterConfig(archive, DeploymentArchiveProcessorUtils.SAML_ADAPTER_CONFIG_PATH_TENANT1);
        modifySAMLAdapterConfig(archive, DeploymentArchiveProcessorUtils.SAML_ADAPTER_CONFIG_PATH_TENANT2);

        modifyOIDCAdapterConfig(archive, DeploymentArchiveProcessorUtils.ADAPTER_CONFIG_PATH_JS);
    }

    private void modifyWebXML(Archive<?> archive, TestClass testClass) {
        if (!archive.contains(DeploymentArchiveProcessorUtils.WEBXML_PATH)) return;
        if (!testClass.getJavaClass().isAnnotationPresent(UseServletFilter.class)) return;

        log.debug("Modifying WEB.XML in " + archive.getName() + " for Servlet Filter.");
        DeploymentArchiveProcessorUtils.modifyWebXMLForServletFilter(archive, testClass);
    }

    private void modifyOIDCAdapterConfig(Archive<?> archive, String adapterConfigPath) {
        if (!archive.contains(adapterConfigPath)) return;

        log.debug("Modifying adapter config " + adapterConfigPath + " in " + archive.getName());

        DeploymentArchiveProcessorUtils.modifyOIDCAdapterConfig(archive, adapterConfigPath);
    }

    private void modifySAMLAdapterConfig(Archive<?> archive, String adapterConfigPath) {
        if (!archive.contains(adapterConfigPath)) return;

        log.debug("Modifying adapter config " + adapterConfigPath + " in " + archive.getName());
        DeploymentArchiveProcessorUtils.modifySAMLAdapterConfig(archive, adapterConfigPath);
    }
}
