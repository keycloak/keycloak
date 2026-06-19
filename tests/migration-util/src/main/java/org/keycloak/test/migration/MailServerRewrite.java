package org.keycloak.test.migration;

public class MailServerRewrite extends TestRewrite {

    @Override
    public void rewrite() {
        int mailServerLine = findLine(".*MailServer mail = new MailServer().*");
        if (mailServerLine != -1) {
            replaceLine(mailServerLine - 1, "    @InjectMailServer");
            replaceLine(mailServerLine, "    MailServer mail;");
            addImport("org.keycloak.testframework.mail.annotations.InjectMailServer");
        }
    }

}
