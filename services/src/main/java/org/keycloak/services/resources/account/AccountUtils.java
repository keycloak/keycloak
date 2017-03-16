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
package org.keycloak.services.resources.account;

import org.keycloak.events.Details;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelDuplicateException;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.services.messages.Messages;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class AccountUtils {

    static void updateUsername(String username, UserModel user, KeycloakSession session) {
        RealmModel realm = session.getContext().getRealm();
        if (realm.isEditUsernameAllowed() && username != null) {
            UserModel existing = session.users().getUserByUsername(username, realm);
            if (existing != null && !existing.getId().equals(user.getId())) {
                throw new ModelDuplicateException(Messages.USERNAME_EXISTS);
            }

            user.setUsername(username);
        }
    }

    static void updateEmail(String email, UserModel user, KeycloakSession session, EventBuilder event) {
        RealmModel realm = session.getContext().getRealm();
        String oldEmail = user.getEmail();
        boolean emailChanged = oldEmail != null ? !oldEmail.equals(email) : email != null;
        if (emailChanged && !realm.isDuplicateEmailsAllowed()) {
            UserModel existing = session.users().getUserByEmail(email, realm);
            if (existing != null && !existing.getId().equals(user.getId())) {
                throw new ModelDuplicateException(Messages.EMAIL_EXISTS);
            }
        }

        user.setEmail(email);

        if (emailChanged) {
            user.setEmailVerified(false);
            event.clone().event(EventType.UPDATE_EMAIL).detail(Details.PREVIOUS_EMAIL, oldEmail).detail(Details.UPDATED_EMAIL, email).success();
        }

        if (realm.isRegistrationEmailAsUsername()) {
            if (!realm.isDuplicateEmailsAllowed()) {
                UserModel existing = session.users().getUserByEmail(email, realm);
                if (existing != null && !existing.getId().equals(user.getId())) {
                    throw new ModelDuplicateException(Messages.USERNAME_EXISTS);
                }
            }
            user.setUsername(email);
        }
    }

}
