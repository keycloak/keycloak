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

package org.keycloak.federation.ldap;

import org.jboss.logging.Logger;
import org.keycloak.federation.ldap.idm.model.LDAPObject;
import org.keycloak.federation.ldap.idm.store.ldap.LDAPIdentityStore;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.UserModelDelegate;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class WritableLDAPUserModelDelegate extends UserModelDelegate implements UserModel {
    private static final Logger logger = Logger.getLogger(WritableLDAPUserModelDelegate.class);

    protected LDAPFederationProvider provider;
    protected LDAPObject ldapObject;

    public WritableLDAPUserModelDelegate(UserModel delegate, LDAPFederationProvider provider, LDAPObject ldapObject) {
        super(delegate);
        this.provider = provider;
        this.ldapObject = ldapObject;
    }

    @Override
    public void updateCredential(UserCredentialModel cred) {
        if (!provider.getSupportedCredentialTypes(delegate).contains(cred.getType())) {
            delegate.updateCredential(cred);
            return;
        }

        if (cred.getType().equals(UserCredentialModel.PASSWORD)) {
            LDAPIdentityStore ldapIdentityStore = provider.getLdapIdentityStore();
            String password = cred.getValue();
            ldapIdentityStore.updatePassword(ldapObject, password);
        } else {
            logger.warnf("Don't know how to update credential of type [%s] for user [%s]", cred.getType(), delegate.getUsername());
        }
    }

}
