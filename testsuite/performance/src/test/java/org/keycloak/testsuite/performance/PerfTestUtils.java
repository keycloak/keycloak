package org.keycloak.testsuite.performance;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public final class PerfTestUtils {

    private PerfTestUtils() {
    }

    public static <T> T readSystemProperty(String propertyName, Class<T> expectedClass) {
        String propAsString = System.getProperty(propertyName);
        if (propAsString == null || propAsString.length() == 0) {
            throw new IllegalArgumentException("Property '" + propertyName + "' not specified");
        }

        if (Integer.class.equals(expectedClass)) {
            return expectedClass.cast(Integer.parseInt(propAsString));
        } else if (Boolean.class.equals(expectedClass)) {
            return expectedClass.cast(Boolean.valueOf(propAsString));
        }  else {
            throw new IllegalArgumentException("Not supported type " + expectedClass);
        }
    }

    public static String getRealmName(int realmNumber) {
        return "realm" + realmNumber;
    }

    public static String getApplicationName(int realmNumber, int applicationNumber) {
        return getRealmName(realmNumber) + "application" + applicationNumber;
    }

    public static String getRoleName(int realmNumber, int roleNumber) {
        return getRealmName(realmNumber) + "role" + roleNumber;
    }

    public static String getDefaultRoleName(int realmNumber, int defaultRoleNumber) {
        return getRealmName(realmNumber) + "defrole" + defaultRoleNumber;
    }

    public static String getApplicationRoleName(int realmNumber, int applicationNumber, int roleNumber) {
        return getApplicationName(realmNumber, applicationNumber) + "role" + roleNumber;
    }

    public static String getUsername(int userNumber) {
        return "user" + userNumber;
    }
}
