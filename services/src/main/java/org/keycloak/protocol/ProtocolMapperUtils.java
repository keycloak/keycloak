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

import org.jboss.logging.Logger;
import org.keycloak.models.ClientSessionContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.UserModel;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.OIDCLoginProtocolFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.AbstractMap;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ProtocolMapperUtils {
    private static final Logger log = Logger.getLogger(ProtocolMapperUtils.class);

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

    public static final String USER_PROPERTY_MAPPER_WRONG_PROPERTY = "userPropertyMapperWrongProperty";

    private static final Map<String, Method> USER_MODEL_METHODS = new HashMap<>();
    private static final Map<String, Method> DEPRECATED_USER_MODEL_METHODS = new HashMap<>();
    static {
        for (Method declaredMethod : UserModel.class.getDeclaredMethods()) {
            if (declaredMethod.getParameterCount() == 0 && !(declaredMethod.getReturnType().isAssignableFrom(Stream.class)) && !(declaredMethod.getReturnType().isAssignableFrom(Map.class))) {
                if (declaredMethod.getName().startsWith("is")) {
                    memorizedMethod(declaredMethod.getName().substring(2), declaredMethod);
                } else if (declaredMethod.getName().startsWith("get")) {
                    memorizedMethod(declaredMethod.getName().substring(3), declaredMethod);
                }
            }
        }
    }

    private static void memorizedMethod(String property, Method method) {
        Method existingMethod = DEPRECATED_USER_MODEL_METHODS.put(property, method);
        if (existingMethod != null) {
            throw new IllegalStateException("found methods with colliding names: " + method.getName() + " and " + existingMethod.getName());
        }
        property = property.substring(0, 1).toLowerCase() + property.substring(1);
        existingMethod = USER_MODEL_METHODS.put(property, method);
        if (existingMethod != null) {
            throw new IllegalStateException("found methods with colliding names: " + method.getName() + " and " + existingMethod.getName());
        }
    }

    public static void validateUserModelProperty(String propertyName) throws ProtocolMapperConfigException {
        if (!USER_MODEL_METHODS.containsKey(propertyName)) {
            throw new ProtocolMapperConfigException(USER_PROPERTY_MAPPER_WRONG_PROPERTY, "User property '" + propertyName + "' does not exist, try one of these property names: " + USER_MODEL_METHODS.keySet(),
                    propertyName, USER_MODEL_METHODS.keySet().toString());
        }
    }

    public static String getUserModelValue(UserModel user, String propertyName) {
        Method method = USER_MODEL_METHODS.get(propertyName);
        if (method == null) {
            method = DEPRECATED_USER_MODEL_METHODS.get(propertyName);
            if (method != null) {
                log.warn("Using user properties starting with a capital letter like '" + propertyName + "' is deprecated since Keycloak 22, update your configurations");
            } else {
                // For KC22, log a warning. Future versions should throw an exception as this is probably an error and wouldn't work as the user expects it to work
                log.warn("User property '" + propertyName + "' doesn't exist, try one of these property values: " + USER_MODEL_METHODS.keySet());
                return null;
            }
        }

        try {
            Object val = method.invoke(user);
            return val != null ? val.toString() : null;
        } catch (IllegalAccessException | InvocationTargetException e) {
            log.warn("unable to retrieve property '" + propertyName + "'", e);
            return null;
        }
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
        KeycloakSessionFactory sessionFactory = session.getKeycloakSessionFactory();
        return ctx.getProtocolMappersStream()
                .<Entry<ProtocolMapperModel, ProtocolMapper>>map(mapperModel -> {
                    ProtocolMapper mapper = (ProtocolMapper) sessionFactory.getProviderFactory(ProtocolMapper.class, mapperModel.getProtocolMapper());
                    if (mapper == null) {
                        return null;
                    }
                    return new AbstractMap.SimpleEntry<>(mapperModel, mapper);
                })
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(ProtocolMapperUtils::compare));
    }

    public static int compare(Entry<ProtocolMapperModel, ProtocolMapper> entry) {
        int priority = entry.getValue().getPriority();
        return priority;
    }

    public static boolean isEnabled(KeycloakSession session, ProtocolMapperModel mapper) {
        return session.getKeycloakSessionFactory().getProviderFactory(ProtocolMapper.class, mapper.getProtocolMapper()) != null;
    }
}
