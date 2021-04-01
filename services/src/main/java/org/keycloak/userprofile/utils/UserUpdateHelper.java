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
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.userprofile.LegacyUserProfileProviderFactory;
import org.keycloak.userprofile.UserProfile;
import org.keycloak.userprofile.UserProfileAttributes;
import org.keycloak.userprofile.UserProfileProvider;
import org.keycloak.userprofile.profile.representations.UserRepresentationUserProfile;
import org.keycloak.userprofile.validation.UserUpdateEvent;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author <a href="mailto:markus.till@bosch.io">Markus Till</a>
 */
public class UserUpdateHelper {


    public static void updateRegistrationProfile(RealmModel realm, UserModel currentUser, UserProfile updatedUser) {
        register(UserUpdateEvent.RegistrationProfile, realm, currentUser, updatedUser);
    }

    public static void updateRegistrationUserCreation(RealmModel realm, UserModel currentUser, UserProfile updatedUser) {
        register(UserUpdateEvent.RegistrationUserCreation, realm, currentUser, updatedUser);
    }

    public static void updateIdpReview(RealmModel realm, UserModel userModelDelegate, UserProfile updatedProfile) {
        update(UserUpdateEvent.IdpReview, realm, userModelDelegate, updatedProfile.getAttributes(), false);
    }

    public static void updateUserProfile(RealmModel realm, UserModel user, UserProfile updatedProfile) {
        update(UserUpdateEvent.UpdateProfile, realm, user, updatedProfile.getAttributes(), false);
    }

    public static void updateAccount(RealmModel realm, UserModel user, UserProfile updatedProfile) {
        update(UserUpdateEvent.Account, realm, user, updatedProfile);
    }

    /**
     * <p>This method should be used when account is updated through the old console where the behavior is different
     * than when using the new Account REST API and console in regards to how user attributes are managed.
     *
     * @deprecated Remove this method as soon as the old console is no longer part of the distribution
     * @param realm
     * @param user
     * @param updatedProfile
     */
    @Deprecated
    public static void updateAccountOldConsole(RealmModel realm, UserModel user, UserProfile updatedProfile) {
        update(UserUpdateEvent.Account, realm, user, updatedProfile.getAttributes(), false);
    }

    public static void updateUserResource(KeycloakSession session, UserModel user, UserRepresentation rep, boolean removeExistingAttributes) {
        UserProfileProvider profileProvider = session.getProvider(UserProfileProvider.class, LegacyUserProfileProviderFactory.PROVIDER_ID);
        RealmModel realm = session.getContext().getRealm();
        UserRepresentationUserProfile userProfile = new UserRepresentationUserProfile(rep, profileProvider);
        update(UserUpdateEvent.UserResource, realm, user, userProfile.getAttributes(), removeExistingAttributes);
    }

    /**
     * will update the user model with the profile values, all missing attributes in the new profile will be removed on the user model
     * @param userUpdateEvent
     * @param realm
     * @param currentUser
     * @param updatedUser
     */
    private static void update(UserUpdateEvent userUpdateEvent, RealmModel realm, UserModel currentUser, UserProfile updatedUser) {
        update(userUpdateEvent, realm, currentUser, updatedUser.getAttributes(), true);
    }

    /**
     * will update the user model with the profile values, attributes which are missing will be ignored
     * @param userUpdateEvent
     * @param realm
     * @param currentUser
     * @param updatedUser
     */
    private static void register(UserUpdateEvent userUpdateEvent, RealmModel realm, UserModel currentUser, UserProfile updatedUser) {
        update(userUpdateEvent, realm, currentUser, updatedUser.getAttributes(), false);
    }

    private static void update(UserUpdateEvent userUpdateEvent, RealmModel realm, UserModel currentUser, UserProfileAttributes updatedUser, boolean removeMissingAttributes) {

        if (updatedUser == null || updatedUser.size() == 0)
            return;

        filterAttributes(userUpdateEvent, realm, updatedUser);

        updateAttributes(currentUser, updatedUser, removeMissingAttributes);
    }

    private static void filterAttributes(UserUpdateEvent userUpdateEvent, RealmModel realm, UserProfileAttributes updatedUser) {
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
    }

    private static void updateAttributes(UserModel currentUser, UserProfileAttributes attributes, boolean removeMissingAttributes) {
        for (Map.Entry<String, List<String>> attr : attributes.entrySet()) {
            List<String> currentValue = currentUser.getAttributeStream(attr.getKey()).collect(Collectors.toList());
            //In case of username we need to provide lower case values
            List<String> updatedValue = attr.getKey().equals(UserModel.USERNAME) ? AttributeToLower(attr.getValue()) : attr.getValue();
            if (currentValue.size() != updatedValue.size() || !currentValue.containsAll(updatedValue)) {
                currentUser.setAttribute(attr.getKey(), updatedValue);
            }
        }
        if (removeMissingAttributes) {
            Set<String> attrsToRemove = new HashSet<>(currentUser.getAttributes().keySet());
            attrsToRemove.removeAll(attributes.keySet());

            for (String attr : attrsToRemove) {
                if (attributes.isReadOnlyAttribute(attr)) {
                    continue;
                }
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
