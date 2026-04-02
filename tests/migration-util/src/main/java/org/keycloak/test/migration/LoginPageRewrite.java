package org.keycloak.test.migration;

public class LoginPageRewrite extends TestRewrite {

    @Override
    public void rewrite() {
        int loginPageLine = findLine(".*loginPage\\..*");
        if (loginPageLine >= 0) {
            int startingLine = findClassDeclaration();
            for (int i = startingLine; i < content.size(); i++) {
                String l = content.get(i);
                if (l.trim().contains("loginPage.login(")) {
                    replaceLine(i, l.replace("loginPage.login(", "loginPage.fillLogin("));
                    content.add(i+1, "        loginPage.submit();");
                    info(i, "Statement rewritten: 'loginPage.login(' --> 'loginPage.fillLogin('");
                }
                if (l.trim().contains("loginPage.open()")) {
                    replaceLine(i, l.replace("loginPage.open()", "oauth.openLoginForm()"));
                    info(i, "Statement rewritten: 'loginPage.open()' --> 'oauth.openLoginForm()'");
                }
            }
        }
    }

}
