/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.subsystem.adapter.saml.extension;

import org.keycloak.adapters.saml.AdapterConstants;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.jboss.as.controller.capability.CapabilityServiceSupport;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.web.common.WarMetaData;
import org.jboss.logging.Logger;
import org.jboss.metadata.javaee.spec.ParamValueMetaData;
import org.jboss.metadata.web.jboss.JBossWebMetaData;
import org.jboss.metadata.web.spec.LoginConfigMetaData;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;

/**
 *
 * @author hmlnarik
 */
public class KeycloakClusteredSsoDeploymentProcessor implements DeploymentUnitProcessor {

    private static final Logger LOG = Logger.getLogger(KeycloakClusteredSsoDeploymentProcessor.class);

    private static final String DEFAULT_CACHE_CONTAINER = "web";
    private static final String SSO_CACHE_CONTAINER_NAME_PARAM_NAME = "keycloak.sessionIdMapperUpdater.infinispan.containerName";
    private static final String SSO_CACHE_NAME_PARAM_NAME = "keycloak.sessionIdMapperUpdater.infinispan.cacheName";

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();

        if (isKeycloakSamlAuthMethod(deploymentUnit) && isDistributable(deploymentUnit)) {
            addSamlReplicationConfiguration(deploymentUnit, phaseContext);
        }
    }

    public static boolean isDistributable(final DeploymentUnit deploymentUnit) {
        WarMetaData warMetaData = deploymentUnit.getAttachment(WarMetaData.ATTACHMENT_KEY);
        if (warMetaData == null) {
            return false;
        }
        JBossWebMetaData webMetaData = warMetaData.getMergedJBossWebMetaData();
        if (webMetaData == null) {
            return false;
        }

        return webMetaData.getDistributable() != null || webMetaData.getReplicationConfig() != null;
    }

    public static boolean isKeycloakSamlAuthMethod(final DeploymentUnit deploymentUnit) {
        if (Configuration.INSTANCE.getSecureDeployment(deploymentUnit) != null) {
            return true;
        }

        WarMetaData warMetaData = deploymentUnit.getAttachment(WarMetaData.ATTACHMENT_KEY);
        if (warMetaData == null) {
            return false;
        }
        JBossWebMetaData webMetaData = warMetaData.getMergedJBossWebMetaData();
        if (webMetaData == null) {
            return false;
        }

        LoginConfigMetaData loginConfig = webMetaData.getLoginConfig();

        return loginConfig != null && Objects.equals(loginConfig.getAuthMethod(), "KEYCLOAK-SAML");
    }

    @Override
    public void undeploy(DeploymentUnit du) {
        
    }

    private void addSamlReplicationConfiguration(DeploymentUnit deploymentUnit, DeploymentPhaseContext context) {
        WarMetaData warMetaData = deploymentUnit.getAttachment(WarMetaData.ATTACHMENT_KEY);
        if (warMetaData == null) {
            return;
        }

        JBossWebMetaData webMetaData = warMetaData.getMergedJBossWebMetaData();
        if (webMetaData == null) {
            webMetaData = new JBossWebMetaData();
            warMetaData.setMergedJBossWebMetaData(webMetaData);
        }

        // Find out default names of cache container and cache
        String cacheContainer = DEFAULT_CACHE_CONTAINER;
        String deploymentSessionCacheName =
          (deploymentUnit.getParent() == null
              ? ""
              : deploymentUnit.getParent().getName() + ".")
          + deploymentUnit.getName();

        // Update names from jboss-web.xml's <replicationConfig>
        if (webMetaData.getReplicationConfig() != null && webMetaData.getReplicationConfig().getCacheName() != null) {
            ServiceName sn = ServiceName.parse(webMetaData.getReplicationConfig().getCacheName());
            cacheContainer = sn.getParent().getSimpleName();
            deploymentSessionCacheName = sn.getSimpleName();
        }
        String ssoCacheName = deploymentSessionCacheName + ".ssoCache";

        // Override if they were set in the context parameters
        List<ParamValueMetaData> contextParams = webMetaData.getContextParams();
        if (contextParams == null) {
            contextParams = new ArrayList<>();
        }
        for (ParamValueMetaData contextParam : contextParams) {
            if (Objects.equals(contextParam.getParamName(), SSO_CACHE_CONTAINER_NAME_PARAM_NAME)) {
                cacheContainer = contextParam.getParamValue();
            } else if (Objects.equals(contextParam.getParamName(), SSO_CACHE_NAME_PARAM_NAME)) {
                ssoCacheName = contextParam.getParamValue();
            }
        }

        LOG.debugv("Determined SSO cache container configuration: container: {0}, cache: {1}", cacheContainer, ssoCacheName);
        addCacheDependency(context, deploymentUnit, cacheContainer, ssoCacheName);

        // Set context parameters for SSO cache container/name
        ParamValueMetaData paramContainer = new ParamValueMetaData();
        paramContainer.setParamName(AdapterConstants.REPLICATION_CONFIG_CONTAINER_PARAM_NAME);
        paramContainer.setParamValue(cacheContainer);
        contextParams.add(paramContainer);

        ParamValueMetaData paramSsoCache = new ParamValueMetaData();
        paramSsoCache.setParamName(AdapterConstants.REPLICATION_CONFIG_SSO_CACHE_PARAM_NAME);
        paramSsoCache.setParamValue(ssoCacheName);
        contextParams.add(paramSsoCache);

        webMetaData.setContextParams(contextParams);
    }

    private void addCacheDependency(DeploymentPhaseContext context, DeploymentUnit deploymentUnit, String cacheContainer, String cacheName) {
        ServiceName wf10CacheContainerServiceName = ServiceName.of("jboss", "infinispan", cacheContainer);
        final ServiceController<?> wf10CacheContainerService = context.getServiceRegistry().getService(wf10CacheContainerServiceName);

        boolean legacy = wf10CacheContainerService != null;
        ServiceTarget st = context.getServiceTarget();

        if (legacy) {
            ServiceName cacheServiceName = wf10CacheContainerServiceName.append(cacheName);
            ServiceController<?> cacheService = context.getServiceRegistry().getService(cacheServiceName);
            if (cacheService != null) {
                st.addDependency(cacheServiceName);
            }
        } else {
            CapabilityServiceSupport support = deploymentUnit.getAttachment(Attachments.CAPABILITY_SERVICE_SUPPORT);

            ServiceName cacheServiceName = support.getCapabilityServiceName("org.wildfly.clustering.infinispan.cache." + cacheContainer + "." + cacheName);
            ServiceController<?> cacheService = context.getServiceRegistry().getService(cacheServiceName);
            if (cacheService != null) {
                st.addDependency(cacheServiceName);
            }
        }
    }

}
