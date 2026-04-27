package org.keycloak.test.migration;

public class AddManagedResourcesRewrite extends TestRewrite {

    @Override
    public void rewrite() {
        boolean assertAdminEvents = findLine(".*assertAdminEvents[.].*") != -1;

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
    }

}
