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
 *
 */

package org.keycloak.services.clientpolicy.executor;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.keycloak.events.Errors;
import org.keycloak.models.ClientModel;
import org.keycloak.protocol.oidc.OIDCConfigAttributes;
import org.keycloak.representations.idm.ClientPolicyExecutorConfigurationRepresentation;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.services.clientpolicy.ClientPolicyContext;
import org.keycloak.services.clientpolicy.ClientPolicyException;
import org.keycloak.services.clientpolicy.context.ClientCRUDContext;

/**
 * Check that the JWT authorization grant settings of a client are not set or changed by the request.
 *
 * <p>The identity provider allow list and the token audience allow list decide which assertions the
 * JWT authorization grant accepts for a client, so they are trust decisions rather than ordinary
 * client metadata. This executor lets a deployment keep those decisions away from whoever the policy
 * applies to. Combined with the <code>client-updater-context</code> condition it can be limited to
 * requests that the client itself makes, leaving administrators free to configure the client.</p>
 *
 * <p>Only a change is rejected. A request that carries the value the client already has is allowed,
 * so an existing configuration is preserved and a read-modify-write of the whole client keeps
 * working.</p>
 *
 * @author <a href="mailto:mahdi.a.alhakim@gmail.com">Mahdi Alhakim</a>
 */
public class JWTAuthorizationGrantSettingsDisabledExecutor implements ClientPolicyExecutorProvider<ClientPolicyExecutorConfigurationRepresentation> {

    private static final Set<String> PROTECTED_ATTRIBUTES = Set.of(
            OIDCConfigAttributes.JWT_AUTHORIZATION_GRANT_IDP,
            OIDCConfigAttributes.JWT_AUTHORIZATION_GRANT_AUDIENCE);

    @Override
    public String getProviderId() {
        return JWTAuthorizationGrantSettingsDisabledExecutorFactory.PROVIDER_ID;
    }

    @Override
    public void executeOnEvent(ClientPolicyContext context) throws ClientPolicyException {
        switch (context.getEvent()) {
            case REGISTER:
            case UPDATE:
                ClientCRUDContext clientContext = (ClientCRUDContext) context;
                validate(clientContext.getProposedClientRepresentation(), clientContext.getTargetClient());
                break;
            default:
                return;
        }
    }

    private void validate(ClientRepresentation proposedClient, ClientModel existingClient) throws ClientPolicyException {
        if (proposedClient == null) {
            return;
        }

        Map<String, String> attributes = proposedClient.getAttributes();
        if (attributes == null) {
            return;
        }

        for (String attribute : PROTECTED_ATTRIBUTES) {
            if (!attributes.containsKey(attribute)) {
                continue;
            }

            String proposed = attributes.get(attribute);
            // On REGISTER there is no existing client, so any value is a new setting.
            String current = existingClient == null ? null : existingClient.getAttribute(attribute);

            // A value that storage treats as undefined and an absent attribute are the same thing, so
            // clearing an attribute that is not set is not a change. The test has to match storage,
            // which treats a value as undefined with String.trim (see ClientAdapter.setAttribute).
            // Character.isWhitespace, used by ObjectUtil.isBlank, instead counts characters such as the
            // em space U+2003 as blank, and trim keeps those, so reading such a value as a clear here
            // would let storage keep it and set the attribute this executor is meant to lock.
            if (isUndefined(proposed) && isUndefined(current)) {
                continue;
            }

            // Deliberately an exact comparison of the stored form. Both attributes are serialized,
            // the identity provider list with a separator and the audience map as JSON, so a
            // re-serialization that means the same thing is still rejected. That errs towards
            // refusing a request rather than letting a change through.
            if (!Objects.equals(proposed, current)) {
                throw new ClientPolicyException(Errors.INVALID_REGISTRATION,
                        "Not permitted to set, change or remove the client attribute " + attribute);
            }
        }

        validateGrantNotEnabled(attributes, existingClient);
    }

    /**
     * The enabled flag is not one of the protected attributes, because {@code DescriptionConverter}
     * writes it from the standard {@code grant_types} field of an OpenID Connect registration
     * request, so rejecting every write of it would reject a spec compliant registration. Turning it
     * on for a client that already exists is rejected all the same: the allow lists an administrator
     * configured earlier are still on the client, so the flag on its own is enough to make the grant
     * work again. Turning it off stays allowed.
     */
    private void validateGrantNotEnabled(Map<String, String> attributes, ClientModel existingClient) throws ClientPolicyException {
        if (existingClient == null) {
            // A REGISTER that gets this far carries no allow list, the loop above rejects one,
            // so the flag on its own grants nothing.
            return;
        }

        String attribute = OIDCConfigAttributes.JWT_AUTHORIZATION_GRANT_ENABLED;
        if (!Boolean.parseBoolean(attributes.get(attribute))) {
            // Absent, or the grant is being turned off.
            return;
        }

        if (Boolean.parseBoolean(existingClient.getAttribute(attribute))) {
            // Already enabled, so this is not a change.
            return;
        }

        throw new ClientPolicyException(Errors.INVALID_REGISTRATION,
                "Not permitted to enable the JWT authorization grant on an existing client, attribute " + attribute);
    }

    /**
     * Whether storage keeps the value, matching {@code ClientAdapter.setAttribute}. A value is kept
     * unless it is null or empty once {@link String#trim()} has run, so this is deliberately not
     * {@code ObjectUtil.isBlank}: that treats whitespace such as the em space U+2003 as blank while
     * {@code trim} does not strip it, which would let such a value through the check and into storage.
     */
    private static boolean isUndefined(String value) {
        return value == null || value.trim().isEmpty();
    }
}
