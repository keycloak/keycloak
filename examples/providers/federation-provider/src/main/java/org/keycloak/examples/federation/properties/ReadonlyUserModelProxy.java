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

package org.keycloak.examples.federation.properties;

import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserCredentialValueModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.UserModelDelegate;

/**
 * Readonly proxy for a UserModel that prevents passwords from being updated.
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ReadonlyUserModelProxy extends UserModelDelegate {

    public ReadonlyUserModelProxy(UserModel delegate) {
        super(delegate);
    }

    @Override
    public void setUsername(String username) {
        throw new IllegalStateException("Username is readonly");
    }

    @Override
    public void updateCredentialDirectly(UserCredentialValueModel cred) {
        if (cred.getType().equals(UserCredentialModel.PASSWORD)) {
            throw new IllegalStateException("Passwords are readonly");
        }
        super.updateCredentialDirectly(cred);
    }

    @Override
    public void updateCredential(UserCredentialModel cred) {
        if (cred.getType().equals(UserCredentialModel.PASSWORD)) {
            throw new IllegalStateException("Passwords are readonly");
        }
        super.updateCredential(cred);
    }
}
