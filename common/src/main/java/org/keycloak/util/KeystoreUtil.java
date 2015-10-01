package org.keycloak.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.PrivateKey;

import org.keycloak.constants.GenericConstants;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class KeystoreUtil {
    static {
        BouncyIntegration.init();
    }

    public enum KeystoreFormat {
        JKS,
        PKCS12
    }

    public static KeyStore loadKeyStore(String filename, String password) throws Exception {
        KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
        InputStream trustStream = (filename.startsWith(GenericConstants.PROTOCOL_CLASSPATH))
                ?KeystoreUtil.class.getResourceAsStream(filename.replace(GenericConstants.PROTOCOL_CLASSPATH, ""))
                :new FileInputStream(new File(filename));
        trustStore.load(trustStream, password.toCharArray());
        trustStream.close();
        return trustStore;
    }

    public static PrivateKey loadPrivateKeyFromKeystore(String keystoreFile, String storePassword, String keyPassword, String keyAlias, KeystoreFormat format) {
        InputStream stream = FindFile.findFile(keystoreFile);

        try {
            KeyStore keyStore = null;
            if (format == KeystoreFormat.JKS) {
                keyStore = KeyStore.getInstance(format.toString());
            } else {
                keyStore = KeyStore.getInstance(format.toString(), "BC");
            }

            keyStore.load(stream, storePassword.toCharArray());
            PrivateKey key = (PrivateKey) keyStore.getKey(keyAlias, keyPassword.toCharArray());
            if (key == null) {
                throw new RuntimeException("Couldn't load key with alias '" + keyAlias + "' from keystore");
            }
            return key;
        } catch (Exception e) {
            throw new RuntimeException("Failed to load private key: " + e.getMessage(), e);
        }
    }
}
