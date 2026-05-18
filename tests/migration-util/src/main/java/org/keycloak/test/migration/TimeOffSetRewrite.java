package org.keycloak.test.migration;

public class TimeOffSetRewrite extends TestRewrite {

    @Override
    public void rewrite() {
        int timeOffSetLine = findLine(".* timeOffSet\\.set\\(.*");
        if (timeOffSetLine != -1) {
            addImport("org.keycloak.testframework.remote.timeoffset.InjectTimeOffSet");
            addImport("org.keycloak.testframework.remote.timeoffset.TimeOffSet");
            int managedRealm = findLine("    ManagedRealm managedRealm;");
            insertContent(managedRealm + 1, "", "    @InjectTimeOffSet",  "    TimeOffSet timeOffSet;");
            info(managedRealm + 1, "Injecting: TimeOffSet");
        }
    }

}
