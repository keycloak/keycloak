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
package org.keycloak.crls;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.jboss.logging.Logger;
import org.keycloak.common.crypto.CRLProvider;
import org.keycloak.common.crypto.CryptoIntegration;
import org.keycloak.connections.httpclient.HttpClientProvider;
import org.keycloak.models.KeycloakSession;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.security.NoSuchProviderException;
import java.security.cert.*;
import java.util.Hashtable;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

public class CrlLoader {

    private static final Logger logger = Logger.getLogger(CrlLoader.class);
    private final LdapContext ldapContext = new LdapContext();
    private final KeycloakSession session;

    public CrlLoader(KeycloakSession session) {
        this.session = session;
    }

    public X509CRL loadCrl(String url) throws GeneralSecurityException {
        CRLProvider provider = CryptoIntegration.getProvider().getCrlProvider();

        if (url != null) {
            if (url.startsWith("http") || url.startsWith("https")) {
                // load CRL using remote URI
                try {
                    return loadFromURI(provider, new URI(url));
                } catch (URISyntaxException e) {
                    logger.errorf(e.getMessage());
                }
            } else if (url.startsWith("ldap")) {
                // load CRL from LDAP
                try {
                    return loadCRLFromLDAP(provider, new URI(url));
                } catch(URISyntaxException e) {
                    logger.errorf(e.getMessage());
                }
            } else {
                // load CRL from file
                return loadCRLFromRelativePath(provider, url);
            }
        }

        String message = String.format("Unable to load CRL from \"%s\"", url);
        throw new GeneralSecurityException(message);
    }

    private X509CRL loadFromURI(CRLProvider provider, URI remoteURI) throws GeneralSecurityException {
        try {
            logger.debugf("Loading CRL from %s", remoteURI.toString());

            CloseableHttpClient httpClient = session.getProvider(HttpClientProvider.class).getHttpClient();
            HttpGet get = new HttpGet(remoteURI);
            get.setHeader("Pragma", "no-cache");
            get.setHeader("Cache-Control", "no-cache, no-store");
            try (CloseableHttpResponse response = httpClient.execute(get)) {
                try {
                    InputStream content = response.getEntity().getContent();
                    return loadCRLFromFile(provider, streamToFile(content, remoteURI));
                } finally {
                    EntityUtils.consumeQuietly(response.getEntity());
                }
            }
        }
        catch(IOException ex) {
            logger.errorf(ex.getMessage());
        }
        return null;
    }

    private X509CRL loadCRLFromLDAP(CRLProvider provider, URI remoteURI) throws GeneralSecurityException {
        Hashtable<String, String> env = new Hashtable<>(2);
        env.put(Context.INITIAL_CONTEXT_FACTORY, ldapContext.getLdapFactoryClassName());
        env.put(Context.PROVIDER_URL, remoteURI.toString());

        try {
            DirContext ctx = new InitialDirContext(env);
            try {
                Attributes attrs = ctx.getAttributes("");
                Attribute cRLAttribute = attrs.get("certificateRevocationList;binary");
                byte[] data = (byte[])cRLAttribute.get();
                if (data == null || data.length == 0) {
                    throw new CertificateException(String.format("Failed to download CRL from \"%s\"", remoteURI.toString()));
                }
                return loadCRLFromFile(provider, streamToFile(new ByteArrayInputStream(data), remoteURI));
            } finally {
                ctx.close();
            }
        } catch (NamingException | IOException e) {
            logger.errorf(e.getMessage());
        }

        return null;
    }

    private File streamToFile(InputStream inputStream, URI remoteURI) throws IOException {
        String property = System.getProperty("java.io.tmpdir");
        Path crlPath = Paths.get(property, "crls");
        if (!crlPath.toFile().exists()) {
            Files.createDirectory(crlPath);
        }
        String fileName = convertFileNameToReadable(remoteURI);
        Path filePath = Paths.get(crlPath.toString(), fileName);
        long copy = Files.copy(inputStream, filePath, REPLACE_EXISTING);
        return filePath.toFile();
    }

    private String convertFileNameToReadable(URI remoteURI) {
        return remoteURI.toString().replaceAll("[^A-Za-z0-9]", "-");
    }

    private X509CRL loadCRLFromRelativePath(CRLProvider provider, String relativePath) {
        try {
            String configDir = System.getProperty("jboss.server.config.dir");
            if (configDir != null) {
                File f = new File(configDir + File.separator + relativePath);
                return loadCRLFromFile(provider, f);
            }
        }
        catch(IOException | CertificateException | CRLException | NoSuchProviderException ex) {
            logger.errorf(ex.getMessage());
        }
        return null;
    }

    private X509CRL loadCRLFromFile(CRLProvider provider, File crlFile) throws IOException, CertificateException, CRLException, NoSuchProviderException {
        if (crlFile.isFile()) {
            logger.debugf("Loading CRL from %s", crlFile.getAbsolutePath());

            if (!crlFile.canRead()) {
                throw new IOException(String.format("Unable to read CRL from \"%s\"", crlFile.getAbsolutePath()));
            }
            return provider.generateCRL(crlFile);
        }
        return null;
    }

    public static class LdapContext {
        private final String ldapFactoryClassName;

        public LdapContext() {
            ldapFactoryClassName = "com.sun.jndi.ldap.LdapCtxFactory";
        }

        public String getLdapFactoryClassName() {
            return ldapFactoryClassName;
        }
    }
}
