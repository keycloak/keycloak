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

import org.jboss.arquillian.container.test.spi.client.deployment.ApplicationArchiveProcessor;
import org.jboss.arquillian.test.spi.TestClass;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.keycloak.testsuite.adapter.servlet.TomcatConfigApplication;
import org.keycloak.testsuite.utils.arquillian.DeploymentArchiveProcessorUtils;
import org.keycloak.testsuite.utils.arquillian.tomcat.TomcatDeploymentArchiveProcessorUtils;

public class CommonTomcatDeploymentArchiveProcessor implements ApplicationArchiveProcessor {

    private final Logger log = Logger.getLogger(CommonTomcatDeploymentArchiveProcessor.class);

    @Override
    public void process(Archive<?> archive, TestClass testClass) {
        if (DeploymentArchiveProcessorUtils.checkRunOnServerDeployment(archive)) return;

        modifyOIDCAdapterConfig(archive, DeploymentArchiveProcessorUtils.ADAPTER_CONFIG_PATH);

        DeploymentArchiveProcessorUtils.SAML_CONFIGS.forEach(path -> modifySAMLAdapterConfig(archive, path));

        TomcatDeploymentArchiveProcessorUtils.copyWarClasspathFilesToCommonTomcatClasspath(archive);

        // KEYCLOAK-9606 - might be unnecessary, however for now we need to test what is in docs
        TomcatDeploymentArchiveProcessorUtils.replaceKEYCLOAKMethodWithBASIC(archive);

        if (containsSAMLAdapterConfig(archive)) {
            TomcatDeploymentArchiveProcessorUtils.replaceOIDCValveWithSAMLValve(archive);
        }

        if (TomcatDeploymentArchiveProcessorUtils.isJaxRSApp(archive)) {
            TomcatDeploymentArchiveProcessorUtils.removeServletConfigurationInWebXML(archive);

            if (!TomcatDeploymentArchiveProcessorUtils.containsApplicationConfigClass(archive)) {
                ((WebArchive) archive).addClass(TomcatConfigApplication.class);
            }
        }
    }

    private boolean containsSAMLAdapterConfig(Archive<?> archive) {
        return  DeploymentArchiveProcessorUtils.SAML_CONFIGS
                .stream()
                .anyMatch(archive::contains);
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
