/*
 * Copyright 2022 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.keystore;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.cert.CertificateException;

import org.keycloak.common.crypto.CryptoIntegration;
import org.keycloak.common.util.KeystoreUtil.KeystoreFormat;

import org.jboss.logging.Logger;

/**
 * {@code KeyStoreSpi} implementation that hot-reloads {@code KeyStore} when the backing file changes.
 */
public class ReloadingKeyStoreFileSpi extends DelegatingKeyStoreSpi {

  private static final Logger log = Logger.getLogger(ReloadingKeyStoreFileSpi.class);

  private final String type;
  private final Path path;
  private final char[] password;
  private FileTime lastModified;

  public ReloadingKeyStoreFileSpi(String type, Path path, String password) throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException, NoSuchProviderException {
    if (password == null) {
      throw new IllegalArgumentException("Password must not be null");
    }

    this.type = type;
    this.path = path;
    this.password = password.toCharArray();

    refresh();
  }

  /**
   * Reload keystore if it has been modified on disk since is was last loaded.
   * @throws IOException
   * @throws KeyStoreException
   * @throws CertificateException
   * @throws NoSuchAlgorithmException
   * @throws NoSuchProviderException
   */
  void refresh() throws IOException, KeyStoreException, NoSuchAlgorithmException, CertificateException, NoSuchProviderException {
    // If keystore has been previously loaded, check the modification timestamp to decide if reload is needed.
    if ((lastModified != null) && (lastModified.compareTo(Files.getLastModifiedTime(path)) >= 0)) {
      // File was not modified since last reload: do nothing.
      return;
    }

    // Load keystore from disk.
    log.debugv("Reloading keystore {0}", path);
    KeyStore ks = CryptoIntegration.getProvider().getKeyStore(KeystoreFormat.valueOf(type));
    ks.load(Files.newInputStream(path), password);
    setKeyStoreDelegate(ks);
    this.lastModified = Files.getLastModifiedTime(path);
  }

}
