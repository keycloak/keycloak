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
import org.keycloak.testsuite.console.page.AdminConsoleCreate;

/**
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public class CreateIdentityProvider extends AdminConsoleCreate {
    public static final String PROVIDER_ID = "provider-id";

    @Page
    private IdentityProviderForm form;

    public CreateIdentityProvider() {
        setEntity("identity-provider");
    }

    @Override
    public String getUriFragment() {
        return super.getUriFragment() + "/{" + PROVIDER_ID + "}";
    }

    public void setProviderId(String id) {
        setUriParameter(PROVIDER_ID, id);
    }

    public String getProviderId() {
        return (String) getUriParameter(PROVIDER_ID);
    }

    public IdentityProviderForm form() {
        return form;
    }
}
