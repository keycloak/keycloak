package org.keycloak.adapters.saml.config;

import java.io.Serializable;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class Key implements Serializable {

    public static class KeyStoreConfig implements Serializable {
        private String file;
        private String resource;
        private String password;
        private String type;
        private String alias;
        private String privateKeyAlias;
        private String privateKeyPassword;
        private String certificateAlias;


        public String getFile() {
            return file;
        }

        public void setFile(String file) {
            this.file = file;
        }

        public String getResource() {
            return resource;
        }

        public void setResource(String resource) {
            this.resource = resource;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getPrivateKeyAlias() {
            return privateKeyAlias;
        }

        public void setPrivateKeyAlias(String privateKeyAlias) {
            this.privateKeyAlias = privateKeyAlias;
        }

        public String getPrivateKeyPassword() {
            return privateKeyPassword;
        }

        public void setPrivateKeyPassword(String privateKeyPassword) {
            this.privateKeyPassword = privateKeyPassword;
        }

        public String getCertificateAlias() {
            return certificateAlias;
        }

        public void setCertificateAlias(String certificateAlias) {
            this.certificateAlias = certificateAlias;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getAlias() {
            return alias;
        }

        public void setAlias(String alias) {
            this.alias = alias;
        }
    }


    private boolean signing;
    private boolean encryption;
    private KeyStoreConfig keystore;
    private String privateKeyPem;
    private String publicKeyPem;
    private String certificatePem;


    public boolean isSigning() {
        return signing;
    }

    public void setSigning(boolean signing) {
        this.signing = signing;
    }

    public boolean isEncryption() {
        return encryption;
    }

    public void setEncryption(boolean encryption) {
        this.encryption = encryption;
    }

    public KeyStoreConfig getKeystore() {
        return keystore;
    }

    public void setKeystore(KeyStoreConfig keystore) {
        this.keystore = keystore;
    }

    public String getPrivateKeyPem() {
        return privateKeyPem;
    }

    public void setPrivateKeyPem(String privateKeyPem) {
        this.privateKeyPem = privateKeyPem;
    }

    public String getPublicKeyPem() {
        return publicKeyPem;
    }

    public void setPublicKeyPem(String publicKeyPem) {
        this.publicKeyPem = publicKeyPem;
    }

    public String getCertificatePem() {
        return certificatePem;
    }

    public void setCertificatePem(String certificatePem) {
        this.certificatePem = certificatePem;
    }
}
