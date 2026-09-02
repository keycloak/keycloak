package org.keycloak.test.migration;

public class AssertEventsRewrite extends TestRewrite {

    @Override
    public void rewrite() {
        int assertEventsLine = findLine(".*AssertEvents events = new AssertEvents\\(this\\);.*");
        if (assertEventsLine != -1) {
            replaceLine(assertEventsLine - 1, "    @InjectEvents");
            replaceLine(assertEventsLine, "    Events events;");
            addImport("org.keycloak.testframework.annotations.InjectEvents");
            addImport("org.keycloak.testframework.events.Events");
        }
    }

}
