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

package org.keycloak.testsuite.util;

import java.util.ArrayList;
import java.util.List;

import org.keycloak.testsuite.arquillian.ContainerInfo;
import org.keycloak.testsuite.arquillian.SuiteContext;
import org.keycloak.testsuite.arquillian.annotation.EnableVault;
import org.keycloak.testsuite.arquillian.containers.AbstractQuarkusDeployableContainer;

/**
 * @author mhajas
 */
public class VaultUtils {

    public static void enableVault(SuiteContext suiteContext, EnableVault.PROVIDER_ID provider) {
        ContainerInfo serverInfo = suiteContext.getAuthServerInfo();

        if (serverInfo.isUndertow()) {
            System.setProperty("keycloak.vault." + provider.getName() + ".provider.enabled", "true");
        }
        else if (serverInfo.isQuarkus()) {
            AbstractQuarkusDeployableContainer container = (AbstractQuarkusDeployableContainer)suiteContext.getAuthServerInfo().getArquillianContainer().getDeployableContainer();
            List<String> additionalArgs = new ArrayList<>();

            if (provider == EnableVault.PROVIDER_ID.KEYSTORE) {
                additionalArgs.add("--vault=keystore");
                additionalArgs.add("--vault-file=../secrets/myks");
                additionalArgs.add("--vault-pass=keystorepassword");
            } else if (provider == EnableVault.PROVIDER_ID.PLAINTEXT) {
                additionalArgs.add("--vault=file");
                additionalArgs.add("--vault-dir=../secrets");
            }
            container.setAdditionalBuildArgs(additionalArgs);
        }
    }

    public static void disableVault(SuiteContext suiteContext, EnableVault.PROVIDER_ID provider) {
        ContainerInfo serverInfo = suiteContext.getAuthServerInfo();

        if (serverInfo.isUndertow()) {
            System.setProperty("keycloak.vault." + provider.getName() + ".provider.enabled", "false");
        }
    }

}
