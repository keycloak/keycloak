/*
 * Copyright 2019 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.testsuite.x509;

import java.security.NoSuchProviderException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;

import org.jboss.arquillian.drone.api.annotation.Drone;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.util.PhantomJSBrowser;
import org.openqa.selenium.WebDriver;


/**
 * @author Sebastian Loesch
 * @date 02/14/2019
 */

public class X509BrowserLoginIssuerDnTest extends AbstractX509AuthenticationTest {

    @Drone
    @PhantomJSBrowser
    private WebDriver phantomJS;

    @Before
    public void replaceTheDefaultDriver() {
        replaceDefaultWebDriver(phantomJS);
    }

    @BeforeClass
    public static void checkAssumption() {
        try {
            CertificateFactory.getInstance("X.509", "SUN");
        }
        catch (CertificateException | NoSuchProviderException e) {
            Assume.assumeNoException("Test assumes the SUN security provider", e);
        }
    }

    @BeforeClass
    public static void onBeforeTestClass() {
        configurePhantomJS("/ca.crt", "/certs/clients/test-user-san@localhost.cert.pem",
                           "/certs/clients/test-user@localhost.key.pem", "password");
    }

    private String setup(boolean canonicalDnEnabled) throws Exception {
        String issuerDn = canonicalDnEnabled ?
            "1.2.840.113549.1.9.1=#1614636f6e74616374406b6579636c6f616b2e6f7267,cn=keycloak intermediate ca,ou=keycloak,o=red hat,st=ma,c=us" :
            "EMAILADDRESS=contact@keycloak.org, CN=Keycloak Intermediate CA, OU=Keycloak, O=Red Hat, ST=MA, C=US";

        UserRepresentation user = findUser("test-user@localhost");
        user.singleAttribute("x509_certificate_identity", issuerDn);
        updateUser(user);
        return issuerDn;
    }

    @Test
    public void loginAsUserFromCertIssuerDnCanonical() throws Exception {
        String issuerDn = setup(true);
        x509BrowserLogin(createLoginIssuerDNToCustomAttributeConfig(true), userId, "test-user@localhost", issuerDn);
    }

    @Test
    public void loginAsUserFromCertIssuerDnNonCanonical() throws Exception {
        String issuerDn = setup(false);
        x509BrowserLogin(createLoginIssuerDNToCustomAttributeConfig(false), userId, "test-user@localhost", issuerDn);
  }
}
