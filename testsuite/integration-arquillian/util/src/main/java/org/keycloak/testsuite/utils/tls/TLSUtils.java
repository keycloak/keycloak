package org.keycloak.testsuite.utils.tls;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import java.security.KeyStore;
import java.security.cert.X509Certificate;

public class TLSUtils {

   private TLSUtils() {}

   private static final TrustManager TRUST_ALL_MANAGER = new X509TrustManager() {
      public X509Certificate[] getAcceptedIssuers() {
         return null;
      }

      public void checkClientTrusted(X509Certificate[] certs, String authType) {

      }

      public void checkServerTrusted(X509Certificate[] certs, String authType) {
      }
   };

   public static SSLContext initializeTLS() {
      try {
         KeyStore keystore = KeyStore.getInstance("jks");
         keystore.load(TLSUtils.class.getResourceAsStream("/keycloak.jks"), "secret".toCharArray());
         KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
         keyManagerFactory.init(keystore, "secret".toCharArray());
         KeyManager[] keyManagers = keyManagerFactory.getKeyManagers();

         // Essentially, this is REQUEST CLIENT AUTH behavior. It doesn't fail if the client doesn't have a cert.
         // However it will challenge him to send it.
         KeyStore truststore = KeyStore.getInstance("jks");
         truststore.load(TLSUtils.class.getResourceAsStream("/keycloak.truststore"), "secret".toCharArray());
         TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
         trustManagerFactory.init(truststore);
         TrustManager[] trustManagers = new TrustManager[trustManagerFactory.getTrustManagers().length + 1];
         for (int i = 0; i < trustManagerFactory.getTrustManagers().length; ++i) {
            trustManagers[i] = trustManagerFactory.getTrustManagers()[i];
         }
         trustManagers[trustManagers.length - 1] = TRUST_ALL_MANAGER;

         SSLContext sslContext;
         sslContext = SSLContext.getInstance("TLS");
         sslContext.init(keyManagers, trustManagers, null);
         return sslContext;
      } catch (Exception e) {
         throw new IllegalStateException("Could not initialize TLS", e);
      }
   }
}
