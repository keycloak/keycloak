package org.keycloak.storage.ldap.mappers;

import org.keycloak.models.UserModel;
import org.keycloak.storage.ldap.LDAPUtils;
import org.keycloak.models.utils.reflection.Property;

import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class UserAttributeTranscriber {
    private static final Pattern pattern = Pattern.compile("\\$\\{attr\\.(.+?)}");
    private static final Map<String, Property<Object>> userModelProperties = LDAPUtils.getUserModelProperties();

    public static String getTemplatedString(final String value, UserModel user) {
        if (value != null) {
            String retVal = value;
            Matcher matcher = pattern.matcher(value);
            while(matcher.find()){
                String attrName = matcher.group(1);
                if(attrName == null || attrName.trim().length() == 0)
                    continue;
                String subVal = subAttr(attrName, user);

                if(subVal != null){
                    retVal = retVal.replace("${attr." + attrName + "}", subVal);
                }
            }
            return retVal;
        }
        return value;
    }

    private static String subAttr(String attrName, UserModel user){
        Property<Object> userModelProperty = userModelProperties.get(attrName.toLowerCase());
        if(userModelProperty != null){
            Object attrValue = userModelProperty.getValue(user);
            if(attrValue != null){
                return attrValue.toString();
            }
        }
        return user.getFirstAttribute(attrName);
    }
}
