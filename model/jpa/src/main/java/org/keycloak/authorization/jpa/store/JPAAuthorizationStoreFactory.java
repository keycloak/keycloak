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

package org.keycloak.authorization.jpa.store;

import javax.persistence.EntityManager;

import org.keycloak.Config;
import org.keycloak.authorization.AuthorizationProvider;
import org.keycloak.authorization.store.AuthorizationStoreFactory;
import org.keycloak.authorization.store.StoreFactory;
import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;

import static org.keycloak.models.jpa.JpaRealmProviderFactory.PROVIDER_PRIORITY;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class JPAAuthorizationStoreFactory implements AuthorizationStoreFactory {

    /**
     * Legacy store doesn't store realm id for any entity and no method there is using new introduced RealmModel parameter.
     * The parameter was introduced for usage only in the new storage. Therefore, in some cases we may break our rule specified in JavaDoc
     * and use {@code null} value as parameter that otherwise cannot be {@code null}. We need to be careful and place such value only to a method call
     * that cannot end up in the new store because it would end with {@link NullPointerException}. To mark all places where we do this,
     * we use this variable so it is easily searchable.
     */
    public static final RealmModel NULL_REALM = null;

    @Override
    public StoreFactory create(KeycloakSession session) {
        AuthorizationProvider provider = session.getProvider(AuthorizationProvider.class);
        return new JPAStoreFactory(getEntityManager(session), provider);
    }

    @Override
    public void init(Config.Scope config) {

    }

    @Override
    public void close() {

    }

    @Override
    public String getId() {
        return "jpa";
    }

    private EntityManager getEntityManager(KeycloakSession session) {
        return session.getProvider(JpaConnectionProvider.class).getEntityManager();
    }

    @Override
    public int order() {
        return PROVIDER_PRIORITY;
    }
}
