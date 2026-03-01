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

package org.keycloak.testsuite.webauthn.pages;

import java.util.List;

import jakarta.ws.rs.core.UriBuilder;

import org.keycloak.testsuite.auth.page.AuthRealm;

/**
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public abstract class AbstractAccountPage extends AuthRealm {

    /**
     * Account Console is based on hash routing, e.g. [server_root]/auth/realms/test/account/#/password.
     * All page objects for Account Console need to specify their "hash path" by adding items to this property.
     */
    protected List<String> hashPath = null;

    public AbstractAccountPage() {
        setAuthRealm(TEST);
    }

    @Override
    public UriBuilder createUriBuilder() {
        String fragment = null;
        if (hashPath != null) {
            fragment = "/" + String.join("/", hashPath);
        }

        return super.createUriBuilder().path("account/").path(fragment);
    }
}
