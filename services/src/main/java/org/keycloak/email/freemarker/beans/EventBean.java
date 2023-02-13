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

package org.keycloak.email.freemarker.beans;

import org.keycloak.events.Event;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class EventBean {
    private Event event;

    public EventBean(Event event) {
        this.event = event;
    }

    public Date getDate() {
        return new Date(event.getTime());
    }

    public String getEvent() {
        return event.getType().toString().toLowerCase().replace("_", " ");
    }

    public String getClient() {
        return event.getClientId();
    }

    public String getIpAddress() {
        return event.getIpAddress();
    }

    public List<DetailBean> getDetails() {
        List<DetailBean> details = new LinkedList<DetailBean>();
        for (Map.Entry<String, String> e : event.getDetails().entrySet()) {
            details.add(new DetailBean(e));
        }
        return details;
    }

    public static class DetailBean {

        private Map.Entry<String, String> entry;

        public DetailBean(Map.Entry<String, String> entry) {
            this.entry = entry;
        }

        public String getKey() {
            return entry.getKey();
        }

        public String getValue() {
            return entry.getValue().replace("_", " ");
        }

    }
}
