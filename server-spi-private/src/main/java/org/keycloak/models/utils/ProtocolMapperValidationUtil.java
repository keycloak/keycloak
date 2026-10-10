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
package org.keycloak.models.utils;

import java.util.Set;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ProtocolMapperContainerModel;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.ProtocolMapper;
import org.keycloak.protocol.ProtocolMapperConfigException;

/**
 * Validates the session note mappers of a protocol mapper container on the paths that write a whole container at
 * once, such as a client scope created inline or a realm import, which do not go through the per-mapper admin
 * endpoint and so never reach {@link ProtocolMapper#validateConfig}.
 */
public class ProtocolMapperValidationUtil {

    /**
     * Only the session note mappers are validated here, by provider id, rather than every mapper of the container.
     * A container is written before the rest of the realm exists: a realm import creates the client scopes before
     * the clients, so a mapper whose {@link ProtocolMapper#validateConfig} resolves another entity would reject a
     * reference the import is about to create. The OID4VC target role mapper rejects a client id that does not
     * exist yet, which would make a valid export unimportable.
     */
    private static final Set<String> SESSION_NOTE_MAPPERS = Set.of(
            "oidc-usersessionmodel-note-mapper",     // UserSessionNoteMapper.PROVIDER_ID
            "saml-user-session-note-mapper");        // UserSessionNoteStatementMapper.PROVIDER_ID

    private ProtocolMapperValidationUtil() {
        // utility class
    }

    public static boolean isSessionNoteMapper(ProtocolMapperModel model) {
        return SESSION_NOTE_MAPPERS.contains(model.getProtocolMapper());
    }

    public static void validateProtocolMappers(KeycloakSession session, RealmModel realm, ProtocolMapperContainerModel container) throws ProtocolMapperConfigException {
        for (ProtocolMapperModel model : container.getProtocolMappersStream().toList()) {
            if (!isSessionNoteMapper(model)) {
                continue;
            }
            ProtocolMapper mapper = (ProtocolMapper) session.getKeycloakSessionFactory()
                    .getProviderFactory(ProtocolMapper.class, model.getProtocolMapper());
            // unlike ProtocolMappersResource, tolerate an unknown provider: an import must not
            // hard-fail just because a third-party mapper JAR is absent
            if (mapper != null) {
                mapper.validateConfig(session, realm, container, model);
            }
        }
    }
}
