package org.keycloak.testsuite.util;

import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.util.Map;

import org.keycloak.common.util.Base64;
import org.keycloak.common.util.KeystoreUtil;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;

import com.google.common.base.Splitter;

/**
 * A collection of utilities for manipulating {@link org.keycloak.representations.idm.RealmRepresentation} in tests.
 *
 * @author Sebastian Łaskawiec
 */
public class RealmRepresentationUtils {

   public static final String GENERATED_CERTIFICATE_MARKER = "generated:certificate:";
   public static final String GENERATED_ATTRIBUTE_KEYSTORE = "keystore";
   public static final String GENERATED_ATTRIBUTE_PASSWORD = "password";
   public static final String GENERATED_ATTRIBUTE_ALIAS = "alias";

   public static void replaceGeneratedCertificates(RealmRepresentation tr) {
      if (tr != null) {
         if (tr.getClients() != null) {
            for (ClientRepresentation clientRepresentation : tr.getClients()) {
               if (clientRepresentation.getAttributes() != null) {
                  for (Map.Entry<String, String> attribute : clientRepresentation.getAttributes().entrySet()) {
                     if (attribute.getValue().startsWith(GENERATED_CERTIFICATE_MARKER)) {
                        String certificateAsPEM = getCertificateAsPEM(attribute.getValue());
                        attribute.setValue(certificateAsPEM);
                     }
                  }
               }
            }
         }
      }
   }

   private static String getCertificateAsPEM(String configurationLine) {
      configurationLine = configurationLine.substring(GENERATED_CERTIFICATE_MARKER.length());
      Map<String, String> parsedConfiguration = Splitter.on(",").trimResults().withKeyValueSeparator("=").split(configurationLine);

      String keystorePath = parsedConfiguration.get(GENERATED_ATTRIBUTE_KEYSTORE);
      String password = parsedConfiguration.get(GENERATED_ATTRIBUTE_PASSWORD);
      String alias = parsedConfiguration.get(GENERATED_ATTRIBUTE_ALIAS);

      try {
         KeyStore keyStore = KeystoreUtil.loadKeyStore(keystorePath, password);
         return Base64.encodeBytes(keyStore.getCertificate(alias).getEncoded());
      } catch (CertificateEncodingException | KeyStoreException e) {
         throw new IllegalStateException("Could not obtain certificate from configuration " + parsedConfiguration, e);
      } catch (Exception e) {
         throw new IllegalStateException("Could not obtain keystore from configuration " + parsedConfiguration, e);
      }
   }
}
