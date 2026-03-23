package org.keycloak.test.migration;

public class RunOnServerRewrite extends TestRewrite {

    private static final String REGEX = "testingClient\\.server\\([a-zA-Z\"_-]*\\)";
    private static final String REPLACEMENT = "runOnServerClient";

    @Override
    public void rewrite() {
        int runOnServerLine = findLine(".*testingClient.server(.*)[.].*");
        if (runOnServerLine >= 0) {
            int managedRealm = findLine("    ManagedRealm managedRealm;");

            if (!content.get(runOnServerLine).trim().contains("testingClient.server()")) {
                content.add(managedRealm + 1, "");
                content.add(managedRealm + 2, "    @InjectRunOnServer");
                content.add(managedRealm + 3, "    RunOnServerClient runOnServerClient;");
                info(managedRealm + 2, "Injecting: RunOnServerClient");
            } else {
                content.add(managedRealm + 1, "");
                content.add(managedRealm + 2, "    @InjectRealm(ref = \"master\", attachTo = \"master\")");
                content.add(managedRealm + 3, "    ManagedRealm masterRealm;");
                content.add(managedRealm + 4, "");
                content.add(managedRealm + 5, "    @InjectRunOnServer(ref = \"runOnMaster\", realmRef=\"master\")");
                content.add(managedRealm + 6, "    RunOnServerClient runOnServerClient;");
                info(managedRealm + 5, "Injecting: RunOnServerClient");
            }
            addImport("org.keycloak.testframework.remote.runonserver.InjectRunOnServer");
            addImport("org.keycloak.testframework.remote.runonserver.RunOnServerClient");
        }

        int startingLine = findClassDeclaration();
        for (int i = startingLine; i < content.size(); i++) {
            String l = content.get(i);
            if (l.trim().contains("testingClient.server")) {
                replaceLine(i, l.replaceAll(REGEX, REPLACEMENT));
                info(i, "Statement rewritten: '" + REGEX + "' --> '" + REPLACEMENT + "'");
            }
        }
    }

}
