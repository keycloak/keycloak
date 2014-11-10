package org.keycloak.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.KeyStore;

import org.keycloak.constants.GenericConstants;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class KeystoreUtil {

    public static KeyStore loadKeyStore(String filename, String password) throws Exception {
        KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
        InputStream trustStream = (filename.startsWith(GenericConstants.PROTOCOL_CLASSPATH))
                ?KeystoreUtil.class.getResourceAsStream(filename.replace(GenericConstants.PROTOCOL_CLASSPATH, ""))
                :new FileInputStream(new File(filename));
        trustStore.load(trustStream, password.toCharArray());
        trustStream.close();
        return trustStore;
    }
}
