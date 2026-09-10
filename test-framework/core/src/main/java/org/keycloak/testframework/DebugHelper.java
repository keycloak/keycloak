package org.keycloak.testframework;

import java.lang.reflect.Method;

public class DebugHelper {

    private static boolean IN_TEST = false;
    private static String CURRENT_TEST_CLASS;
    private static String CURRENT_TEST_METHOD;

    static void testStarted(Class<?> clazz, Method method) {
        IN_TEST = true;
        CURRENT_TEST_CLASS = clazz.getName();
        CURRENT_TEST_METHOD = method.getName();
    }

    static void testFinished() {
        IN_TEST = false;
        CURRENT_TEST_CLASS = null;
        CURRENT_TEST_METHOD = null;
    }

    public static boolean isInTest() {
        return IN_TEST;
    }

    public static boolean isInTest(String test) {
        if (!IN_TEST) {
            return false;
        }

        String[] split = test.split("#");

        String expectedClassName = split[0].isEmpty() ? null : split[0];
        String expectedMethod = split.length > 1 ? split[1] : null;

        if (expectedClassName != null) {
            if (expectedClassName.indexOf('.') != -1) {
                if (!expectedClassName.equals(CURRENT_TEST_CLASS)) {
                    return false;
                }
            } else {
                String currentTestSimpleName = CURRENT_TEST_CLASS.substring(CURRENT_TEST_CLASS.lastIndexOf('.') + 1);
                if (!expectedClassName.equals(currentTestSimpleName)) {
                    return false;
                }
            }
        }

        if (expectedMethod != null) {
            if (!expectedMethod.equals(CURRENT_TEST_METHOD)) {
                return false;
            }
        }

        return true;
    }

}
