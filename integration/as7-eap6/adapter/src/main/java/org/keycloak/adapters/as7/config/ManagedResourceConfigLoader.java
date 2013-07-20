package org.keycloak.adapters.as7.config;

import org.apache.catalina.Context;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.jboss.logging.Logger;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.plugins.providers.RegisterBuiltin;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.keycloak.EnvUtil;
import org.keycloak.PemUtils;
import org.keycloak.ResourceMetadata;
import org.keycloak.representations.idm.PublishedRealmRepresentation;

import javax.ws.rs.client.WebTarget;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.PublicKey;

public class ManagedResourceConfigLoader {
    static final Logger log = Logger.getLogger(ManagedResourceConfigLoader.class);
    protected ManagedResourceConfig remoteSkeletonKeyConfig;
    protected ResourceMetadata resourceMetadata;
    protected KeyStore clientCertKeystore;
    protected KeyStore truststore;
    protected ResteasyClient client;

    public ManagedResourceConfigLoader(Context context) {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonSerialize.Inclusion.NON_DEFAULT);
        InputStream is = null;
        String path = context.getServletContext().getInitParameter("keycloak.config.file");
        if (path == null) {
            is = context.getServletContext().getResourceAsStream("/WEB-INF/resteasy-oauth.json");
        } else {
            try {
                is = new FileInputStream(path);
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
        remoteSkeletonKeyConfig = null;
        try {
            remoteSkeletonKeyConfig = mapper.readValue(is, ManagedResourceConfig.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void init(boolean setupClient) {



        String truststorePath = remoteSkeletonKeyConfig.getTruststore();
        if (truststorePath != null) {
            truststorePath = EnvUtil.replace(truststorePath);
            String truststorePassword = remoteSkeletonKeyConfig.getTruststorePassword();
            truststorePath = null;
            try {
                this.truststore = loadKeyStore(truststorePath, truststorePassword);
            } catch (Exception e) {
                throw new RuntimeException("Failed to load truststore", e);
            }
        }
        String clientKeystore = remoteSkeletonKeyConfig.getClientKeystore();
        String clientKeyPassword = null;
        if (clientKeystore != null) {
            clientKeystore = EnvUtil.replace(clientKeystore);
            String clientKeystorePassword = remoteSkeletonKeyConfig.getClientKeystorePassword();
            clientCertKeystore = null;
            try {
                clientCertKeystore = loadKeyStore(clientKeystore, clientKeystorePassword);
            } catch (Exception e) {
                throw new RuntimeException("Failed to load keystore", e);
            }
        }

        initClient();

        String realm = remoteSkeletonKeyConfig.getRealm();

        if (remoteSkeletonKeyConfig.getRealmUrl() != null) {
            PublishedRealmRepresentation rep = null;
            try {
                rep = client.target(remoteSkeletonKeyConfig.getRealmUrl()).request().get(PublishedRealmRepresentation.class);
            } finally {
                if (!setupClient) {
                    client.close();
                }
            }
            remoteSkeletonKeyConfig.setRealm(rep.getRealm());
            remoteSkeletonKeyConfig.setAuthUrl(rep.getAuthorizationUrl());
            remoteSkeletonKeyConfig.setCodeUrl(rep.getCodeUrl());
            remoteSkeletonKeyConfig.setRealmKey(rep.getPublicKeyPem());
            remoteSkeletonKeyConfig.setAdminRole(rep.getAdminRole());
        }

        String resource = remoteSkeletonKeyConfig.getResource();
        if (realm == null) throw new RuntimeException("Must set 'realm' in config");

        String realmKeyPem = remoteSkeletonKeyConfig.getRealmKey();
        if (realmKeyPem == null) {
            throw new IllegalArgumentException("You must set the realm-public-key");
        }

        PublicKey realmKey = null;
        try {
            realmKey = PemUtils.decodePublicKey(realmKeyPem);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        resourceMetadata = new ResourceMetadata();
        resourceMetadata.setRealm(realm);
        resourceMetadata.setResourceName(resource);
        resourceMetadata.setRealmKey(realmKey);
        resourceMetadata.setClientKeystore(clientCertKeystore);
        clientKeyPassword = remoteSkeletonKeyConfig.getClientKeyPassword();
        resourceMetadata.setClientKeyPassword(clientKeyPassword);
        resourceMetadata.setTruststore(this.truststore);

    }

    protected void initClient() {
        int size = 10;
        if (remoteSkeletonKeyConfig.getConnectionPoolSize() > 0)
            size = remoteSkeletonKeyConfig.getConnectionPoolSize();
        ResteasyClientBuilder.HostnameVerificationPolicy policy = ResteasyClientBuilder.HostnameVerificationPolicy.WILDCARD;
        if (remoteSkeletonKeyConfig.isAllowAnyHostname())
            policy = ResteasyClientBuilder.HostnameVerificationPolicy.ANY;
        ResteasyProviderFactory providerFactory = new ResteasyProviderFactory();
        ClassLoader old = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(ManagedResourceConfigLoader.class.getClassLoader());
        try {
            ResteasyProviderFactory.getInstance(); // initialize builtins
            RegisterBuiltin.register(providerFactory);
        } finally {
            Thread.currentThread().setContextClassLoader(old);
        }
        ResteasyClientBuilder builder  = new ResteasyClientBuilder()
                .providerFactory(providerFactory)
                .connectionPoolSize(size)
                .hostnameVerification(policy)
                .keyStore(clientCertKeystore, remoteSkeletonKeyConfig.getClientKeyPassword());
        if (remoteSkeletonKeyConfig.isDisableTrustManager()) {
           builder.disableTrustManager();
        } else {
           builder.trustStore(truststore);
        }
        client = builder.build();
    }

    public static KeyStore loadKeyStore(String filename, String password) throws Exception {
        KeyStore trustStore = KeyStore.getInstance(KeyStore
                .getDefaultType());
        File truststoreFile = new File(filename);
        FileInputStream trustStream = new FileInputStream(truststoreFile);
        trustStore.load(trustStream, password.toCharArray());
        trustStream.close();
        return trustStore;
    }

    public ManagedResourceConfig getRemoteSkeletonKeyConfig() {
        return remoteSkeletonKeyConfig;
    }

    public ResourceMetadata getResourceMetadata() {
        return resourceMetadata;
    }

    public ResteasyClient getClient() {
        return client;
    }

    public KeyStore getClientCertKeystore() {
        return clientCertKeystore;
    }

    public KeyStore getTruststore() {
        return truststore;
    }
}