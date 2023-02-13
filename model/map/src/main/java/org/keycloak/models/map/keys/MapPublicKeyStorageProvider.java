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

package org.keycloak.models.map.keys;

import org.jboss.logging.Logger;
import org.keycloak.crypto.KeyWrapper;
import org.keycloak.crypto.PublicKeysWrapper;
import org.keycloak.keys.PublicKeyLoader;
import org.keycloak.keys.PublicKeyStorageProvider;
import org.keycloak.models.KeycloakSession;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

public class MapPublicKeyStorageProvider implements PublicKeyStorageProvider {

    private static final Logger log = Logger.getLogger(MapPublicKeyStorageProvider.class);

    private final KeycloakSession session;

    private final Map<String, FutureTask<PublicKeysWrapper>> tasksInProgress;

    public MapPublicKeyStorageProvider(KeycloakSession session, Map<String, FutureTask<PublicKeysWrapper>> tasksInProgress) {
        this.session = session;
        this.tasksInProgress = tasksInProgress;
    }

    @Override
    public KeyWrapper getFirstPublicKey(String modelKey, String algorithm, PublicKeyLoader loader) {
        return getPublicKey(modelKey, null, algorithm, loader);
    }

    @Override
    public KeyWrapper getPublicKey(String modelKey, String kid, String algorithm, PublicKeyLoader loader) {
        WrapperCallable wrapperCallable = new WrapperCallable(modelKey, loader);
        FutureTask<PublicKeysWrapper> task = new FutureTask<>(wrapperCallable);
        FutureTask<PublicKeysWrapper> existing = tasksInProgress.putIfAbsent(modelKey, task);
        PublicKeysWrapper currentKeys;

        if (existing == null) {
            task.run();
        } else {
            task = existing;
        }

        try {
            currentKeys = task.get();

            // Computation finished. Let's see if key is available
            KeyWrapper publicKey = currentKeys.getKeyByKidAndAlg(kid, algorithm);
            if (publicKey != null) {
                return publicKey;
            }

        } catch (ExecutionException ee) {
            throw new RuntimeException("Error when loading public keys: " + ee.getMessage(), ee);
        } catch (InterruptedException ie) {
            throw new RuntimeException("Error. Interrupted when loading public keys", ie);
        } finally {
            // Our thread inserted the task. Let's clean
            if (existing == null) {
                tasksInProgress.remove(modelKey);
            }
        }

        List<String> availableKids = currentKeys == null ? Collections.emptyList() : currentKeys.getKids();
        log.warnf("PublicKey wasn't found in the storage. Requested kid: '%s' . Available kids: '%s'", kid, availableKids);

        return null;
    }

    private class WrapperCallable implements Callable<PublicKeysWrapper> {

        private final String modelKey;
        private final PublicKeyLoader delegate;

        public WrapperCallable(String modelKey, PublicKeyLoader delegate) {
            this.modelKey = modelKey;
            this.delegate = delegate;
        }

        @Override
        public PublicKeysWrapper call() throws Exception {
            PublicKeysWrapper publicKeys = delegate.loadKeys();

            if (log.isDebugEnabled()) {
                log.debugf("Public keys retrieved successfully for model %s. New kids: %s", modelKey, publicKeys.getKids());
            }

            return publicKeys;
        }
    }

    @Override
    public void close() {

    }
}
