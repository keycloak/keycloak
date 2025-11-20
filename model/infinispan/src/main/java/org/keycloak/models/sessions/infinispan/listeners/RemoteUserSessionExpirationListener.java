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

package org.keycloak.models.sessions.infinispan.listeners;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.sessions.infinispan.entities.RemoteUserSessionEntity;

import org.infinispan.client.hotrod.annotation.ClientCacheEntryExpired;
import org.infinispan.client.hotrod.annotation.ClientListener;
import org.infinispan.client.hotrod.event.ClientCacheEntryCustomEvent;
import org.infinispan.commons.io.UnsignedNumeric;
import org.infinispan.commons.marshall.Marshaller;
import org.infinispan.util.concurrent.BlockingManager;

/**
 * A listener for remote Infinispan caches.
 * <p>
 * It listens to the {@link ClientCacheEntryExpired} events for user sessions.
 */
@ClientListener(converterFactoryName = "___eager-key-value-version-converter", useRawData = true)
public class RemoteUserSessionExpirationListener extends BaseUserSessionExpirationListener {

    private final Marshaller marshaller;

    public RemoteUserSessionExpirationListener(KeycloakSessionFactory factory, BlockingManager blockingManager, Marshaller marshaller) {
        super(factory, blockingManager);
        this.marshaller = marshaller;
    }

    @ClientCacheEntryExpired
    public void onSessionExpired(ClientCacheEntryCustomEvent<byte[]> entryExpired) {
        try {
            RemoteUserSessionEntity entity = extractRemoteUserSessionEntity(entryExpired);
            if (entity == null) {
                return;
            }
            sendExpirationEvent(entity.getUserSessionId(), entity.getUserId(), entity.getRealmId());
        } catch (Exception e) {
            logger.error("Error handling an expired entry", e);
        }
    }

    private RemoteUserSessionEntity extractRemoteUserSessionEntity(ClientCacheEntryCustomEvent<byte[]> event) throws IOException, ClassNotFoundException {
        ByteBuffer rawData = ByteBuffer.wrap(event.getEventData());

        // skip the key, we don't need it
        skipElement(rawData);

        // read the value
        Object value = marshaller.objectFromByteBuffer(readElement(rawData));
        return value instanceof RemoteUserSessionEntity ruse ? ruse : null;
    }

    private static void skipElement(ByteBuffer buffer) {
        int length = UnsignedNumeric.readUnsignedInt(buffer);
        buffer.position(buffer.position() + length);
    }

    private static byte[] readElement(ByteBuffer buffer) {
        int length = UnsignedNumeric.readUnsignedInt(buffer);
        byte[] element = new byte[length];
        buffer.get(element);
        return element;
    }
}
