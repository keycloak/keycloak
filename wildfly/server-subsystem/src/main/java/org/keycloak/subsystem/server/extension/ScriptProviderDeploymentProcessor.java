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
package org.keycloak.subsystem.server.extension;

import static org.keycloak.representations.provider.ScriptProviderDescriptor.AUTHENTICATORS;
import static org.keycloak.representations.provider.ScriptProviderDescriptor.MAPPERS;
import static org.keycloak.representations.provider.ScriptProviderDescriptor.POLICIES;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.module.ResourceRoot;
import org.jboss.vfs.VirtualFile;
import org.keycloak.authentication.AuthenticatorSpi;
import org.keycloak.authentication.authenticators.browser.DeployedScriptAuthenticatorFactory;
import org.keycloak.authorization.policy.provider.PolicySpi;
import org.keycloak.authorization.policy.provider.js.DeployedScriptPolicyFactory;
import org.keycloak.common.util.StreamUtil;
import org.keycloak.protocol.ProtocolMapperSpi;
import org.keycloak.protocol.oidc.mappers.DeployedScriptOIDCProtocolMapper;
import org.keycloak.provider.KeycloakDeploymentInfo;
import org.keycloak.representations.provider.ScriptProviderDescriptor;
import org.keycloak.representations.provider.ScriptProviderMetadata;
import org.keycloak.util.JsonSerialization;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
final class ScriptProviderDeploymentProcessor {

    private static final Map<String, BiConsumer<KeycloakDeploymentInfo, ScriptProviderMetadata>> PROVIDERS = new HashMap<>();

    private static void registerScriptAuthenticator(KeycloakDeploymentInfo info, ScriptProviderMetadata metadata) {
        info.addProvider(AuthenticatorSpi.class, new DeployedScriptAuthenticatorFactory(metadata));
    }

    private static void registerScriptPolicy(KeycloakDeploymentInfo info, ScriptProviderMetadata metadata) {
        info.addProvider(PolicySpi.class, new DeployedScriptPolicyFactory(metadata));
    }

    private static void registerScriptMapper(KeycloakDeploymentInfo info, ScriptProviderMetadata metadata) {
        info.addProvider(ProtocolMapperSpi.class, new DeployedScriptOIDCProtocolMapper(metadata));
    }

    static void deploy(DeploymentUnit deploymentUnit, KeycloakDeploymentInfo info) {
        ResourceRoot resourceRoot = deploymentUnit.getAttachment(Attachments.DEPLOYMENT_ROOT);

        if (resourceRoot == null) {
            return;
        }

        VirtualFile jarFile = resourceRoot.getRoot();

        if (jarFile == null || !jarFile.exists() || !jarFile.getName().endsWith(".jar")) {
            return;
        }

        ScriptProviderDescriptor descriptor = readScriptProviderDescriptor(jarFile);

        if (descriptor == null) {
            return;
        }

        for (Map.Entry<String, List<ScriptProviderMetadata>> entry : descriptor.getProviders().entrySet()) {
            for (ScriptProviderMetadata metadata : entry.getValue()) {
                String fileName = metadata.getFileName();

                if (fileName == null) {
                    throw new RuntimeException("You must provide the script file name");
                }

                try (InputStream in = jarFile.getChild(fileName).openStream()) {
                    metadata.setCode(StreamUtil.readString(in, StandardCharsets.UTF_8));
                } catch (IOException cause) {
                    throw new RuntimeException("Failed to read script file [" + fileName + "]", cause);
                }

                metadata.setId(new StringBuilder("script").append("-").append(fileName).toString());

                String name = metadata.getName();

                if (name == null) {
                    name = fileName;
                }

                metadata.setName(name);

                PROVIDERS.get(entry.getKey()).accept(info, metadata);
            }
        }
    }

    private static ScriptProviderDescriptor readScriptProviderDescriptor(VirtualFile deploymentRoot) {
        VirtualFile metadataFile = deploymentRoot.getChild("META-INF/keycloak-scripts.json");

        if (!metadataFile.exists()) {
            return null;
        }

        try (InputStream inputStream = metadataFile.openStream()) {
            return JsonSerialization.readValue(inputStream, ScriptProviderDescriptor.class);
        } catch (IOException cause) {
            throw new RuntimeException("Failed to read providers metadata", cause);
        }
    }

    static {
        PROVIDERS.put(AUTHENTICATORS, ScriptProviderDeploymentProcessor::registerScriptAuthenticator);
        PROVIDERS.put(POLICIES, ScriptProviderDeploymentProcessor::registerScriptPolicy);
        PROVIDERS.put(MAPPERS, ScriptProviderDeploymentProcessor::registerScriptMapper);
    }
}
