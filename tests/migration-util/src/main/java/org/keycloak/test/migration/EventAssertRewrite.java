package org.keycloak.test.migration;

public class EventAssertRewrite extends TestRewrite {

    @Override
    public void rewrite() {
        boolean events = findLine(".*events[.][a-zA-Z]+\\(.*") != -1;

        if (events) {
            addImport("org.keycloak.testframework.annotations.InjectEvents");
            addImport("org.keycloak.testframework.events.Events");

            int managedRealm = findLine("    ManagedRealm managedRealm;");

            content.add(managedRealm + 1, "");
            content.add(managedRealm + 2, "    @InjectEvents");
            content.add(managedRealm + 3, "    Events events;");

            info(managedRealm + 2, "Injecting: Events");

            addImport("org.keycloak.testframework.events.EventAssertion");

            for (int i = 0; i < content.size(); i++) {
                String l = content.get(i);
                String trimmed = l.trim();

                if (trimmed.startsWith("events.")) {
                    String updated = l.replaceFirst("events.", "EventAssertion.assertSuccess(events.poll())");
                    replaceLine(i, updated);

                    info(i, "Assert event rewritten: \n\t\t" + trimmed + "\n\t\t --> \n\t\t" + updated.trim());
                }
            }
        }
    }

}
