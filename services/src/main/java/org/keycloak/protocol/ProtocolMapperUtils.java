package org.keycloak.protocol;

import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.UserModel;
import org.keycloak.representations.AccessToken;

import java.lang.reflect.Method;
import java.util.List;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ProtocolMapperUtils {
    public static final String USER_ATTRIBUTE = "user.attribute";
    public static final String USER_SESSION_NOTE = "user.session.note";
    public static final String MULTIVALUED = "multivalued";
    public static final String USER_MODEL_PROPERTY_LABEL = "usermodel.prop.label";
    public static final String USER_MODEL_PROPERTY_HELP_TEXT = "usermodel.prop.tooltip";
    public static final String USER_MODEL_ATTRIBUTE_LABEL = "usermodel.attr.label";
    public static final String USER_MODEL_ATTRIBUTE_HELP_TEXT = "usermodel.attr.tooltip";
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

    public static String[] parseRole(String role) {
        int scopeIndex = role.lastIndexOf('.');
        if (scopeIndex > -1) {
            String appName = role.substring(0, scopeIndex);
            role = role.substring(scopeIndex + 1);
            String[] rtn = {appName, role};
            return rtn;
        } else {
            String[] rtn = {null, role};
            return rtn;

        }
    }
}
