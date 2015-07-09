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
    public static final String USER_MODEL_PROPERTY_LABEL = "User Property";
    public static final String USER_MODEL_PROPERTY_HELP_TEXT = "Name of the property method in the UserModel interface.  For example, a value of 'email' would reference the UserModel.getEmail() method.";
    public static final String USER_MODEL_ATTRIBUTE_LABEL = "User Attribute";
    public static final String USER_MODEL_ATTRIBUTE_HELP_TEXT = "Name of stored user attribute which is the name of an attribute within the UserModel.attribute map.";
    public static final String USER_SESSION_MODEL_NOTE_LABEL = "User Session Note";
    public static final String USER_SESSION_MODEL_NOTE_HELP_TEXT = "Name of stored user session note within the UserSessionModel.note map.";
    public static final String MULTIVALUED_LABEL = "Multivalued";
    public static final String MULTIVALUED_HELP_TEXT = "Indicates if attribute supports multiple values. If true, then the list of all values of this attribute will be set as claim. If false, then just first value will be set as claim";

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
