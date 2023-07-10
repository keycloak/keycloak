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

import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.models.ClientSecretConstants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ComponentExportRepresentation;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class StripSecretsUtils {

    private static final Pattern VAULT_VALUE = Pattern.compile("^\\$\\{vault\\.(.+?)}$");

    private static final Map<Class<?>, BiConsumer<KeycloakSession, Object>> REPRESENTATION_FORMATTER = new HashMap<>();

    static {
        REPRESENTATION_FORMATTER.put(RealmRepresentation.class, (session, o) -> StripSecretsUtils.stripRealm(session, (RealmRepresentation) o));
        REPRESENTATION_FORMATTER.put(UserRepresentation.class, (session, o) -> StripSecretsUtils.stripUser((UserRepresentation) o));
        REPRESENTATION_FORMATTER.put(ClientRepresentation.class, (session, o) -> StripSecretsUtils.stripClient((ClientRepresentation) o));
        REPRESENTATION_FORMATTER.put(IdentityProviderRepresentation.class, (session, o) -> StripSecretsUtils.stripBroker((IdentityProviderRepresentation) o));
        REPRESENTATION_FORMATTER.put(ComponentRepresentation.class, (session, o) -> StripSecretsUtils.stripComponent(session, (ComponentRepresentation) o));
    }

    public static <T> T stripSecrets(KeycloakSession session, T representation) {
        BiConsumer<KeycloakSession, Object> formatter = REPRESENTATION_FORMATTER.get(representation.getClass());

        if (formatter == null) {
            return representation;
        }

        formatter.accept(session, representation);

        return representation;
    }

    private static String maskNonVaultValue(String value) {
        return value == null
          ? null
          : (VAULT_VALUE.matcher(value).matches()
             ? value
             : ComponentRepresentation.SECRET_VALUE
            );
    }

    private static ComponentRepresentation stripComponent(KeycloakSession session, ComponentRepresentation rep) {
        Map<String, ProviderConfigProperty> configProperties = ComponentUtil.getComponentConfigProperties(session, rep);
        if (rep.getConfig() == null) {
            return rep;
        }

        Iterator<Map.Entry<String, List<String>>> itr = rep.getConfig().entrySet().iterator();
        while (itr.hasNext()) {
            Map.Entry<String, List<String>> next = itr.next();
            ProviderConfigProperty configProperty = configProperties.get(next.getKey());
            if (configProperty != null) {
                if (configProperty.isSecret()) {
                    if (next.getValue() == null || next.getValue().isEmpty()) {
                        next.setValue(Collections.singletonList(ComponentRepresentation.SECRET_VALUE));
                    } else {
                        next.setValue(next.getValue().stream().map(StripSecretsUtils::maskNonVaultValue).collect(Collectors.toList()));
                    }
                }
            } else {
                itr.remove();
            }
        }
        return rep;
    }

    private static RealmRepresentation stripRealm(RealmRepresentation rep) {
        if (rep.getSmtpServer() != null && rep.getSmtpServer().containsKey("password")) {
            rep.getSmtpServer().put("password", maskNonVaultValue(rep.getSmtpServer().get("password")));
        }
        return rep;
    }

    private static IdentityProviderRepresentation stripBroker(IdentityProviderRepresentation rep) {
        if (rep.getConfig() != null && rep.getConfig().containsKey("clientSecret")) {
            rep.getConfig().put("clientSecret", maskNonVaultValue(rep.getConfig().get("clientSecret")));
        }
        return rep;
    }

    private static void stripRealm(KeycloakSession session, RealmRepresentation rep) {
        stripRealm(rep);

        List<ClientRepresentation> clients = rep.getClients();
        if (clients != null) {
            for (ClientRepresentation c : clients) {
                stripClient(c);
            }
        }
        List<IdentityProviderRepresentation> providers = rep.getIdentityProviders();
        if (providers != null) {
            for (IdentityProviderRepresentation r : providers) {
                stripBroker(r);
            }
        }

        MultivaluedHashMap<String, ComponentExportRepresentation> components = rep.getComponents();
        if (components != null) {
            for (Map.Entry<String, List<ComponentExportRepresentation>> ent : components.entrySet()) {
                for (ComponentExportRepresentation c : ent.getValue()) {
                    stripComponentExport(session, ent.getKey(), c);
                }
            }
        }

        List<UserRepresentation> users = rep.getUsers();
        if (users != null) {
            for (UserRepresentation u: users) {
                stripUser(u);
            }
        }

        users = rep.getFederatedUsers();
        if (users != null) {
            for (UserRepresentation u: users) {
                stripUser(u);
            }
        }
    }

    private static UserRepresentation stripUser(UserRepresentation user) {
        user.setCredentials(null);
        return user;
    }

    private static ClientRepresentation stripClient(ClientRepresentation rep) {
        if (rep.getSecret() != null) {
            rep.setSecret(maskNonVaultValue(rep.getSecret()));
        }
        if (rep.getAttributes() != null && rep.getAttributes().containsKey(ClientSecretConstants.CLIENT_ROTATED_SECRET)) {
            rep.getAttributes().put(
                    ClientSecretConstants.CLIENT_ROTATED_SECRET,
                    maskNonVaultValue(rep.getAttributes().get(ClientSecretConstants.CLIENT_ROTATED_SECRET))
            );
        }
        return rep;
    }

    private static ComponentExportRepresentation stripComponentExport(KeycloakSession session, String providerType, ComponentExportRepresentation rep) {
        Map<String, ProviderConfigProperty> configProperties = ComponentUtil.getComponentConfigProperties(session, providerType, rep.getProviderId());
        if (rep.getConfig() == null) {
            return rep;
        }

        Iterator<Map.Entry<String, List<String>>> itr = rep.getConfig().entrySet().iterator();
        while (itr.hasNext()) {
            Map.Entry<String, List<String>> next = itr.next();
            ProviderConfigProperty configProperty = configProperties.get(next.getKey());
            if (configProperty != null) {
                if (configProperty.isSecret()) {
                    if (next.getValue() == null || next.getValue().isEmpty()) {
                        next.setValue(Collections.singletonList(ComponentRepresentation.SECRET_VALUE));
                    } else {
                        next.setValue(next.getValue().stream().map(StripSecretsUtils::maskNonVaultValue).collect(Collectors.toList()));
                    }
                }
            } else {
                itr.remove();
            }
        }

        MultivaluedHashMap<String, ComponentExportRepresentation> sub = rep.getSubComponents();
        for (Map.Entry<String, List<ComponentExportRepresentation>> ent: sub.entrySet()) {
            for (ComponentExportRepresentation c: ent.getValue()) {
                stripComponentExport(session, ent.getKey(), c);
            }
        }
        return rep;
    }

}