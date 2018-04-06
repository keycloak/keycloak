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

package org.keycloak.federation.sssd;

import org.keycloak.models.UserModel;
import org.keycloak.models.utils.UserModelDelegate;
import org.keycloak.storage.ReadOnlyException;

/**
 * Readonly proxy for a SSSD UserModel that prevents attributes from being updated.
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @author <a href="mailto:bruno@abstractj.org">Bruno Oliveira</a>
 * @version $Revision: 1 $
 */
public class ReadonlySSSDUserModelDelegate extends UserModelDelegate implements UserModel {

    private final SSSDFederationProvider provider;

    public ReadonlySSSDUserModelDelegate(UserModel delegate, SSSDFederationProvider provider) {
        super(delegate);
        this.provider = provider;
    }

    @Override
    public void setUsername(String username) {
        throw new ReadOnlyException("Federated storage is not writable");
    }

    @Override
    public void setLastName(String lastName) {
        throw new ReadOnlyException("Federated storage is not writable");
    }

    @Override
    public void setFirstName(String first) {
        throw new ReadOnlyException("Federated storage is not writable");
    }

    @Override
    public void setEmail(String email) {
        throw new ReadOnlyException("Federated storage is not writable");
    }
}
