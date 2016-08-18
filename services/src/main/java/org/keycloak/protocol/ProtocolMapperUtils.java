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

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.UserModel;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.OIDCLoginProtocolFactory;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderFactory;

import java.lang.reflect.Method;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ProtocolMapperUtils {

    public static final String USER_ROLE = "user.role";
    public static final String USER_ATTRIBUTE = "user.attribute";
    public static final String USER_SESSION_NOTE = "user.session.note";
    public static final String MULTIVALUED = "multivalued";
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
    public static final String MULTIVALUED_HELP_TEXT = "multivalued.tooltip";

    public static String getUserModelValue(UserModel user, String propertyName) {

        String methodName = "get" + Character.toUpperCase(propertyName.charAt(0)) + propertyName.substring(1);
        try {
            Method method = UserModel.class.getMethod(methodName);
            Object val = method.invoke(user);
            if (val != null) return val.toString();
        } catch (Exception ignore) {

        }
        methodName = "is" + Character.toUpperCase(propertyName.charAt(0)) + propertyName.substring(1);
        try {
            Method method = UserModel.class.getMethod(methodName);
            Object val = method.invoke(user);
            if (val != null) return val.toString();
        } catch (Exception ignore) {

        }
        return null;
    }

    /**
     * Find the builtin locale mapper.
     *
     * @param session A KeycloakSession
     * @return The builtin locale mapper.
     */
    public static ProtocolMapperModel findLocaleMapper(KeycloakSession session) {
        ProtocolMapperModel found = null;
        for (ProviderFactory p : session.getKeycloakSessionFactory().getProviderFactories(LoginProtocol.class)) {
            LoginProtocolFactory factory = (LoginProtocolFactory) p;
            for (ProtocolMapperModel mapper : factory.getBuiltinMappers()) {
                if (mapper.getName().equals(OIDCLoginProtocolFactory.LOCALE) && mapper.getProtocol().equals(OIDCLoginProtocol.LOGIN_PROTOCOL)) {
                    found = mapper;
                    break;
                }
            }
            if (found != null) break;
        }
        return found;
    }
}
