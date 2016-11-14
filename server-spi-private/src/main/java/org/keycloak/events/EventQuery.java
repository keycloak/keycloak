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

import java.util.Date;
import java.util.List;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public interface EventQuery {

    public EventQuery type(EventType... types);

    public EventQuery realm(String realmId);

    public EventQuery client(String clientId);

    public EventQuery user(String userId);

    public EventQuery fromDate(Date fromDate);

    public EventQuery toDate(Date toDate);

    public EventQuery ipAddress(String ipAddress);

    public EventQuery firstResult(int result);

    public EventQuery maxResults(int results);

    public List<Event> getResultList();

}
