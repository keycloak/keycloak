/*
 * Copyright 2022. Red Hat, Inc. and/or its affiliates
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

package org.keycloak.models.map.storage.jpa.hibernate.listeners;

import org.hibernate.FlushMode;
import org.hibernate.HibernateException;
import org.hibernate.engine.spi.ActionQueue;
import org.hibernate.event.internal.DefaultAutoFlushEventListener;
import org.hibernate.event.spi.AutoFlushEvent;
import org.hibernate.event.spi.EventSource;
import org.hibernate.internal.CoreMessageLogger;
import org.jboss.logging.Logger;

/**
 * Extends Hibernate's {@link DefaultAutoFlushEventListener} to always flush queued inserts to allow correct handling
 * of orphans of that entities in the same transactions.
 * If they wouldn't be flushed, they won't be orphaned (at least not in Hibernate 5.3.24.Final).
 * This class copies over all functionality of the base class that can't be overwritten via inheritance.
 */
public class JpaAutoFlushListener extends DefaultAutoFlushEventListener {

    public static final JpaAutoFlushListener INSTANCE = new JpaAutoFlushListener();

    private static final CoreMessageLogger LOG = Logger.getMessageLogger(CoreMessageLogger.class, DefaultAutoFlushEventListener.class.getName());

    public void onAutoFlush(AutoFlushEvent event) throws HibernateException {
        final EventSource source = event.getSession();
        try {
            source.getEventListenerManager().partialFlushStart();

            if (flushMightBeNeeded(source)) {
                // Need to get the number of collection removals before flushing to executions
                // (because flushing to executions can add collection removal actions to the action queue).
                final ActionQueue actionQueue = source.getActionQueue();
                final int oldSize = actionQueue.numberOfCollectionRemovals();
                flushEverythingToExecutions(event);
                if (flushIsReallyNeeded(event, source)) {
                    LOG.trace("Need to execute flush");
                    event.setFlushRequired(true);

                    // note: performExecutions() clears all collectionXxxxtion
                    // collections (the collection actions) in the session
                    performExecutions(source);
                    postFlush(source);

                    postPostFlush(source);

                    if (source.getFactory().getStatistics().isStatisticsEnabled()) {
                        source.getFactory().getStatistics().flush();
                    }
                } else {
                    LOG.trace("Don't need to execute flush");
                    event.setFlushRequired(false);
                    actionQueue.clearFromFlushNeededCheck(oldSize);
                }
            }
        } finally {
            source.getEventListenerManager().partialFlushEnd(
                    event.getNumberOfEntitiesProcessed(),
                    event.getNumberOfEntitiesProcessed()
            );
        }
    }

    private boolean flushIsReallyNeeded(AutoFlushEvent event, final EventSource source) {
        return source.getHibernateFlushMode() == FlushMode.ALWAYS
                // START OF FIX for auto-flush-mode on inserts that might later be deleted in same transaction
                || source.getActionQueue().numberOfInsertions() > 0
                // END OF FIX
                || source.getActionQueue().areTablesToBeUpdated(event.getQuerySpaces());
    }

    private boolean flushMightBeNeeded(final EventSource source) {
        return !source.getHibernateFlushMode().lessThan(FlushMode.AUTO)
                && source.getDontFlushFromFind() == 0
                && (source.getPersistenceContext().getNumberOfManagedEntities() > 0 ||
                source.getPersistenceContext().getCollectionEntries().size() > 0);
    }

}