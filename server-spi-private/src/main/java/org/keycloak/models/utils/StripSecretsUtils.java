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

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.models.ClientSecretConstants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ComponentExportRepresentation;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class StripSecretsUtils {

    private static final Pattern VAULT_VALUE = Pattern.compile("^\\$\\{vault\\.(.+?)}$");

    private static final Map<Class<?>, BiConsumer<KeycloakSession, Object>> REPRESENTATION_FORMATTER = new HashMap<>();

    /** interface to encapsulate the getComponentProperties() function in order to make the code unit-testable
     */
    protected interface GetComponentPropertiesFn {
        Map<String, ProviderConfigProperty> getComponentProperties(KeycloakSession session, String providerType, String providerId);
    }

    static {
        REPRESENTATION_FORMATTER.put(RealmRepresentation.class, (session, o) -> StripSecretsUtils.stripRealm(session, (RealmRepresentation) o));
        REPRESENTATION_FORMATTER.put(UserRepresentation.class, (session, o) -> StripSecretsUtils.stripUser((UserRepresentation) o));
        REPRESENTATION_FORMATTER.put(ClientRepresentation.class, (session, o) -> StripSecretsUtils.stripClient((ClientRepresentation) o));
        REPRESENTATION_FORMATTER.put(IdentityProviderRepresentation.class, (session, o) -> StripSecretsUtils.stripBroker((IdentityProviderRepresentation) o));
        REPRESENTATION_FORMATTER.put(ComponentRepresentation.class, (session, o) -> StripSecretsUtils.stripComponent(session, (ComponentRepresentation) o));
        REPRESENTATION_FORMATTER.put(CredentialRepresentation.class, (session, o) -> StripSecretsUtils.stripCredentials((CredentialRepresentation) o));
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

    protected static CredentialRepresentation stripCredentials(CredentialRepresentation rep) {
        rep.setValue("**********");
        return rep;
    }

    private static ComponentRepresentation stripComponent(KeycloakSession session, ComponentRepresentation rep) {
        Map<String, ProviderConfigProperty> configProperties = ComponentUtil.getComponentConfigProperties(session, rep);
        return stripComponent(configProperties, rep);
    }

    protected static ComponentRepresentation stripComponent( Map<String, ProviderConfigProperty> configProperties, ComponentRepresentation rep) {
        if (rep.getConfig() != null) {
            stripComponentConfigMap(rep.getConfig(), configProperties);
        }
        return rep;

    }
    private static void stripComponentConfigMap(MultivaluedHashMap<String, String> configMap, Map<String, ProviderConfigProperty> configProperties) {
        Iterator<Map.Entry<String, List<String>>> itr = configMap.entrySet().iterator();
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
    }

    private static Map<String, String> stripFromMap(Map<String, String> map, String key) {
        if ((map != null) && map.containsKey(key)) {
            map.put(key, maskNonVaultValue(map.get(key)));
        }
        return map;
    }

    protected static IdentityProviderRepresentation stripBroker(IdentityProviderRepresentation rep) {
        stripFromMap(rep.getConfig(), "clientSecret");
        return rep;
    }

    private static RealmRepresentation stripRealm(RealmRepresentation rep) {
        stripFromMap(rep.getSmtpServer(), "password");
        stripFromMap(rep.getSmtpServer(), "authTokenClientSecret");
        return rep;
    }

    private static void stripRealm(KeycloakSession session, RealmRepresentation rep) {
        stripRealm(session, rep, ComponentUtil::getComponentConfigProperties);
    }
    protected static void stripRealm(KeycloakSession session, RealmRepresentation rep, GetComponentPropertiesFn fnGetConfigProperties) {
        stripRealm(rep);

        Optional.ofNullable(rep.getClients())
                .ifPresent(clients -> clients.forEach(StripSecretsUtils::stripClient));

        Optional.ofNullable(rep.getIdentityProviders())
                .ifPresent(providers -> providers.forEach(StripSecretsUtils::stripBroker));

        Optional.ofNullable(rep.getComponents())
                .ifPresent(components -> components
                        .forEach((providerType, componentList)-> componentList
                                .forEach(component -> stripComponentExport(session, providerType, component, fnGetConfigProperties))));

        Optional.ofNullable(rep.getUsers())
                .ifPresent(users -> users.forEach(StripSecretsUtils::stripUser));

        Optional.ofNullable(rep.getFederatedUsers())
                .ifPresent(users -> users.forEach(StripSecretsUtils::stripUser));
    }

    protected static UserRepresentation stripUser(UserRepresentation user) {
        user.setCredentials(null);
        return user;
    }

    protected static ClientRepresentation stripClient(ClientRepresentation rep) {
        if (rep.getSecret() != null) {
            rep.setSecret(maskNonVaultValue(rep.getSecret()));
        }

        stripFromMap(rep.getAttributes(), ClientSecretConstants.CLIENT_ROTATED_SECRET);
        return rep;
    }

    private static ComponentExportRepresentation stripComponentExport(KeycloakSession session, String providerType, ComponentExportRepresentation rep) {
        return stripComponentExport(session, providerType, rep, ComponentUtil::getComponentConfigProperties);
    }
    private static ComponentExportRepresentation stripComponentExport(KeycloakSession session, String providerType, ComponentExportRepresentation rep, GetComponentPropertiesFn fnGetConfigProperties) {
        Map<String, ProviderConfigProperty> configProperties = fnGetConfigProperties.getComponentProperties(session, providerType, rep.getProviderId());

        if (rep.getConfig() != null) {
            stripComponentConfigMap(rep.getConfig(), configProperties);
        }

        rep.getSubComponents()
                    .forEach((subCompProviderType, subCompProviders) ->
                            subCompProviders.forEach(subComp -> stripComponentExport(session, subCompProviderType, subComp)));
        return rep;
    }

}
