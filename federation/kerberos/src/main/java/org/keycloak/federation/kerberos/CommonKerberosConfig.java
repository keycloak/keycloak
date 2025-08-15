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
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.component.ComponentModel;
import org.keycloak.representations.idm.ComponentRepresentation;

/**
 * Common configuration useful for all providers
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public abstract class CommonKerberosConfig {

    protected MultivaluedHashMap<String, String> userStorageConfig;

    public CommonKerberosConfig(ComponentModel componentModel) {
        this.userStorageConfig = componentModel.getConfig();
    }

    public CommonKerberosConfig(ComponentRepresentation componentRep) {
        this.userStorageConfig = componentRep.getConfig();
    }

    protected MultivaluedHashMap<String, String> getConfig() {
        return userStorageConfig;
    }

    // Should be always true for KerberosFederationProvider
    public boolean isAllowKerberosAuthentication() {
        return Boolean.valueOf(getConfig().getFirst(KerberosConstants.ALLOW_KERBEROS_AUTHENTICATION));
    }

    public String getKerberosRealm() {
        return getConfig().getFirst(KerberosConstants.KERBEROS_REALM);
    }

    public String getServerPrincipal() {
        return getConfig().getFirst(KerberosConstants.SERVER_PRINCIPAL);
    }

    public String getKeyTab() {
        return getConfig().getFirst(KerberosConstants.KEYTAB);
    }

    public boolean isDebug() {
        return Boolean.valueOf(getConfig().getFirst(KerberosConstants.DEBUG));
    }


}
