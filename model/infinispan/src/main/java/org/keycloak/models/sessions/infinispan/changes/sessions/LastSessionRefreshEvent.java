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

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.HashMap;
import java.util.Map;

import org.infinispan.commons.marshall.Externalizer;
import org.infinispan.commons.marshall.MarshallUtil;
import org.infinispan.commons.marshall.SerializeWith;
import org.keycloak.cluster.ClusterEvent;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@SerializeWith(LastSessionRefreshEvent.ExternalizerImpl.class)
public class LastSessionRefreshEvent implements ClusterEvent {

    private final Map<String, SessionData> lastSessionRefreshes;

    public LastSessionRefreshEvent(Map<String, SessionData> lastSessionRefreshes) {
        this.lastSessionRefreshes = lastSessionRefreshes;
    }

    public Map<String, SessionData> getLastSessionRefreshes() {
        return lastSessionRefreshes;
    }


    public static class ExternalizerImpl implements Externalizer<LastSessionRefreshEvent> {


        @Override
        public void writeObject(ObjectOutput output, LastSessionRefreshEvent obj) throws IOException {
            MarshallUtil.marshallMap(obj.lastSessionRefreshes, output);
        }


        @Override
        public LastSessionRefreshEvent readObject(ObjectInput input) throws IOException, ClassNotFoundException {
            Map<String, SessionData> map = MarshallUtil.unmarshallMap(input, new MarshallUtil.MapBuilder<String, SessionData, Map<String, SessionData>>() {

                @Override
                public Map<String, SessionData> build(int size) {
                    return new HashMap<>(size);
                }

            });

            LastSessionRefreshEvent event = new LastSessionRefreshEvent(map);
            return event;
        }

    }
}
