package org.keycloak.adapters.as7.config;

import org.apache.catalina.Context;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.jboss.logging.Logger;
import org.keycloak.EnvUtil;
import org.keycloak.PemUtils;
import org.keycloak.ResourceMetadata;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.PublicKey;

public class ManagedResourceConfigLoader
{
   static final Logger log = Logger.getLogger(ManagedResourceConfigLoader.class);
   protected ManagedResourceConfig remoteSkeletonKeyConfig;
   protected ResourceMetadata resourceMetadata;

   public ManagedResourceConfigLoader(Context context)
   {
      ObjectMapper mapper = new ObjectMapper();
      mapper.setSerializationInclusion(JsonSerialize.Inclusion.NON_DEFAULT);
      InputStream is = null;
      String path = context.getServletContext().getInitParameter("skeleton.key.config.file");
      if (path == null)
      {
         is = context.getServletContext().getResourceAsStream("/WEB-INF/resteasy-oauth.json");
      }
      else
      {
         try
         {
            is = new FileInputStream(path);
         }
         catch (FileNotFoundException e)
         {
            throw new RuntimeException(e);
         }
      }
      remoteSkeletonKeyConfig = null;
      try
      {
         remoteSkeletonKeyConfig = mapper.readValue(is, ManagedResourceConfig.class);
      }
      catch (IOException e)
      {
         throw new RuntimeException(e);
      }

      String name = remoteSkeletonKeyConfig.getResource();
      String realm = remoteSkeletonKeyConfig.getRealm();
      if (realm == null) throw new RuntimeException("Must set 'realm' in config");

      String realmKeyPem = remoteSkeletonKeyConfig.getRealmKey();
      if (realmKeyPem == null)
      {
         throw new IllegalArgumentException("You must set the realm-public-key");
      }

      PublicKey realmKey = null;
      try
      {
         realmKey = PemUtils.decodePublicKey(realmKeyPem);
      }
      catch (Exception e)
      {
         throw new RuntimeException(e);
      }
      resourceMetadata = new ResourceMetadata();
      resourceMetadata.setRealm(realm);
      resourceMetadata.setResourceName(name);
      resourceMetadata.setRealmKey(realmKey);


      String truststore = remoteSkeletonKeyConfig.getTruststore();
      if (truststore != null)
      {
         truststore = EnvUtil.replace(truststore);
         String truststorePassword = remoteSkeletonKeyConfig.getTruststorePassword();
         KeyStore trust = null;
         try
         {
            trust = loadKeyStore(truststore, truststorePassword);
         }
         catch (Exception e)
         {
            throw new RuntimeException("Failed to load truststore", e);
         }
         resourceMetadata.setTruststore(trust);
      }
      String clientKeystore = remoteSkeletonKeyConfig.getClientKeystore();
      String clientKeyPassword = null;
      if (clientKeystore != null)
      {
         clientKeystore = EnvUtil.replace(clientKeystore);
         String clientKeystorePassword = remoteSkeletonKeyConfig.getClientKeystorePassword();
         KeyStore serverKS = null;
         try
         {
            serverKS = loadKeyStore(clientKeystore, clientKeystorePassword);
         }
         catch (Exception e)
         {
            throw new RuntimeException("Failed to load keystore", e);
         }
         resourceMetadata.setClientKeystore(serverKS);
         clientKeyPassword = remoteSkeletonKeyConfig.getClientKeyPassword();
         resourceMetadata.setClientKeyPassword(clientKeyPassword);
      }

   }
   public static KeyStore loadKeyStore(String filename, String password) throws Exception
   {
      KeyStore trustStore = KeyStore.getInstance(KeyStore
              .getDefaultType());
      File truststoreFile = new File(filename);
      FileInputStream trustStream = new FileInputStream(truststoreFile);
      trustStore.load(trustStream, password.toCharArray());
      trustStream.close();
      return trustStore;
   }

   public ManagedResourceConfig getRemoteSkeletonKeyConfig()
   {
      return remoteSkeletonKeyConfig;
   }

   public ResourceMetadata getResourceMetadata()
   {
      return resourceMetadata;
   }

}