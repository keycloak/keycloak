package org.keycloak.test.migration;

public class EventAssertRewrite extends TestRewrite {

    @Override
    public void rewrite() {
        boolean events = findLine(".*events[.][a-zA-Z]+\\(.*") != -1;

        if (events) {
            addImport("org.keycloak.testframework.events.EventAssertion");

            for (int i = 0; i < content.size(); i++) {
                String l = content.get(i);
                String trimmed = l.trim();

                if (trimmed.startsWith("events.assertEvent(")) {
                    String updated = l.replaceFirst("events.assertEvent", "EventAssertion.assertSuccess(events.poll())");
                    replaceLine(i, updated);

                    info(i, "Assert event rewritten: \n\t\t" + trimmed + "\n\t\t --> \n\t\t" + updated.trim());
                }
                if (trimmed.startsWith("events.expectLogin(")) {
                    String updated = l.replaceFirst("events.expectLogin", "EventAssertion.assertSuccess(events.poll())");
                    replaceLine(i, updated);

                    info(i, "Assert event rewritten: \n\t\t" + trimmed + "\n\t\t --> \n\t\t" + updated.trim());
                }
            }
        }
    }

}
