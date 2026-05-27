/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.services.clientpolicy.context;

import org.keycloak.models.ClientModel;
import org.keycloak.models.UserModel;
import org.keycloak.representations.JsonWebToken;
import org.keycloak.services.resources.admin.AdminAuth;

/**
 * Internal scaffolding for AdminAuth wiring shared by all protocol-mapper context classes.
 *
 * @see ClientProtocolMapperContext
 * @see ClientProtocolMapperRegisterContext
 * @see ClientProtocolMapperUpdateContext
 * @see ClientProtocolMapperRemoveContext
 */
abstract class AbstractClientProtocolMapperContext implements ClientProtocolMapperContext {

    protected final AdminAuth adminAuth;

    AbstractClientProtocolMapperContext(AdminAuth adminAuth) {
        this.adminAuth = adminAuth;
    }

    @Override
    public ClientModel getAuthenticatedClient() {
        return adminAuth.getClient();
    }

    @Override
    public UserModel getAuthenticatedUser() {
        return adminAuth.getUser();
    }

    @Override
    public JsonWebToken getToken() {
        return adminAuth.getToken();
    }
}
