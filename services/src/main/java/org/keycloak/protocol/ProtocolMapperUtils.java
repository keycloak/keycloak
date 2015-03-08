package org.keycloak.protocol;

import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.UserModel;

import java.lang.reflect.Method;
import java.util.List;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ProtocolMapperUtils {
    public static final String USER_ATTRIBUTE = "user.attribute";
    public static final String USER_SESSION_NOTE = "user.session.note";
    public static final String USER_MODEL_PROPERTY_LABEL = "User Property";
    public static final String USER_MODEL_PROPERTY_HELP_TEXT = "Name of the property method in the UserModel interface.  For example, a value of 'email' would reference the UserModel.getEmail() method.";
    public static final String USER_MODEL_ATTRIBUTE_LABEL = "User Attribute";
    public static final String USER_MODEL_ATTRIBUTE_HELP_TEXT = "Name of stored user attribute which is the name of an attribute within the UserModel.attribute map.";
    public static final String USER_SESSION_MODEL_NOTE_LABEL = "User Session Note";
    public static final String USER_SESSION_MODEL_NOTE_HELP_TEXT = "Name of stored user session note within the UserSessionModel.note map.";

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
}
