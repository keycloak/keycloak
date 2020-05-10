package org.keycloak.testsuite.admin;

import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude.AuthServer;
import org.keycloak.testsuite.arquillian.annotation.EnableVault;

/**
 * @author Martin Kanis <mkanis@redhat.com>
 */
@EnableVault
@AuthServerContainerExclude(AuthServer.REMOTE)
public class SMTPConnectionVaultTest extends SMTPConnectionTest {

    @Override
    public String setSmtpPassword() {
        return "${vault.smtp_password}";
    }
}