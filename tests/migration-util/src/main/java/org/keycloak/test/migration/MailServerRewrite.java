package org.keycloak.test.migration;

public class MailServerRewrite extends TestRewrite {

    @Override
    public void rewrite() {
        int mailServerLine = findLine(".*MailServer mail = new MailServer().*");
        if (mailServerLine != -1) {
            addImport("org.keycloak.testframework.mail.annotations.InjectMailServer");
            replaceLine(mailServerLine - 1, "\n    @InjectMailServer");
            replaceLine(mailServerLine, "    MailServer mail;");
        }
    }

}
