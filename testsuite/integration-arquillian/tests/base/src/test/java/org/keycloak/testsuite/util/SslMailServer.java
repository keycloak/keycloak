/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.testsuite.util;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.HashMap;
import java.util.Map;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

import jakarta.mail.internet.MimeMessage;

import org.jboss.logging.Logger;
import org.subethamail.smtp.server.SMTPServer;

import static org.keycloak.testsuite.util.MailServerConfiguration.FROM;
import static org.keycloak.testsuite.util.MailServerConfiguration.HOST;
import static org.keycloak.testsuite.util.MailServerConfiguration.PORT;
import static org.keycloak.testsuite.util.MailServerConfiguration.PORT_SSL;
import static org.keycloak.testsuite.util.MailServerConfiguration.STARTTLS;

public class SslMailServer {

    private static final Logger log = Logger.getLogger(MailServer.class);

    public static final String PRIVATE_KEY = "keystore/keycloak.jks";

    public static final String TRUSTED_CERTIFICATE = "keystore/keycloak.truststore";

    //private key tested with invalid certificate
    public static final String INVALID_KEY = "keystore/email_invalid.jks";

    private static MessageHandlerFactoryImpl messageHandlerFactory = new MessageHandlerFactoryImpl();

    private static SMTPServer smtpServer;

    private static Map<String, String> serverConfiguration = new HashMap<>();


    public static void start() {
        smtpServer = new SMTPServer(messageHandlerFactory);
        smtpServer.setHostName(HOST);
        smtpServer.setPort(Integer.parseInt(PORT));
        smtpServer.start();

        log.info("Started mail server (" + smtpServer.getHostName() + ":" + smtpServer.getPort() + ")");
    }

    public static void stop() {
        if (smtpServer != null) {
            log.info("Stopping mail server (" + smtpServer.getHostName() + ":" + smtpServer.getPort() + ")");
            // Suppress error from SubEthaSmtp on shutdown
            Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
                @Override
                public void uncaughtException(Thread t, Throwable e) {
                    if (!(e.getCause() instanceof SocketException && e.getStackTrace()[0].getClassName()
                            .equals("org.subethamail.smtp.server.Session"))) {
                        log.error("Exception in thread \"" + t.getName() + "\" ");
                        log.error(e.getMessage(), e);
                    }
                }
            });
            smtpServer.stop();
        }
    }

    public static void startWithSsl(String privateKey, boolean enableSsl) {
        InputStream keyStoreIS = null;
        try {
            keyStoreIS = new FileInputStream(privateKey);
            char[] keyStorePassphrase = "secret".toCharArray();
            KeyStore ksKeys = null;
            ksKeys = KeyStore.getInstance("JKS");
            ksKeys.load(keyStoreIS, keyStorePassphrase);

            // KeyManager decides which key material to use.
            KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            kmf.init(ksKeys, keyStorePassphrase);

            // Trust store for client authentication.
            InputStream trustStoreIS = new FileInputStream(String.valueOf(MailServer.class.getClassLoader().getResource(TRUSTED_CERTIFICATE).getFile()));
            char[] trustStorePassphrase = "secret".toCharArray();
            KeyStore ksTrust = KeyStore.getInstance("JKS");
            ksTrust.load(trustStoreIS, trustStorePassphrase);

            // TrustManager decides which certificate authorities to use.
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            tmf.init(ksTrust);

            final SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);

            smtpServer = new SMTPServer(messageHandlerFactory) {
                @Override
                public SSLSocket createSSLSocket(Socket socket) throws IOException {
                    InetSocketAddress remoteAddress =
                            (InetSocketAddress) socket.getRemoteSocketAddress();
                    SSLSocketFactory sf = sslContext.getSocketFactory();
                    SSLSocket s = (SSLSocket) (sf.createSocket(
                            socket, remoteAddress.getHostName(), socket.getPort(), true));

                    // we are a server
                    s.setUseClientMode(false);

                    // select protocols and cipher suites
                    s.setEnabledProtocols(s.getSupportedProtocols());
                    s.setEnabledCipherSuites(s.getSupportedCipherSuites());
                    return s;
                }
            };
        } catch (KeyStoreException | IOException | NoSuchAlgorithmException | UnrecoverableKeyException | KeyManagementException | CertificateException e) {
            throw new RuntimeException(e);
        }

        smtpServer.setHostName(HOST);
        smtpServer.setPort(Integer.parseInt(PORT_SSL));
        smtpServer.setEnableTLS(enableSsl);
        smtpServer.start();

        log.info("Started mail server (" + smtpServer.getHostName() + ":" + smtpServer.getPort() + ")");
    }

    public static void startWithSsl(String privateKey) {
        startWithSsl(privateKey, true);
    }

    public static void startWithOpportunisticSsl(String privateKey) {
        startWithSsl(privateKey, false);
    }

    public static Map<String, String> getServerConfiguration() {
        serverConfiguration.put("from", FROM);
        serverConfiguration.put("host", HOST);
        serverConfiguration.put("port", PORT_SSL);
        serverConfiguration.put("starttls", STARTTLS);
        return serverConfiguration;
    }

    public static MimeMessage getLastReceivedMessage() throws InterruptedException {
        return messageHandlerFactory.getMessage();
    }
}
