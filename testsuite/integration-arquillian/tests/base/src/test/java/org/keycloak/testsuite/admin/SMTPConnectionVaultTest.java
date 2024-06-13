package org.keycloak.testsuite.admin;

import org.keycloak.testsuite.arquillian.annotation.EnableVault;

/**
 * @author Martin Kanis <mkanis@redhat.com>
 */
@EnableVault
public class SMTPConnectionVaultTest extends SMTPConnectionTest {

    @Override
    public String setSmtpPassword() {
        return "${vault.smtp_password}";
    }
}