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

package org.keycloak.connections.jpa.updater.liquibase.custom;

import liquibase.exception.CustomChangeException;
import org.keycloak.models.LDAPConstants;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.storage.UserStorageProvider;

import java.util.function.Predicate;

/**
 * @author <a href="mailto:bburke@redhat.com">Bill Burke</a>
 */
public class MigrateUserFedToComponent extends AbstractUserFedToComponent {

    @Override
    protected void generateStatementsImpl() {
        kcSession.getKeycloakSessionFactory().getProviderFactoriesStream(UserStorageProvider.class)
                .map(ProviderFactory::getId)
                .filter(Predicate.isEqual(LDAPConstants.LDAP_PROVIDER).negate())
                .forEach(this::convertFedProviderToComponent);
    }

    @Override
    protected String getTaskId() {
        return "Update 2.5.0.Final";
    }

    private void convertFedProviderToComponent(String id) {
        try {
            convertFedProviderToComponent(id, null);
        } catch (CustomChangeException ex) {
            throw new RuntimeException(ex);
        }
    }
}
