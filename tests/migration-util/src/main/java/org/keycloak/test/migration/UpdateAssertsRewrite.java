package org.keycloak.test.migration;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class UpdateAssertsRewrite extends TestRewrite {

    @Override
    public void rewrite() {
        for (int i = 0; i < content.size(); i++) {
            String l = content.get(i);
            String trimmed = l.trim();
            if (trimmed.startsWith("Assert.")) {
                String method = trimmed.substring("Assert.".length(), trimmed.indexOf("("));
                int arguments = l.substring(l.indexOf("(") + 1, l.lastIndexOf(")")).split(", ").length;

                if (method.equals("fail")) {
                    directReplace(i, "fail");
                } else if (method.equals("assertTrue") && arguments == 1) {
                    directReplace(i, "assertTrue");
                } else if (method.equals("assertNull") && arguments == 1) {
                    directReplace(i, "assertNull");
                } else if (method.equals("assertNotNull") && arguments == 1) {
                    directReplace(i, "assertNotNull");
                } else if (method.equals("assertNotNull") && arguments == 2) {
                    moveMessageToLast(i, "assertNotNull");
                }  else if (method.equals("assertEquals") && arguments == 2) {
                    directReplace(i, "assertEquals");
                } else if (method.equals("assertEquals") && arguments == 3) {
                    moveMessageToLast(i, "assertEquals");
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
