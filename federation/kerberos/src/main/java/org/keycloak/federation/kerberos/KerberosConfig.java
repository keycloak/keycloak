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
import org.keycloak.component.ComponentModel;
import org.keycloak.models.LDAPConstants;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.storage.UserStorageProvider.EditMode;

/**
 * Configuration specific to {@link KerberosFederationProvider}
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class KerberosConfig extends CommonKerberosConfig {

    public KerberosConfig(ComponentModel component) {
        super(component);
    }

    public KerberosConfig(ComponentRepresentation component) {
        super(component);
    }

    public EditMode getEditMode() {
        String editModeString = getConfig().getFirst(LDAPConstants.EDIT_MODE);
        if (editModeString == null) {
            return EditMode.UNSYNCED;
        } else {
            return EditMode.valueOf(editModeString);
        }
    }

    public boolean isAllowPasswordAuthentication() {
        return Boolean.valueOf(getConfig().getFirst(KerberosConstants.ALLOW_PASSWORD_AUTHENTICATION));
    }

    public boolean isUpdateProfileFirstLogin() {
        return Boolean.valueOf(getConfig().getFirst(KerberosConstants.UPDATE_PROFILE_FIRST_LOGIN));
    }

}
