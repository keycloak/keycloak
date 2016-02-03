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

package org.keycloak.federation.kerberos;

import org.keycloak.common.constants.KerberosConstants;
import org.keycloak.models.LDAPConstants;
import org.keycloak.models.UserFederationProvider;
import org.keycloak.models.UserFederationProviderModel;

/**
 * Configuration specific to {@link KerberosFederationProvider}
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class KerberosConfig extends CommonKerberosConfig {

    public KerberosConfig(UserFederationProviderModel userFederationProvider) {
        super(userFederationProvider);
    }

    public UserFederationProvider.EditMode getEditMode() {
        String editModeString = getConfig().get(LDAPConstants.EDIT_MODE);
        if (editModeString == null) {
            return UserFederationProvider.EditMode.UNSYNCED;
        } else {
            return UserFederationProvider.EditMode.valueOf(editModeString);
        }
    }

    public boolean isAllowPasswordAuthentication() {
        return Boolean.valueOf(getConfig().get(KerberosConstants.ALLOW_PASSWORD_AUTHENTICATION));
    }

    public boolean isUpdateProfileFirstLogin() {
        return Boolean.valueOf(getConfig().get(KerberosConstants.UPDATE_PROFILE_FIRST_LOGIN));
    }

}
