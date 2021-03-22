/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.events;

import org.keycloak.events.admin.AdminEvent;
import org.keycloak.models.KeycloakTransaction;
import org.keycloak.provider.Provider;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 *
 * This interface provides a way to listen to events that happen during the keycloak run.
 * <p/>
 * There are two types of events:
 * <ul>
 *     <li>Event - User events (fired when users do some action, like log in, register etc.)</li>
 *     <li>Admin event - An administrator did some action like client created/updated etc.</li>
 * </ul>
 *
 *
 * Implementors can leverage the fact that the {@code onEvent} and {@code onAdminEvent} are run within a running
 * transaction. Hence, if the event processing uses JPA, it can insert event details into a table, and the whole
 * transaction including the event is either committed or rolled back. However if transaction processing is not
 * an option, e.g. in the case of log files, it is recommended to hook onto transaction after the commit is complete
 * via the {@link org.keycloak.models.KeycloakTransactionManager#enlistAfterCompletion(KeycloakTransaction)} method, so
 * that the events are stacked in memory and only written to the file after the original transaction completes
 * successfully.
 *
 */
public interface EventListenerProvider extends Provider {

    /**
     *
     * Called when a user event occurs e.g. log in, register.
     * <p/>
     * Note this method should not do any action that cannot be rolled back, see {@link EventListenerProvider} javadoc
     * for more details.
     * 
     * @param event to be triggered
     */
    void onEvent(Event event);

    /**
     *
     * Called when an admin event occurs e.g. a client was updated/deleted.
     * <p/>
     * Note this method should not do any action that cannot be rolled back, see {@link EventListenerProvider} javadoc
     * for more details.
     *
     * @param event to be triggered
     * @param includeRepresentation when false, event listener should NOT include representation field in the resulting
     *                              action
     */
    void onEvent(AdminEvent event, boolean includeRepresentation);

}
