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

import java.io.File;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

import javax.net.ssl.SSLContext;

import org.apache.http.ssl.SSLContexts;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.models.Constants;
import org.keycloak.testsuite.arquillian.AuthServerTestEnricher;

import static org.keycloak.testsuite.auth.page.AuthRealm.ADMIN;
import static org.keycloak.testsuite.auth.page.AuthRealm.MASTER;
import static org.keycloak.testsuite.util.IOUtil.PROJECT_BUILD_DIRECTORY;


public class AdminClientUtil {

    public static Keycloak createAdminClient() throws Exception {
        SSLContext ssl = null;
        if ("true".equals(System.getProperty("auth.server.ssl.required"))) {
            File trustore = new File(PROJECT_BUILD_DIRECTORY, "dependency/keystore/keycloak.truststore");
            ssl = getSSLContextWithTrustore(trustore, "secret");

            System.setProperty("javax.net.ssl.trustStore", trustore.getAbsolutePath());
        }

        return Keycloak.getInstance(AuthServerTestEnricher.getAuthServerContextRoot() + "/auth",
                MASTER, ADMIN, ADMIN, Constants.ADMIN_CLI_CLIENT_ID, null, ssl);
    }


    private static SSLContext getSSLContextWithTrustore(File file, String password) throws CertificateException, NoSuchAlgorithmException, KeyStoreException, IOException, KeyManagementException {
        if (!file.isFile()) {
            throw new RuntimeException("Truststore file not found: " + file.getAbsolutePath());
        }
        SSLContext theContext = SSLContexts.custom()
                .useProtocol("TLS")
                .loadTrustMaterial(file, password == null ? null : password.toCharArray())
                .build();
        return theContext;
    }

}
