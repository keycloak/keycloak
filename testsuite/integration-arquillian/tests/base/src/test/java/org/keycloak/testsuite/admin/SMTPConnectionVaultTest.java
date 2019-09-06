package org.keycloak.testsuite.admin;

/**
 * @author Martin Kanis <mkanis@redhat.com>
 */
public class SMTPConnectionVaultTest extends SMTPConnectionTest {

    final static String SMTP_PASSWORD = "${vault.smtp_password}";
}
