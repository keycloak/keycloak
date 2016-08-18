/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.authorization.mongo.store;

import org.keycloak.authorization.AuthorizationProvider;
import org.keycloak.authorization.store.PolicyStore;
import org.keycloak.authorization.store.ResourceServerStore;
import org.keycloak.authorization.store.ResourceStore;
import org.keycloak.authorization.store.ScopeStore;
import org.keycloak.authorization.store.StoreFactory;
import org.keycloak.connections.mongo.api.context.MongoStoreInvocationContext;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class MongoStoreFactory implements StoreFactory {

    private final MongoStoreInvocationContext invocationContext;
    private final AuthorizationProvider authorizationProvider;

    public MongoStoreFactory(MongoStoreInvocationContext invocationContext, AuthorizationProvider authorizationProvider) {
        this.invocationContext = invocationContext;
        this.authorizationProvider = authorizationProvider;
    }

    @Override
    public PolicyStore getPolicyStore() {
        return new MongoPolicyStore(this.invocationContext, this.authorizationProvider);
    }

    @Override
    public ResourceServerStore getResourceServerStore() {
        return new MongoResourceServerStore(this.invocationContext, this.authorizationProvider);
    }

    @Override
    public ResourceStore getResourceStore() {
        return new MongoResourceStore(this.invocationContext, this.authorizationProvider);
    }

    @Override
    public ScopeStore getScopeStore() {
        return new MongoScopeStore(this.invocationContext, this.authorizationProvider);
    }

    @Override
    public void close() {

    }
}
