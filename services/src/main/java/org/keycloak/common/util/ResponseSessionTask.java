/*
 * Copyright 2022 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.common.util;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionTaskWithResult;
import org.keycloak.models.RealmModel;
import org.keycloak.sessions.RootAuthenticationSessionModel;

/**
 * A {@link KeycloakSessionTaskWithResult} that is aimed to be used by endpoints that want to produce a {@link Response} in
 * a retriable transaction. It takes care of the boilerplate code that is common to be seen in this scenario, allowing the
 * endpoint to focus on the actual code that has to be executed in a retriable manner.
 * </p>
 * More specifically, this task:
 * <li>
 *     <ul>pushes the task's session into the resteasy context, restoring the original value after the task is over. This allows
 *     for endpoints to create new instances of themselves and inject the resteasy properties correctly;</ul>
 *     <ul>sets up the task's session context, based on model values found in the original session's context;</ul>
 *     <ul>handles {@link WebApplicationException} when it is thrown by the task.</ul>
 * </li>
 *
 * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
 */
public abstract class ResponseSessionTask implements KeycloakSessionTaskWithResult<Response> {

    private final KeycloakSession originalSession;

    /**
     * Constructs a new instance of this task.
     *
     * @param originalSession the original {@link KeycloakSession} that was active when the task was created.
     */
    public ResponseSessionTask(final KeycloakSession originalSession) {
        this.originalSession = originalSession;
    }

    @Override
    public Response run(final KeycloakSession session) {
        // save the session that was originally in the resteasy context, so it can be restored once the task finishes.
        KeycloakSession originalContextSession = Resteasy.getContextData(KeycloakSession.class);
        try {
            // set up the current session context based on the original session context.
            this.setupSessionContext(session);
            // push the current session into the resteasy context.
            Resteasy.pushContext(KeycloakSession.class, session);
            // run the actual task.
            return runInternal(session);
        } catch (WebApplicationException we) {
            // If the exception is capable of producing a complete response, including a message entity, we return the response
            // here so that the overall transaction is still committed. If the message entity is missing, we throw the exception
            // so that KeycloakError handler is invoked later on to produce a valid response and the transaction is rolled back.
            //
            // Another reason to convert the web application exception into a response here is because some exception subtypes use
            // the Keycloak session to construct the response. As a result, the conversion has to happen before the session is closed.
            Response response = we.getResponse();
            if (response.getEntity() != null) {
                return response;
            }
            throw we;
        } finally {
            // restore original session in resteasy context.
            Resteasy.pushContext(KeycloakSession.class, originalContextSession);
        }
    }

    /**
     * Sets up the context for the specified session. The original realm's context is used to determine what models
     * need to be re-loaded using the current session.
     *
     * @param session the session whose context is to be prepared.
     */
    private void setupSessionContext(final KeycloakSession session) {
        if (this.originalSession == null) return;
        KeycloakContext context = this.originalSession.getContext();
        // setup realm model if necessary.
        RealmModel realmModel = null;
        if (context.getRealm() != null) {
            realmModel = session.realms().getRealm(context.getRealm().getId());
            session.getContext().setRealm(realmModel);
        }
        // setup client model if necessary.
        ClientModel clientModel = null;
        if (context.getClient() != null) {
            clientModel = session.clients().getClientById(realmModel, context.getClient().getId());
            session.getContext().setClient(clientModel);
        }
        // setup auth session model if necessary.
        if (context.getAuthenticationSession() != null) {
            RootAuthenticationSessionModel rootAuthSession = session.authenticationSessions().getRootAuthenticationSession(realmModel,
                    context.getAuthenticationSession().getParentSession().getId());
            if (rootAuthSession != null) {
                session.getContext().setAuthenticationSession(rootAuthSession.getAuthenticationSession(clientModel,
                        context.getAuthenticationSession().getTabId()));
            }
        }
    }

    /**
     * Builds the response that is to be returned.
     *
     * @param session a reference the {@link KeycloakSession}.
     * @return the constructed {@link Response}.
     */
    public abstract Response runInternal(final KeycloakSession session);
}
