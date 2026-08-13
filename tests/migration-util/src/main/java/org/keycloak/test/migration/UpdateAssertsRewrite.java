package org.keycloak.test.migration;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class UpdateAssertsRewrite extends TestRewrite {

    private final static String FAIL = "fail";
    private final static String ASSERT_THROWS = "assertThrows";
    private final static String ASSERT_TRUE = "assertTrue";
    private final static String ASSERT_FALSE = "assertFalse";
    private final static String ASSERT_NULL = "assertNull";
    private final static String ASSERT_NOT_NULL = "assertNotNull";
    private final static String ASSERT_EQUALS = "assertEquals";
    private final static String ASSERT_NOT_EQUALS = "assertNotEquals";

    @Override
    public void rewrite() {
        for (int i = 0; i < content.size(); i++) {
            String l = content.get(i);
            String trimmed = l.trim();
            if (trimmed.startsWith("Assert.") && trimmed.endsWith(";")) {
                String method = trimmed.substring("Assert.".length(), trimmed.indexOf("("));
                int arguments = l.substring(l.indexOf("(") + 1, l.lastIndexOf(")")).split(", ").length;

                if (method.equals(FAIL)) {
                    directReplace(i, FAIL);
                } else if (method.equals(ASSERT_THROWS) && arguments == 3) {
                    moveMessageToLast(i, ASSERT_THROWS);
                } else if (method.equals(ASSERT_TRUE) && arguments == 1) {
                    directReplace(i, ASSERT_TRUE);
                } else if (method.equals(ASSERT_TRUE) && arguments == 2) {
                    moveMessageToLast(i, ASSERT_TRUE);
                } else if (method.equals(ASSERT_FALSE) && arguments == 1) {
                    directReplace(i, ASSERT_FALSE);
                } else if (method.equals(ASSERT_FALSE) && arguments == 2) {
                    moveMessageToLast(i, ASSERT_FALSE);
                } else if (method.equals(ASSERT_NULL) && arguments == 1) {
                    directReplace(i, ASSERT_NULL);
                } else if (method.equals(ASSERT_NULL) && arguments == 2) {
                    moveMessageToLast(i, ASSERT_NULL);
                } else if (method.equals(ASSERT_NOT_NULL) && arguments == 1) {
                    directReplace(i, ASSERT_NOT_NULL);
                } else if (method.equals(ASSERT_NOT_NULL) && arguments == 2) {
                    moveMessageToLast(i, ASSERT_NOT_NULL);
                }  else if (method.equals(ASSERT_EQUALS) && arguments == 2) {
                    directReplace(i, ASSERT_EQUALS);
                } else if (method.equals(ASSERT_EQUALS) && arguments == 3) {
                    moveMessageToLast(i, ASSERT_EQUALS);
                }  else if (method.equals(ASSERT_NOT_EQUALS) && arguments == 2) {
                    directReplace(i, ASSERT_NOT_EQUALS);
                } else if (method.equals(ASSERT_NOT_EQUALS) && arguments == 3) {
                    moveMessageToLast(i, ASSERT_NOT_EQUALS);
                }
            }
        }
    }

    private void directReplace(int l, String method) {
        replaceLine(l, content.get(l).replace("Assert." + method, "Assertions." + method));
    }

    private void moveMessageToLast(int l, String method) {
        String current = content.get(l);
        String start = current.substring(0, current.indexOf('(') + 1);
        String end = current.substring(current.lastIndexOf(')'));
        List<String> arguments = new LinkedList<>(Arrays.asList(current.substring(start.length(), current.lastIndexOf(')')).split(", ")));
        String messageArgument = arguments.remove(0);
        arguments.add(messageArgument);

        String updated = start.replace("Assert." + method, "Assertions." + method)
                + String.join(", ", arguments)
                + end;

        replaceLine(l, updated);

        info(l, "Assert rewritten: \n\t\t" + current.trim() + "\n\t\t --> \n\t\t" + updated.trim());
    }

}
