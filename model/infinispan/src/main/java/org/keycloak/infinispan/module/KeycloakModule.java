/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.infinispan.module;

import org.infinispan.factories.GlobalComponentRegistry;
import org.infinispan.factories.annotations.InfinispanModule;
import org.infinispan.factories.impl.BasicComponentRegistry;
import org.infinispan.lifecycle.ModuleLifecycle;
import org.keycloak.infinispan.module.certificates.CertificateReloadManager;

@InfinispanModule(name = "keycloak", requiredModules = {"core"})
public class KeycloakModule implements ModuleLifecycle {

    @Override
    public void cacheManagerStarted(GlobalComponentRegistry gcr) {
        // start certificate reload manager if needed
        CertificateReloadManager crm = gcr.getComponent(BasicComponentRegistry.class)
                .getComponent(CertificateReloadManager.class)
                .running();
        gcr.getCacheManager().addListener(crm);
    }
}
