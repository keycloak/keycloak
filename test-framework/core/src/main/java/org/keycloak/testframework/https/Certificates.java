package org.keycloak.testframework.https;

import javax.net.ssl.SSLContext;

import org.jboss.logging.Logger;

public interface Certificates {

    Logger getLogger();

    SSLContext getClientSSLContext();
}
