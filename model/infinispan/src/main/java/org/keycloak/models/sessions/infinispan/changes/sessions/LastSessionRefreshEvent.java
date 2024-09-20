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

package org.keycloak.models.sessions.infinispan.changes.sessions;

import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;
import org.infinispan.protostream.annotations.ProtoTypeId;
import org.keycloak.cluster.ClusterEvent;
import org.keycloak.marshalling.Marshalling;

import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@ProtoTypeId(Marshalling.LAST_SESSION_REFRESH_EVENT)
public class LastSessionRefreshEvent implements ClusterEvent {

    private final Map<String, SessionData> lastSessionRefreshes;

    @ProtoFactory
    public LastSessionRefreshEvent(Map<String, SessionData> lastSessionRefreshes) {
        this.lastSessionRefreshes = lastSessionRefreshes;
    }

    @ProtoField(value = 1, mapImplementation = HashMap.class)
    public Map<String, SessionData> getLastSessionRefreshes() {
        return lastSessionRefreshes;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof LastSessionRefreshEvent;
    }

    @Override
    public int hashCode() {
        return 1;
    }

}
