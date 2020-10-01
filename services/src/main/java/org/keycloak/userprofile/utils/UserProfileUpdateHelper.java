/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.userprofile.utils;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.userprofile.UserProfile;
import org.keycloak.userprofile.validation.UserUpdateEvent;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:markus.till@bosch.io">Markus Till</a>
 */
public class UserProfileUpdateHelper {

    public static void update(UserUpdateEvent userUpdateEvent, KeycloakSession session, UserModel currentUser, UserProfile updatedUser) {
        update(userUpdateEvent, session, currentUser, updatedUser, true);
    }


    public static void update(UserUpdateEvent userUpdateEvent, KeycloakSession session, UserModel currentUser, UserProfile updatedUser, boolean removeMissingAttributes) {
        RealmModel realm = session.getContext().getRealm();

        if (updatedUser.getAttributes() == null || updatedUser.getAttributes().size() == 0)
            return;

        //The Idp review does not respect "isEditUserNameAllowed" therefore we have to miss the check here
        if (!userUpdateEvent.equals(UserUpdateEvent.IdpReview)) {
            //This step has to be done before email is assigned to the username if isRegistrationEmailAsUsername is set
            //Otherwise email change will not reflect in username changes.
            if (updatedUser.getFirstAttribute(UserModel.USERNAME) != null && !realm.isEditUsernameAllowed()) {
                updatedUser.removeAttribute(UserModel.USERNAME);
            }
        }

        if (updatedUser.getFirstAttribute(UserModel.EMAIL) != null && updatedUser.getFirstAttribute(UserModel.EMAIL).isEmpty()) {
            updatedUser.removeAttribute(UserModel.EMAIL);
            updatedUser.setAttribute(UserModel.EMAIL, Collections.singletonList(null));
        }

        if (updatedUser.getFirstAttribute(UserModel.EMAIL) != null && realm.isRegistrationEmailAsUsername()) {
            updatedUser.removeAttribute(UserModel.USERNAME);
            updatedUser.setAttribute(UserModel.USERNAME, Collections.singletonList(updatedUser.getFirstAttribute(UserModel.EMAIL)));
        }

        updateAttributes(currentUser, updatedUser.getAttributes(), removeMissingAttributes);
    }

    private static void updateAttributes(UserModel currentUser, Map<String, List<String>> updatedUser, boolean removeMissingAttributes) {
        for (Map.Entry<String, List<String>> attr : updatedUser.entrySet()) {
            List<String> currentValue = currentUser.getAttribute(attr.getKey());
            //In case of username we need to provide lower case values
            List<String> updatedValue = attr.getKey().equals(UserModel.USERNAME) ? AttributeToLower(attr.getValue()) : attr.getValue();
            if ((currentValue == null || currentValue.size() != updatedValue.size() || !currentValue.containsAll(updatedValue))) {
                currentUser.setAttribute(attr.getKey(), updatedValue);
            }
        }
        if (removeMissingAttributes) {
            Set<String> attrsToRemove = new HashSet<>(currentUser.getAttributes().keySet());
            attrsToRemove.removeAll(updatedUser.keySet());

            for (String attr : attrsToRemove) {
                currentUser.removeAttribute(attr);
            }

        }
    }

    private static List<String> AttributeToLower(List<String> attr) {
        if (attr.size() == 1 && attr.get(0) != null)
            return Collections.singletonList(KeycloakModelUtils.toLowerCaseSafe(attr.get(0)));
        return attr;
    }


}
