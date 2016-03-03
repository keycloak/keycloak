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

import java.util.Map;

import org.keycloak.models.UserFederationProviderModel;
import org.keycloak.common.constants.KerberosConstants;

/**
 * Common configuration useful for all providers
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public abstract class CommonKerberosConfig {

    private final UserFederationProviderModel providerModel;

    public CommonKerberosConfig(UserFederationProviderModel userFederationProvider) {
        this.providerModel = userFederationProvider;
    }

    // Should be always true for KerberosFederationProvider
    public boolean isAllowKerberosAuthentication() {
        return Boolean.valueOf(getConfig().get(KerberosConstants.ALLOW_KERBEROS_AUTHENTICATION));
    }

    public String getKerberosRealm() {
        return getConfig().get(KerberosConstants.KERBEROS_REALM);
    }

    public String getServerPrincipal() {
        return getConfig().get(KerberosConstants.SERVER_PRINCIPAL);
    }

    public String getKeyTab() {
        return getConfig().get(KerberosConstants.KEYTAB);
    }

    public boolean isDebug() {
        return Boolean.valueOf(getConfig().get(KerberosConstants.DEBUG));
    }

    protected Map<String, String> getConfig() {
        return providerModel.getConfig();
    }

}
