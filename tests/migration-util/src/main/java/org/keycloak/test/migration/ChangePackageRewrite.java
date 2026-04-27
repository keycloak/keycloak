package org.keycloak.test.migration;

public class ChangePackageRewrite extends TestRewrite {

    @Override
    public void rewrite() {
        int packageDeclaration = findLine("package .*");
        String oldPackageString = content.get(packageDeclaration);
        String packageString = oldPackageString.replace("org.keycloak.testsuite", "org.keycloak.tests");
        replaceLine(packageDeclaration, packageString);

        info(packageDeclaration, "Package rewritten: '" + oldPackageString.substring("package ".length(), oldPackageString.length() - 1) + "' --> '" + packageString.substring("package ".length(), packageString.length() - 1) + "'");
    }

}
