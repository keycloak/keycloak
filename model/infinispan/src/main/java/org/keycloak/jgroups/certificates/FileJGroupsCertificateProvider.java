/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.jgroups.certificates;

import java.util.Objects;
import javax.net.ssl.KeyManager;
import javax.net.ssl.TrustManager;

import org.keycloak.spi.infinispan.JGroupsCertificateProvider;

import org.jgroups.util.FileWatcher;
import org.jgroups.util.SslContextFactory;

/**
 * A {@link JGroupsCertificateProvider} implementation that reads the key and trust stores from a file.
 * <p>
 * This implementation periodically inspects the file for changes. If the files are modified, the new key and trust
 * stores are reloaded and used.
 */
public class FileJGroupsCertificateProvider implements JGroupsCertificateProvider {

    private final SslContextFactory.Context context;

    private FileJGroupsCertificateProvider(SslContextFactory.Context context) {
        this.context = Objects.requireNonNull(context);
    }

    public static FileJGroupsCertificateProvider create(String keyStoreFile, String keyStorePassword, String trustStoreFile, String trustStorePassword) {
        var context = new SslContextFactory()
                .sslProtocol("TLS")
                .keyStoreFileName(Objects.requireNonNull(keyStoreFile))
                .keyStorePassword(Objects.requireNonNull(keyStorePassword))
                .keyStoreType("pkcs12")
                .trustStoreFileName(Objects.requireNonNull(trustStoreFile))
                .trustStorePassword(Objects.requireNonNull(trustStorePassword))
                .trustStoreType("pkcs12")
                .watcher(new FileWatcher())
                .build();
        return new FileJGroupsCertificateProvider(context);
    }


    @Override
    public KeyManager keyManager() {
        return context.keyManager();
    }

    @Override
    public TrustManager trustManager() {
        return context.trustManager();
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
