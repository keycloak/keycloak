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
package org.keycloak.models.map.storage.jpa.hibernate;

import org.keycloak.common.Profile;
import org.keycloak.models.map.storage.jpa.PersistenceExceptionConverter;
import org.keycloak.provider.EnvironmentDependentProviderFactory;
import org.keycloak.provider.ExceptionConverter;

import javax.persistence.PersistenceException;

/**
 * This is needed for example by <code>org.keycloak.transaction.JtaTransactionWrapper</code> to map an exception
 * that occurs on commit.
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @author Alexander Schwartz
 */
public class JpaMapExceptionConverter implements ExceptionConverter, EnvironmentDependentProviderFactory {
    @Override
    public Throwable convert(Throwable e) {
        if (!(e instanceof PersistenceException)) return null;
        return PersistenceExceptionConverter.convert(e.getCause() != null ? e.getCause() : e);
    }

    @Override
    public String getId() {
        return "jpa-map";
    }

    @Override
    public boolean isSupported() {
        return Profile.isFeatureEnabled(Profile.Feature.MAP_STORAGE);
    }
}
