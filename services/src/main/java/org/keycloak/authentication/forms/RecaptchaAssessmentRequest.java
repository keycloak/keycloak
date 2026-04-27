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

package org.keycloak.authentication.forms;

import com.fasterxml.jackson.annotation.JsonProperty;

import static java.lang.String.format;

public class RecaptchaAssessmentRequest {
    @JsonProperty("event")
    private Event event;

    public RecaptchaAssessmentRequest(String token, String siteKey, String action) {
        this.event = new Event(token, siteKey, action);
    }

    public String toString() {
        return format("RecaptchaAssessmentRequest(event=%s)", this.getEvent());
    }

    public Event getEvent() {
        return event;
    }

    public void setEvent(Event event) {
        this.event = event;
    }

    public static class Event {
        @JsonProperty("token")
        private String token;

        @JsonProperty("siteKey")
        private String siteKey;

        @JsonProperty("expectedAction")
        private String action;

        public Event(String token, String siteKey, String action) {
            this.token = token;
            this.siteKey = siteKey;
            this.action = action;
        }

        public String toString() {
            return format("Event(token=%s, siteKey=%s, action=%s)",
                    this.getToken(), this.getSiteKey(), this.getAction());
        }

        public String getToken() {
            return token;
        }

        public void setToken(String token) {
            this.token = token;
        }

        public String getSiteKey() {
            return siteKey;
        }

        public void setSiteKey(String siteKey) {
            this.siteKey = siteKey;
        }

        public String getAction() {
            return action;
        }

        public void setAction(String action) {
            this.action = action;
        }
    }
}
