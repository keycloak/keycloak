package org.keycloak.performance.util;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author tkyjovsk
 */
public class StringUtil {

    public static String firstLetterToLowerCase(String string) {
        return string.substring(0, 1).toLowerCase() + string.substring(1);
    }

    public static String firstLetterToUpperCase(String string) {
        return string.substring(0, 1).toUpperCase() + string.substring(1);
    }

    public static List<String> parseStringList(String string) {
        return parseStringList(string, ",");
    }

    public static List<String> parseStringList(String string, String delimiter) {
        List<String> list = new ArrayList<>();
        for (String s : string.split(delimiter)) {
            list.add(s.trim());
        }
        return list;
    }

}
