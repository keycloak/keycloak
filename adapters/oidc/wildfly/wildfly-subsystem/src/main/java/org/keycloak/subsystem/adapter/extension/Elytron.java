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
package org.keycloak.subsystem.adapter.extension;

import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.web.common.WarMetaData;
import org.jboss.metadata.web.jboss.JBossWebMetaData;
import org.jboss.msc.service.ServiceName;

/**
 * Utility class for Elytron integration
 *
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public final class Elytron {

    private static final String DEFAULT_SECURITY_DOMAIN = "other";
    private static final String UNDERTOW_APPLICATION_SECURITY_DOMAIN = "org.wildfly.undertow.application-security-domain.";

    static boolean isElytronEnabled(DeploymentPhaseContext phaseContext) {
        String securityDomain = getSecurityDomain(phaseContext.getDeploymentUnit());
        ServiceName serviceName = ServiceName.parse(new StringBuilder(UNDERTOW_APPLICATION_SECURITY_DOMAIN).append(securityDomain).toString());
        return phaseContext.getServiceRegistry().getService(serviceName) != null;
    }

    private static String getSecurityDomain(DeploymentUnit deploymentUnit) {
        WarMetaData warMetaData = deploymentUnit.getAttachment(WarMetaData.ATTACHMENT_KEY);

        if (warMetaData != null) {
            JBossWebMetaData webMetaData = warMetaData.getMergedJBossWebMetaData();

            if (webMetaData != null) {
                String configuredSecurityDomain = webMetaData.getSecurityDomain();

                if (configuredSecurityDomain != null) {
                    return configuredSecurityDomain;
                }
            }
        }

        return DEFAULT_SECURITY_DOMAIN;
    }
}
