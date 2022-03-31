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

package org.keycloak.forms.account;

import org.keycloak.events.Event;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.utils.FormMessage;
import org.keycloak.provider.Provider;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.List;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public interface AccountProvider extends Provider {

    AccountProvider setUriInfo(UriInfo uriInfo);

    AccountProvider setHttpHeaders(HttpHeaders httpHeaders);

    Response createResponse(AccountPages page);

    AccountProvider setError(Response.Status status, String message, Object ... parameters);

    AccountProvider setErrors(Response.Status status, List<FormMessage> messages);

    AccountProvider setSuccess(String message, Object ... parameters);

    AccountProvider setWarning(String message, Object ... parameters);

    AccountProvider setUser(UserModel user);

    AccountProvider setProfileFormData(MultivaluedMap<String, String> formData);

    AccountProvider setRealm(RealmModel realm);

    AccountProvider setReferrer(String[] referrer);

    AccountProvider setEvents(List<Event> events);

    AccountProvider setSessions(List<UserSessionModel> sessions);

    AccountProvider setPasswordSet(boolean passwordSet);

    AccountProvider setStateChecker(String stateChecker);

    AccountProvider setIdTokenHint(String idTokenHint);

    AccountProvider setFeatures(boolean social, boolean events, boolean passwordUpdateSupported, boolean authorizationSupported);

    AccountProvider setAttribute(String key, String value);
}
