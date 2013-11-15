package org.keycloak.adapters.config;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.plugins.providers.RegisterBuiltin;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.keycloak.EnvUtil;
import org.keycloak.PemUtils;
import org.keycloak.RealmConfiguration;
import org.keycloak.ResourceMetadata;
import org.keycloak.representations.idm.PublishedRealmRepresentation;

import javax.ws.rs.core.UriBuilder;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.PublicKey;
import java.util.Map;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ManagedResourceConfigLoader {
    protected ManagedResourceConfig remoteSkeletonKeyConfig;
    protected ResourceMetadata resourceMetadata;
    protected KeyStore clientCertKeystore;
    protected KeyStore truststore;
    protected ResteasyClient client;
    protected RealmConfiguration realmConfiguration;

    public ManagedResourceConfigLoader() {
    }

    public ManagedResourceConfigLoader(InputStream is) {
        loadConfig(is);
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
        if (remoteSkeletonKeyConfig.getAdminRole() == null) {
            remoteSkeletonKeyConfig.setAdminRole("$REALM-ADMIN$");
        }

        String realm = remoteSkeletonKeyConfig.getRealm();
        if (realm == null) throw new RuntimeException("Must set 'realm' in config");
        String resource = remoteSkeletonKeyConfig.getResource();
        if (resource == null) throw new RuntimeException("Must set 'resource' in config");

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

        if (!setupClient || remoteSkeletonKeyConfig.isBearerOnly()) return;

        realmConfiguration = new RealmConfiguration();
        String authUrl = remoteSkeletonKeyConfig.getAuthUrl();
        if (authUrl == null) {
            throw new RuntimeException("You must specify auth-url");
        }
        String tokenUrl = remoteSkeletonKeyConfig.getCodeUrl();
        if (tokenUrl == null) {
            throw new RuntimeException("You mut specify code-url");
        }
        realmConfiguration.setMetadata(resourceMetadata);
        realmConfiguration.setSslRequired(!remoteSkeletonKeyConfig.isSslNotRequired());

        for (Map.Entry<String, String> entry : getRemoteSkeletonKeyConfig().getCredentials().entrySet()) {
            realmConfiguration.getResourceCredentials().param(entry.getKey(), entry.getValue());
        }

        ResteasyClient client = getClient();

        realmConfiguration.setClient(client);
        realmConfiguration.setAuthUrl(UriBuilder.fromUri(authUrl).queryParam("client_id", resourceMetadata.getResourceName()));
        realmConfiguration.setCodeUrl(client.target(tokenUrl));


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

    public RealmConfiguration getRealmConfiguration() {
        return realmConfiguration;
    }

    protected void loadConfig(InputStream is) {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonSerialize.Inclusion.NON_DEFAULT);
        remoteSkeletonKeyConfig = null;
        try {
            remoteSkeletonKeyConfig = mapper.readValue(is, ManagedResourceConfig.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
