/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.userprofile.profile;

import javax.ws.rs.core.MultivaluedMap;

import org.keycloak.authentication.authenticators.broker.util.SerializedBrokeredIdentityContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserModel;
import org.keycloak.representations.account.UserRepresentation;
import org.keycloak.services.resources.AttributeFormDataProcessor;
import org.keycloak.userprofile.LegacyUserProfileProviderFactory;
import org.keycloak.userprofile.UserProfile;
import org.keycloak.userprofile.UserProfileProvider;
import org.keycloak.userprofile.profile.representations.AccountUserRepresentationUserProfile;
import org.keycloak.userprofile.profile.representations.IdpUserProfile;
import org.keycloak.userprofile.profile.representations.UserModelUserProfile;
import org.keycloak.userprofile.profile.representations.UserRepresentationUserProfile;
import org.keycloak.userprofile.validation.UserUpdateEvent;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public final class UserProfileContextFactory {

    public static DefaultUserProfileContext forIdpReview(SerializedBrokeredIdentityContext currentUser,
            MultivaluedMap<String, String> formData, KeycloakSession session) {
        UserProfileProvider profileProvider = getProfileProvider(session);
        return new DefaultUserProfileContext(UserUpdateEvent.IdpReview, new IdpUserProfile(currentUser, profileProvider),
                AttributeFormDataProcessor.toUserProfile(formData), profileProvider);
    }

    public static DefaultUserProfileContext forUpdateProfile(UserModel currentUser,
            MultivaluedMap<String, String> formData,
            KeycloakSession session) {
        UserProfileProvider profileProvider = getProfileProvider(session);
        return new DefaultUserProfileContext(UserUpdateEvent.UpdateProfile, new UserModelUserProfile(currentUser, profileProvider),
                AttributeFormDataProcessor.toUserProfile(formData), profileProvider);
    }

    public static DefaultUserProfileContext forAccountService(UserModel currentUser,
            UserRepresentation rep, KeycloakSession session) {
        UserProfileProvider profileProvider = getProfileProvider(session);
        return new DefaultUserProfileContext(UserUpdateEvent.Account, new UserModelUserProfile(currentUser, profileProvider),
                new AccountUserRepresentationUserProfile(rep, profileProvider),
                profileProvider);
    }

    public static DefaultUserProfileContext forOldAccount(UserModel currentUser,
            MultivaluedMap<String, String> formData, KeycloakSession session) {
        UserProfileProvider profileProvider = getProfileProvider(session);
        return new DefaultUserProfileContext(UserUpdateEvent.Account, new UserModelUserProfile(currentUser, profileProvider),
                AttributeFormDataProcessor.toUserProfile(formData),
                profileProvider);
    }

    public static DefaultUserProfileContext forRegistrationUserCreation(
            KeycloakSession session, MultivaluedMap<String, String> formData) {
        UserProfileProvider profileProvider = getProfileProvider(session);
        return new DefaultUserProfileContext(UserUpdateEvent.RegistrationUserCreation, null,
                AttributeFormDataProcessor.toUserProfile(formData), profileProvider);
    }

    public static DefaultUserProfileContext forRegistrationProfile(KeycloakSession session,
            MultivaluedMap<String, String> formData) {
        UserProfileProvider profileProvider = getProfileProvider(session);
        return new DefaultUserProfileContext(UserUpdateEvent.RegistrationProfile, null,
                AttributeFormDataProcessor.toUserProfile(formData), profileProvider);
    }

    /**
     * @param currentUser if this is null, then we're creating new user. If it is not null, we're updating existing user
     * @param rep
     * @return user profile context for the validation of user when called from admin REST API
     */
    public static DefaultUserProfileContext forUserResource(UserModel currentUser,
            org.keycloak.representations.idm.UserRepresentation rep, KeycloakSession session) {
        UserProfileProvider profileProvider = getProfileProvider(session);
        UserProfile currentUserProfile = currentUser == null ? null : new UserModelUserProfile(currentUser, profileProvider);
        return new DefaultUserProfileContext(UserUpdateEvent.UserResource, currentUserProfile,
                new UserRepresentationUserProfile(rep, profileProvider), profileProvider);
    }

    public static DefaultUserProfileContext forProfile(UserUpdateEvent event) {
        return new DefaultUserProfileContext(event, null, null, null);
    }

    private static UserProfileProvider getProfileProvider(KeycloakSession session) {
        if (session == null) {
            return null;
        }
        return session.getProvider(UserProfileProvider.class, LegacyUserProfileProviderFactory.PROVIDER_ID);
    }
}
