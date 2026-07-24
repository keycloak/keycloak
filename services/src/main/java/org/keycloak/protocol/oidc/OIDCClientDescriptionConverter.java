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

package org.keycloak.protocol.oidc;

import java.io.IOException;

import org.keycloak.exportimport.ClientDescriptionConverter;
import org.keycloak.models.KeycloakSession;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.oidc.OIDCClientRepresentation;
import org.keycloak.services.clientregistration.oidc.DescriptionConverter;
import org.keycloak.util.JsonSerialization;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class OIDCClientDescriptionConverter implements ClientDescriptionConverter {

    private final KeycloakSession session;

    public OIDCClientDescriptionConverter(KeycloakSession session) {
        this.session = session;
    }


    @Override
    public ClientRepresentation convertToInternal(String description) {
        try {
            OIDCClientRepresentation clientOIDC = JsonSerialization.readValue(description, OIDCClientRepresentation.class);
            return DescriptionConverter.toInternal(session, clientOIDC);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void close() {
    }


}
