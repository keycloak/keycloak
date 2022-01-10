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
package org.keycloak.validation;

import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.representations.oidc.OIDCClientRepresentation;

import javax.ws.rs.BadRequestException;

public class ValidationUtil {

    public static void validateClient(KeycloakSession session, ClientModel client, boolean create, ErrorHandler errorHandler) throws BadRequestException {
        validateClient(session, client, null, create, errorHandler);
    }

    public static void validateClient(KeycloakSession session, ClientModel client, OIDCClientRepresentation oidcClient, boolean create, ErrorHandler errorHandler) throws BadRequestException {
        ClientValidationProvider provider = session.getProvider(ClientValidationProvider.class);
        if (provider != null) {
            ValidationContext.Event event = create ? ValidationContext.Event.CREATE : ValidationContext.Event.UPDATE;
            ValidationResult result;

            if (oidcClient != null) {
                result = provider.validate(new ClientValidationContext.OIDCContext(event, session, client, oidcClient));
            }
            else {
                result = provider.validate(new ClientValidationContext(event, session, client));
            }

            if (!result.isValid()) {
                errorHandler.onError(result);
            }
        }
    }

    public interface ErrorHandler {

        void onError(ValidationResult context);

    }

}
