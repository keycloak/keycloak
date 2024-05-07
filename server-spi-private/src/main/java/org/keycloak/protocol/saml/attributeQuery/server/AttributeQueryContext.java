/*
 * Copyright 2023 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.protocol.saml.attributeQuery.server;

import org.keycloak.events.EventBuilder;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;

/**
 * @author Ben Cresitello-Dittmar
 *
 * Context in which an attribute query response is taking place. This includes the attribute query request document as
 * well as the session and realm objects that the response taking place in.
 */
public class AttributeQueryContext {

    // TODO: resolve dependency issues and use correct types
    private final Object samlDocumentHolder;
    protected final KeycloakSession session;
    protected final RealmModel realm;
    protected final EventBuilder event;

    public AttributeQueryContext(KeycloakSession session,
                                 RealmModel realm,
                                 EventBuilder event,
                                 Object samlDocumentHolder) {
        this.session = session;
        this.realm = realm;
        this.event = event;
        this.samlDocumentHolder = samlDocumentHolder;
    }

    public KeycloakSession getSession() {
        return session;
    }

    public RealmModel getRealm() {
        return realm;
    }

    public EventBuilder getEvent() {
        return event;
    }

    public Object getSamlDocumentHolder() {
        return samlDocumentHolder;
    }

}
