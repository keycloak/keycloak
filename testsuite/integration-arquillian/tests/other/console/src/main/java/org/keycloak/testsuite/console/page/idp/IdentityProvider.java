/*
 * Copyright 2019 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.testsuite.console.page.idp;

import org.jboss.arquillian.graphene.page.Page;

/**
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public class IdentityProvider extends IdentityProviders {
    public static final String PROVIDER_ID = "id";
    public static final String ALIAS = "alias";

    @Page
    private IdentityProviderForm form;

    @Override
    public String getUriFragment() {
        return super.getUriFragment() + "/provider/{" + PROVIDER_ID + "}/{" + ALIAS + "}";
    }

    public void setIds(String providerId, String alias) {
        setUriParameter(PROVIDER_ID, providerId);
        setUriParameter(ALIAS, alias);
    }

    public String getProviderId() {
        return (String) getUriParameter(PROVIDER_ID);
    }

    public String getAlias() {
        return (String) getUriParameter(ALIAS);
    }

    public IdentityProviderForm form() {
        return form;
    }
}
