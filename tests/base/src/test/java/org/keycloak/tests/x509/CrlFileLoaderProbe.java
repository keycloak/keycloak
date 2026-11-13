package org.keycloak.tests.x509;

import java.security.cert.X509CRL;
import java.util.Collection;

import org.keycloak.authentication.authenticators.x509.CertificateValidator;
import org.keycloak.testframework.remote.providers.runonserver.FetchOnServer;

public final class CrlFileLoaderProbe {

    private CrlFileLoaderProbe() {
    }

    public static FetchOnServer configDir() {
        return session -> System.getProperty("jboss.server.config.dir");
    }

    public static FetchOnServer loadCrl(String crlPath) {
        return session -> {
            try {
                Collection<X509CRL> crls = new CertificateValidator.CRLFileLoader(session, crlPath, false).getX509CRLs();
                return "LOADED:" + crls.size();
            } catch (Throwable t) {
                return "ERROR:" + t.getClass().getName() + ":" + t.getMessage();
            }
        };
    }
}
