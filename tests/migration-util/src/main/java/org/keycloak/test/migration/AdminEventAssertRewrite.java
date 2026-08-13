package org.keycloak.test.migration;

public class AdminEventAssertRewrite extends TestRewrite {

    @Override
    public void rewrite() {
        boolean assertAdminEvents = findLine(".*assertAdminEvents[.]assertEvent\\(.*") != -1;

        if (assertAdminEvents) {
            addImport("org.keycloak.testframework.events.AdminEventAssertion");

            for (int i = 0; i < content.size(); i++) {
                String l = content.get(i);
                String trimmed = l.trim();

                if (trimmed.startsWith("assertAdminEvents.assertEvent(")) {
                    String updated = l.replaceFirst("assertAdminEvents.assertEvent\\([^,]*", "AdminEventAssertion.assertEvent(adminEvents.poll()");
                    replaceLine(i, updated);

                    info(i, "Assert admin event rewritten: \n\t\t" + trimmed + "\n\t\t --> \n\t\t" + updated.trim());
                }
            }
        }
    }

}
