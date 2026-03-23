package org.keycloak.test.migration;

public class AddManagedResourcesRewrite extends TestRewrite {

    @Override
    public void rewrite() {
        boolean assertAdminEvents = findLine(".*assertAdminEvents[.].*") != -1;
        boolean assertEvents = findLine(".*events[.].*") != -1;
        boolean mailServer = findLine(".*greenMail.*") != -1;

        addImport("org.keycloak.testframework.annotations.InjectRealm");
        addImport("org.keycloak.testframework.realm.ManagedRealm");

        int classDeclaration = findClassDeclaration();

        content.add(classDeclaration + 1, "");
        content.add(classDeclaration + 2, "    @InjectRealm");
        content.add(classDeclaration + 3, "    ManagedRealm managedRealm;");

        info(classDeclaration + 2, "Injecting: ManagedRealm");

        if (assertAdminEvents) {
            addImport("org.keycloak.testframework.annotations.InjectAdminEvents");
            addImport("org.keycloak.testframework.events.AdminEvents");

            int managedRealm = findLine("    ManagedRealm managedRealm;");

            content.add(managedRealm + 1, "");
            content.add(managedRealm + 2, "    @InjectAdminEvents");
            content.add(managedRealm + 3, "    AdminEvents adminEvents;");

            info(managedRealm + 2, "Injecting: AdminEvents");
        }

        if (assertEvents) {
            addImport("org.keycloak.testframework.annotations.InjectEvents");
            addImport("org.keycloak.testframework.events.Events");

            int managedRealm = findLine("    ManagedRealm managedRealm;");

            content.add(managedRealm + 1, "");
            content.add(managedRealm + 2, "    @InjectEvents");
            content.add(managedRealm + 3, "    Events events;");

            info(managedRealm + 2, "Injecting: Events");
        }

        if (mailServer) {
            addImport("org.keycloak.testframework.mail.annotations.InjectMailServer");
            addImport("org.keycloak.testframework.mail.MailServer");

            int managedRealm = findLine("    ManagedRealm managedRealm;");

            content.add(managedRealm + 1, "");
            content.add(managedRealm + 2, "    @InjectMailServer");
            content.add(managedRealm + 3, "    MailServer mailServer;");

            info(managedRealm + 2, "Injecting: MailServer");
        }
    }

}
