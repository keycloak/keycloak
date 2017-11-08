/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.models.sessions.infinispan.remotestore;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;

import org.infinispan.client.hotrod.event.ClientCacheEntryCreatedEvent;
import org.infinispan.client.hotrod.event.ClientCacheEntryModifiedEvent;
import org.infinispan.client.hotrod.event.ClientCacheEntryRemovedEvent;
import org.infinispan.client.hotrod.event.ClientEvent;
import org.jboss.logging.Logger;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.common.util.Time;

import static org.infinispan.client.hotrod.event.ClientEvent.Type.CLIENT_CACHE_ENTRY_CREATED;
import static org.infinispan.client.hotrod.event.ClientEvent.Type.CLIENT_CACHE_ENTRY_REMOVED;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class ClientListenerExecutorDecorator<K> {

    private static final Logger logger = Logger.getLogger(ClientListenerExecutorDecorator.class);

    private final Object lock = new Object();

    private final ExecutorService decorated;

    // Both "eventsInProgress" and "eventsQueue" maps are guarded by the "lock", so doesn't need to be concurrency safe

    // Events currently submitted to the ExecutorService
    private Map<K, MyClientEvent> eventsInProgress = new HashMap<>();

    // Queue of the events waiting to process. We don't want events of same key to be processed concurrently
    private MultivaluedHashMap<K, MyClientEventContext> eventsQueue = new MultivaluedHashMap<>();


    public ClientListenerExecutorDecorator(ExecutorService decorated) {
        this.decorated = decorated;
    }


    // Explicitly use 3 submit methods to ensure that different type of ClientEvent is not used

    public void submit(ClientCacheEntryCreatedEvent<K> cacheEntryCreatedEvent, Runnable r) {
        MyClientEvent event = convertIspnClientEvent(cacheEntryCreatedEvent);
        submit(event, r);
    }


    public void submit(ClientCacheEntryModifiedEvent<K> cacheEntryModifiedEvent, Runnable r) {
        MyClientEvent event = convertIspnClientEvent(cacheEntryModifiedEvent);
        submit(event, r);
    }


    public void submit(ClientCacheEntryRemovedEvent<K> cacheEntryRemovedEvent, Runnable r) {
        MyClientEvent event = convertIspnClientEvent(cacheEntryRemovedEvent);
        submit(event, r);
    }


    // IMPL

    private void submit(MyClientEvent event, Runnable r) {
        K key = event.key;

        synchronized (lock) {
            if (!eventsInProgress.containsKey(key)) {
                submitImpl(key, event, r);
            } else {
                putEventToTheQueue(key, event, r);
            }
        }
    }


    // Assume it's called from the synchronized block
    private void submitImpl(K key, MyClientEvent event, Runnable r) {
        logger.debugf("Submitting event to the executor: %s . eventsInProgress size: %d, eventsQueue size: %d", event.toString(), eventsInProgress.size(), eventsQueue.size());

        eventsInProgress.put(key, event);

        Runnable decoratedRunnable = () -> {
            Long start = null;
            try {
                if (logger.isDebugEnabled()) {
                    start = Time.currentTimeMillis();
                }

                r.run();
            } finally {
                synchronized (lock) {
                    eventsInProgress.remove(key);

                    if (logger.isDebugEnabled()) {
                        long took = Time.currentTimeMillis() - start;
                        logger.debugf("Finished processing event by the executor: %s, took: %d ms. EventsInProgress size: %d", event.toString(), took, eventsInProgress.size());
                    }

                    pollQueue(key);
                }
            }
        };

        try {
            decorated.submit(decoratedRunnable);
        } catch (RejectedExecutionException ree) {
            eventsInProgress.remove(key);

            logger.errorf("Rejected execution of task for the event '%s' . Try to increase the pool size. Pool is '%s'", event.toString(), decorated.toString());
            throw ree;
        }
    }


    // Assume it's called from the synchronized block
    private void pollQueue(K key) {
        if (eventsQueue.containsKey(key)) {
            List<MyClientEventContext> events = eventsQueue.get(key);

            if (events.size() > 0) {
                MyClientEventContext nextEvent = events.remove(0);

                // Was last event in the queue for that key
                if (events.size() == 0) {
                    eventsQueue.remove(key);
                }

                submitImpl(key, nextEvent.event, nextEvent.r);

            } else {
                // Shouldn't happen
                throw new IllegalStateException("Illegal state. Size was 0 for key " + key);
            }
        }
    }


    // Assume it's called from the synchronized block
    private void putEventToTheQueue(K key, MyClientEvent event, Runnable r) {
        logger.debugf("Calling putEventToTheQueue: %s", event.toString());

        if (!eventsQueue.containsKey(key)) {
            eventsQueue.putSingle(key, new MyClientEventContext(event, r));
        } else {

            List<MyClientEventContext> existingEvents = eventsQueue.get(key);
            MyClientEventContext myNewEvent = new MyClientEventContext(event, r);

            // Try to optimize queue (EG. in case we have REMOVE event, we can ignore the previous CREATE or MODIFIED events)
            switch (event.type) {
                case CLIENT_CACHE_ENTRY_CREATED:
                    boolean add = true;
                    for (MyClientEventContext ctx : existingEvents) {
                        if (ctx.event.type == CLIENT_CACHE_ENTRY_REMOVED) {
                            // Ignore. TODO: Log me?
                            add = false;
                            break;
                        } else if (ctx.event.type == CLIENT_CACHE_ENTRY_CREATED) {
                            // Ignore. Already on the list
                            add = false;
                            break;
                        }
                    }

                    // Add to the beginning before the MODIFIED events
                    if (add) {
                        existingEvents.add(0, myNewEvent);
                    }
                    break;
                case CLIENT_CACHE_ENTRY_MODIFIED:

                    boolean addd = true;
                    for (int i=0 ; i<existingEvents.size() ; i++) {
                        MyClientEventContext ctx = existingEvents.get(i);
                        if (ctx.event.type == CLIENT_CACHE_ENTRY_REMOVED) {
                            // Ignore.
                            addd = false;
                            break;
                        } else if (ctx.event.type == CLIENT_CACHE_ENTRY_CREATED) {
                            // Shift to the next element. CREATE event go first.
                        } else {
                            // Can ignore the previous MODIFY event if we have newer version
                            if (ctx.event.version < myNewEvent.event.version) {
                                existingEvents.remove(i);
                            } else {
                                addd = false;
                            }
                        }

                        if (addd) {
                            // Add to the end
                            existingEvents.add(myNewEvent);
                        }
                    }
                    break;

                case CLIENT_CACHE_ENTRY_REMOVED:
                    // Can just ignore the other events in the queue in case of REMOVE
                    eventsQueue.putSingle(key, new MyClientEventContext(event, r));
                    break;
                default:
                    throw new IllegalStateException("Unsupported event type: " + event.type);
            }

        }

        logger.debugf("Event queued. Current events for the key '%s': %s", key.toString(), eventsQueue.getList(key));
    }


    public MyClientEvent convertIspnClientEvent(ClientEvent ispnClientEvent) {
        if (ispnClientEvent instanceof ClientCacheEntryCreatedEvent) {
            ClientCacheEntryCreatedEvent<K> ev = (ClientCacheEntryCreatedEvent<K>) ispnClientEvent;
            return new MyClientEvent(ev.getKey(), ev.getVersion(), ev.getType());
        } else if (ispnClientEvent instanceof ClientCacheEntryModifiedEvent) {
            ClientCacheEntryModifiedEvent<K> ev = (ClientCacheEntryModifiedEvent<K>) ispnClientEvent;
            return new MyClientEvent(ev.getKey(), ev.getVersion(), ev.getType());
        } else if (ispnClientEvent instanceof ClientCacheEntryRemovedEvent) {
            ClientCacheEntryRemovedEvent<K> ev = (ClientCacheEntryRemovedEvent<K>) ispnClientEvent;
            return new MyClientEvent(ev.getKey(), -1l, ev.getType());
        } else {
            throw new IllegalStateException("Unsupported event type: " + ispnClientEvent.getType());
        }
    }


    private class MyClientEventContext {
        private final MyClientEvent event;
        private final Runnable r;

        private MyClientEventContext(MyClientEvent event, Runnable r) {
            this.event = event;
            this.r = r;
        }

        @Override
        public String toString() {
            return event.toString();
        }
    }


    // Using separate class as ISPN ClientEvent type doesn't provide access to key and version :/
    private class MyClientEvent {
        private final K key;
        private final long version;
        private final ClientEvent.Type type;

        private MyClientEvent(K key, long version, ClientEvent.Type type) {
            this.key = key;
            this.version = version;
            this.type = type;
        }


        @Override
        public String toString() {
            return String.format("ClientEvent [ type=%s, key=%s, version=%d ]", type, key, version);
        }
    }

}


