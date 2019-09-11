package org.keycloak.testsuite.admin;

/**
 * @author Martin Kanis <mkanis@redhat.com>
 */
public class SMTPConnectionVaultTest extends SMTPConnectionTest {

    public final String SMTP_PASSWORD = setSmtpPassword();

    @Override
    public String setSmtpPassword() {
        return "${vault.smtp_password}";
    }
}