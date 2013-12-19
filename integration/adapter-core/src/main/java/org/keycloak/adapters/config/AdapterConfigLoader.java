package org.keycloak.adapters.config;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.keycloak.util.EnvUtil;
import org.keycloak.util.PemUtils;
import org.keycloak.adapters.ResourceMetadata;
import org.keycloak.representations.adapters.config.AdapterConfig;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.PublicKey;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class AdapterConfigLoader {
    protected AdapterConfig adapterConfig;
    protected ResourceMetadata resourceMetadata;
    protected KeyStore clientCertKeystore;
    protected KeyStore truststore;

    public static KeyStore loadKeyStore(String filename, String password) throws Exception {
        KeyStore trustStore = KeyStore.getInstance(KeyStore
                .getDefaultType());
        File truststoreFile = new File(filename);
        FileInputStream trustStream = new FileInputStream(truststoreFile);
        trustStore.load(trustStream, password.toCharArray());
        trustStream.close();
        return trustStore;
    }

    public void init() {
        String truststorePath = adapterConfig.getTruststore();
        if (truststorePath != null) {
            truststorePath = EnvUtil.replace(truststorePath);
            String truststorePassword = adapterConfig.getTruststorePassword();
            truststorePath = null;
            try {
                this.truststore = loadKeyStore(truststorePath, truststorePassword);
            } catch (Exception e) {
                throw new RuntimeException("Failed to load truststore", e);
            }
        }
        String clientKeystore = adapterConfig.getClientKeystore();
        String clientKeyPassword = null;
        if (clientKeystore != null) {
            clientKeystore = EnvUtil.replace(clientKeystore);
            String clientKeystorePassword = adapterConfig.getClientKeystorePassword();
            clientCertKeystore = null;
            try {
                clientCertKeystore = loadKeyStore(clientKeystore, clientKeystorePassword);
            } catch (Exception e) {
                throw new RuntimeException("Failed to load keystore", e);
            }
        }

        String realm = adapterConfig.getRealm();
        if (realm == null) throw new RuntimeException("Must set 'realm' in config");
        String resource = adapterConfig.getResource();
        if (resource == null) throw new RuntimeException("Must set 'resource' in config");

        String realmKeyPem = adapterConfig.getRealmKey();
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
        clientKeyPassword = adapterConfig.getClientKeyPassword();
        resourceMetadata.setClientKeyPassword(clientKeyPassword);
        resourceMetadata.setTruststore(this.truststore);

    }

    public AdapterConfig getAdapterConfig() {
        return adapterConfig;
    }

    public ResourceMetadata getResourceMetadata() {
        return resourceMetadata;
    }

    public KeyStore getClientCertKeystore() {
        return clientCertKeystore;
    }

    public KeyStore getTruststore() {
        return truststore;
    }

    protected void loadConfig(InputStream is) {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonSerialize.Inclusion.NON_DEFAULT);
        adapterConfig = null;
        try {
            adapterConfig = mapper.readValue(is, AdapterConfig.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
