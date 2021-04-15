/*
 *
 *  * Copyright 2021  Red Hat, Inc. and/or its affiliates
 *  * and other contributors as indicated by the @author tags.
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  * http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package org.keycloak.connections.cache;

import org.infinispan.manager.EmbeddedCacheManager;
import org.keycloak.connections.infinispan.DefaultInfinispanConnectionProviderFactory;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class QuarkusInfinispanConnectionFactory extends DefaultInfinispanConnectionProviderFactory {

    @Override
    protected void initContainerManaged(EmbeddedCacheManager cacheManager) {
        super.initContainerManaged(cacheManager);
        // force closing the cache manager when stopping the provider
        // we probably want to refactor the default impl a bit to support this use case
        containerManaged = false;
    }

    @Override
    public int order() {
        return 100;
    }

    @Override
    public String getId() {
        return "quarkus";
    }
}
