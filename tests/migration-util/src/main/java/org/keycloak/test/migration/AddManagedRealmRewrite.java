package org.keycloak.test.migration;

public class AddManagedRealmRewrite extends TestRewrite {

    @Override
    public void rewrite() {
        addImport("org.keycloak.testframework.annotations.InjectRealm");
        addImport("org.keycloak.testframework.realm.ManagedRealm");

        int classDeclaration = findClassDeclaration();

        content.add(classDeclaration + 1, "");
        content.add(classDeclaration + 2, "    @InjectRealm");
        content.add(classDeclaration + 3, "    ManagedRealm managedRealm;");

        info(classDeclaration + 2, "Injecting: ManagedRealm");
    }

}
