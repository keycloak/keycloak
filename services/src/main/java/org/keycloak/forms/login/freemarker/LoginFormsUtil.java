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

package org.keycloak.forms.login.freemarker;

import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.authenticators.broker.AbstractIdpAuthenticator;
import org.keycloak.authentication.authenticators.broker.util.SerializedBrokeredIdentityContext;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.sessions.AuthenticationSessionModel;

import javax.ws.rs.core.MultivaluedMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Various util methods, so the logic is not hardcoded in freemarker beans
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class LoginFormsUtil {

    // Display just those identityProviders on login screen, which are already linked to "known" established user
    public static List<IdentityProviderModel> filterIdentityProvidersByUser(List<IdentityProviderModel> providers, KeycloakSession session, RealmModel realm,
                                                                      Map<String, Object> attributes, MultivaluedMap<String, String> formData) {

        Boolean usernameEditDisabled = (Boolean) attributes.get(LoginFormsProvider.USERNAME_EDIT_DISABLED);
        if (usernameEditDisabled != null && usernameEditDisabled) {
            String username = formData.getFirst(UserModel.USERNAME);
            if (username == null) {
                throw new IllegalStateException("USERNAME_EDIT_DISABLED but username not known");
            }

            UserModel user = session.users().getUserByUsername(realm, username);
            if (user == null || !user.isEnabled()) {
                throw new IllegalStateException("User " + username + " not found or disabled");
            }

            Set<String> federatedIdentities = session.users().getFederatedIdentitiesStream(realm, user)
                    .map(federatedIdentityModel -> federatedIdentityModel.getIdentityProvider())
                    .collect(Collectors.toSet());

            List<IdentityProviderModel> result = new LinkedList<>();
            for (IdentityProviderModel idp : providers) {
                if (federatedIdentities.contains(idp.getAlias())) {
                    result.add(idp);
                }
            }
            return result;
        } else {
            return providers;
        }
    }

    public static List<IdentityProviderModel> filterIdentityProviders(Stream<IdentityProviderModel> providers, KeycloakSession session, AuthenticationFlowContext context) {

        if (context != null) {
            AuthenticationSessionModel authSession = context.getAuthenticationSession();
            SerializedBrokeredIdentityContext serializedCtx = SerializedBrokeredIdentityContext.readFromAuthenticationSession(authSession, AbstractIdpAuthenticator.BROKERED_CONTEXT_NOTE);

            if (serializedCtx != null) {
                IdentityProviderModel idp = serializedCtx.deserialize(session, authSession).getIdpConfig();
                return providers
                        .filter(p -> !Objects.equals(p.getAlias(), idp.getAlias()))
                        .collect(Collectors.toList());
            }
        }
        return providers.collect(Collectors.toList());
    }
}
