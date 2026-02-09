/*
 * Copyright 2016 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @author tags. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.keycloak.testsuite.util;

import java.util.Arrays;
import java.util.List;

import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;

import static org.keycloak.models.utils.KeycloakModelUtils.getDefaultClientAuthenticatorType;

/**
 * This is a client-side version of KeycloakModelUtils that uses the adminClient to
 * manipulate the model.
 *
 * @author Stan Silvert ssilvert@redhat.com (C) 2016 Red Hat Inc.
 */
public class KeycloakModelUtils {

    public static ClientRepresentation createClient(RealmRepresentation realm, String name) {
        ClientRepresentation app = new ClientRepresentation();
        app.setName(name);
        app.setClientId(name);
        List<ClientRepresentation> clients = realm.getClients();
        if (clients != null) {
            clients.add(app);
        } else {
            realm.setClients(Arrays.asList(app));
        }
        app.setClientAuthenticatorType(getDefaultClientAuthenticatorType());
        generateSecret(app);
        app.setFullScopeAllowed(true);

        return app;
    }

    public static CredentialRepresentation generateSecret(ClientRepresentation client) {
        UserCredentialModel secret = UserCredentialModel.generateSecret();
        client.setSecret(secret.getChallengeResponse());
        return ModelToRepresentation.toRepresentation(secret);
    }
}
