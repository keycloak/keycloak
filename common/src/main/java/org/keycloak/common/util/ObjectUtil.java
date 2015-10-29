package org.keycloak.common.util;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class ObjectUtil {

    private ObjectUtil() {}

    /**
     *
     * @param str1
     * @param str2
     * @return true if both strings are null or equal
     */
    public static boolean isEqualOrBothNull(Object str1, Object str2) {
        if (str1 == null && str2 == null) {
            return true;
        }

        if ((str1 != null && str2 == null) || (str1 == null && str2 != null)) {
            return false;
        }

        return str1.equals(str2);
    }
}
