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

package org.keycloak.protocol;

import java.lang.reflect.Method;
import java.util.AbstractMap;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.keycloak.models.ClientSessionContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.UserModel;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.OIDCLoginProtocolFactory;
import org.keycloak.services.util.DPoPUtil;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ProtocolMapperUtils {

    public static final String USER_ROLE = "user.role";
    public static final String USER_ATTRIBUTE = "user.attribute";
    public static final String USER_SESSION_NOTE = "user.session.note";
    public static final String MULTIVALUED = "multivalued";
    public static final String AGGREGATE_ATTRS = "aggregate.attrs";
    public static final String USER_MODEL_PROPERTY_LABEL = "usermodel.prop.label";
    public static final String USER_MODEL_PROPERTY_HELP_TEXT = "usermodel.prop.tooltip";
    public static final String USER_MODEL_ATTRIBUTE_LABEL = "usermodel.attr.label";
    public static final String USER_MODEL_ATTRIBUTE_HELP_TEXT = "usermodel.attr.tooltip";

    public static final String USER_MODEL_CLIENT_ROLE_MAPPING_CLIENT_ID = "usermodel.clientRoleMapping.clientId";
    public static final String USER_MODEL_CLIENT_ROLE_MAPPING_CLIENT_ID_LABEL = "usermodel.clientRoleMapping.clientId.label";
    public static final String USER_MODEL_CLIENT_ROLE_MAPPING_CLIENT_ID_HELP_TEXT = "usermodel.clientRoleMapping.clientId.tooltip";

    public static final String USER_MODEL_CLIENT_ROLE_MAPPING_ROLE_PREFIX = "usermodel.clientRoleMapping.rolePrefix";
    public static final String USER_MODEL_CLIENT_ROLE_MAPPING_ROLE_PREFIX_LABEL = "usermodel.clientRoleMapping.rolePrefix.label";
    public static final String USER_MODEL_CLIENT_ROLE_MAPPING_ROLE_PREFIX_HELP_TEXT = "usermodel.clientRoleMapping.rolePrefix.tooltip";

    public static final String USER_MODEL_REALM_ROLE_MAPPING_ROLE_PREFIX = "usermodel.realmRoleMapping.rolePrefix";
    public static final String USER_MODEL_REALM_ROLE_MAPPING_ROLE_PREFIX_LABEL = "usermodel.realmRoleMapping.rolePrefix.label";
    public static final String USER_MODEL_REALM_ROLE_MAPPING_ROLE_PREFIX_HELP_TEXT = "usermodel.realmRoleMapping.rolePrefix.tooltip";

    public static final String USER_SESSION_MODEL_NOTE_LABEL = "userSession.modelNote.label";
    public static final String USER_SESSION_MODEL_NOTE_HELP_TEXT = "userSession.modelNote.tooltip";
    public static final String MULTIVALUED_LABEL = "multivalued.label";
    public static final String AGGREGATE_ATTRS_LABEL = "aggregate.attrs.label";
    public static final String MULTIVALUED_HELP_TEXT = "multivalued.tooltip";
    public static final String AGGREGATE_ATTRS_HELP_TEXT = "aggregate.attrs.tooltip";

    // Priority of SubMapper. It should be first to allow other mappers override the `sub` claim
    public static final int SUB_MAPPER = -10;

    // Role name mapper can move some roles to different positions
    public static final int PRIORITY_ROLE_NAMES_MAPPER = 10;

    // Hardcoded role mapper can be used to add some roles
    public static final int PRIORITY_HARDCODED_ROLE_MAPPER = 20;

    // Audiences can be resolved once all the roles are correctly set
    public static final int PRIORITY_AUDIENCE_RESOLVE_MAPPER = 30;

    // Add roles to tokens finally
    public static final int PRIORITY_ROLE_MAPPER = 40;

    // Script mapper goes last, so it can access the roles in the token
    public static final int PRIORITY_SCRIPT_MAPPER = 50;

    private static final HashMap<String, Method> ACCESSORS = new HashMap<>();

    // This caches known methods to avoid generating unnecessary failed lookups and exception at runtime which are expensive
    static {
        for (Method method : UserModel.class.getMethods()) {
            String propertyName;
            if (method.getName().startsWith("is") && method.getParameterCount() == 0) {
                propertyName = method.getName().substring(2);
            } else if (method.getName().startsWith("get") && method.getParameterCount() == 0) {
                propertyName = method.getName().substring(3);
            } else {
                continue;
            }
            ACCESSORS.put(getLowerCasedProperty(propertyName), method);
        }
    }

    public static String getUserModelValue(UserModel user, String propertyName) {
        // To support existing configurations, we accept property names starting with both upper and lower case
        // as earlier versions of Keycloak where applying this behavior.
        Method m = ACCESSORS.get(getLowerCasedProperty(propertyName));
        if (m == null) {
            return null;
        }

        try {
            Object val = m.invoke(user);
            if (val != null) return val.toString();
        } catch (Exception ignore) {
        }

        return null;
    }

    private static String getLowerCasedProperty(String propertyName) {
        return Character.toLowerCase(propertyName.charAt(0)) + propertyName.substring(1);
    }

    /**
     * Find the builtin locale mapper.
     *
     * @param session A KeycloakSession
     * @return The builtin locale mapper.
     */
    public static ProtocolMapperModel findLocaleMapper(KeycloakSession session) {
        return session.getKeycloakSessionFactory().getProviderFactoriesStream(LoginProtocol.class)
                .map(LoginProtocolFactory.class::cast)
                .map(factory -> factory.getBuiltinMappers().get(OIDCLoginProtocolFactory.LOCALE))
                .filter(Objects::nonNull)
                .filter(protocolMapper -> Objects.equals(protocolMapper.getProtocol(), OIDCLoginProtocol.LOGIN_PROTOCOL))
                .findFirst()
                .orElse(null);
    }


    public static Stream<Entry<ProtocolMapperModel, ProtocolMapper>> getSortedProtocolMappers(KeycloakSession session, ClientSessionContext ctx) {
        return getSortedProtocolMappers(session, ctx, entry -> true);
    }

    public static Stream<Entry<ProtocolMapperModel, ProtocolMapper>> getSortedProtocolMappers(KeycloakSession session, ClientSessionContext ctx, Predicate<Entry<ProtocolMapperModel, ProtocolMapper>> filter) {
        KeycloakSessionFactory sessionFactory = session.getKeycloakSessionFactory();

        Stream<Entry<ProtocolMapperModel, ProtocolMapper>> protocolMapperStream = //
                ctx.getProtocolMappersStream()
                        .<Entry<ProtocolMapperModel, ProtocolMapper>>map(mapperModel -> {
                            ProtocolMapper mapper = (ProtocolMapper) sessionFactory.getProviderFactory(ProtocolMapper.class, mapperModel.getProtocolMapper());
                            if (mapper == null) {
                                return null;
                            }
                            return new AbstractMap.SimpleEntry<>(mapperModel, mapper);
                        })
                        .filter(Objects::nonNull)
                        .filter(filter);

        if (OIDCLoginProtocol.LOGIN_PROTOCOL.equals(ctx.getClientSession().getClient().getProtocol())) {
            protocolMapperStream = Stream.concat(protocolMapperStream, DPoPUtil.getTransientProtocolMapper());
        }

        return protocolMapperStream.sorted(Comparator.comparing(ProtocolMapperUtils::compare));
    }

    public static int compare(Entry<ProtocolMapperModel, ProtocolMapper> entry) {
        int priority = entry.getValue().getPriority();
        return priority;
    }

    public static boolean isEnabled(KeycloakSession session, ProtocolMapperModel mapper) {
        return session.getKeycloakSessionFactory().getProviderFactory(ProtocolMapper.class, mapper.getProtocolMapper()) != null;
    }
}
