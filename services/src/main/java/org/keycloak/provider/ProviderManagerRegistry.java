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
package org.keycloak.provider;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ProviderManagerRegistry {
    public static final ProviderManagerRegistry SINGLETON = new ProviderManagerRegistry();
    protected List<ProviderManager> preBoot = Collections.synchronizedList(new LinkedList<>());
    protected AtomicReference<ProviderManagerDeployer> deployerRef = new AtomicReference<>();

    public synchronized void setDeployer(ProviderManagerDeployer deployer) {
        this.deployerRef.set(deployer);
    }

    public synchronized void deploy(ProviderManager pm) {
        ProviderManagerDeployer deployer = getDeployer();
        if (deployer == null) {
            preBoot.add(pm);
        } else {
            deployer.deploy(pm);
        }

    }

    public synchronized void undeploy(ProviderManager pm) {
        preBoot.remove(pm);
        ProviderManagerDeployer deployer = getDeployer();
        if (deployer != null) {
            deployer.undeploy(pm);
        }
    }

    private ProviderManagerDeployer getDeployer() {
        return deployerRef.get();
    }

    public List<ProviderManager> getPreBoot() {
        return preBoot;
    }
}
