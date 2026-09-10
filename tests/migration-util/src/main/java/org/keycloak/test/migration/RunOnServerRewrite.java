package org.keycloak.test.migration;

public class RunOnServerRewrite extends TestRewrite {

    private static final String TESTING_CLIENT_REGEX = "testingClient\\.server\\([a-zA-Z\"_-]*\\)";
    private static final String RUN_ON_SERVER_VAR = "runOnServer";
    private static final String RUN_ON_SERVER_MASTER_VAR = RUN_ON_SERVER_VAR + "Master";

    @Override
    public void rewrite() {
        int testingClientLine = findLine(".*testingClient.server(.*)[.].*");
        boolean hasTestingClientLine = testingClientLine > -1;
        boolean hasRunOnServerLine = findLine(".*runOnServer\\..*") > -1;
        boolean hasRunOnServerMasterLine = findLine(".*runOnServerMaster\\..*") > -1;

        if (!hasTestingClientLine && !hasRunOnServerLine && !hasRunOnServerMasterLine) {
            return;
        }

        addImport("org.keycloak.testframework.remote.runonserver.InjectRunOnServer");
        addImport("org.keycloak.testframework.remote.runonserver.RunOnServerClient");

        int insertionPoint = findLine("    ManagedRealm managedRealm;");
        if (hasRunOnServerMasterLine) {
            content.add(++insertionPoint, "");
            content.add(++insertionPoint, "    @InjectRealm(ref=\"master\", attachTo=\"master\")");
            content.add(++insertionPoint, "    ManagedRealm managedMasterRealm;");
            content.add(++insertionPoint, "");
            content.add(++insertionPoint, "    @InjectRunOnServer(ref=\"master\", realmRef=\"master\")");
            content.add(++insertionPoint, "    RunOnServerClient " + RUN_ON_SERVER_MASTER_VAR + ";");
            info(insertionPoint, "Injecting: RunOnServerClient");
        }
        if (hasRunOnServerLine || hasTestingClientLine) {
            content.add(++insertionPoint, "");
            content.add(++insertionPoint, "    @InjectRunOnServer");
            content.add(++insertionPoint, "    RunOnServerClient " + RUN_ON_SERVER_VAR + ";");
            info(insertionPoint, "Injecting: RunOnServerClient");
        }

        if (hasTestingClientLine) {
            for (int i = testingClientLine; i < content.size(); i++) {
                String l = content.get(i);
                if (l.trim().contains("testingClient.server")) {
                    replaceLine(i, l.replaceAll(TESTING_CLIENT_REGEX, RUN_ON_SERVER_VAR));
                    info(i, "Statement rewritten: '" + TESTING_CLIENT_REGEX + "' --> '" + RUN_ON_SERVER_VAR + "'");
                }
            }
        }
    }

}
