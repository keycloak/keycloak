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

package org.keycloak.userprofile;

import java.util.Map;

import org.keycloak.models.UserModel;
import org.keycloak.provider.Provider;

/**
 * <p>The provider responsible for creating {@link UserProfile} instances.
 *
 * @see UserProfile
 * @author <a href="mailto:markus.till@bosch.io">Markus Till</a>
 */
public interface UserProfileProvider extends Provider {

    /**
     * <p>Creates a new {@link UserProfile} instance only for validation purposes to check whether its attributes are in conformance
     * with the given {@code context} and profile configuration.
     *
     * @param context the context
     * @param user an existing user
     *
     * @return the user profile instance
     */
    UserProfile create(UserProfileContext context, UserModel user);

    /**
     * <p>Creates a new {@link UserProfile} instance for a given {@code context} and {@code attributes} for validation purposes.
     *
     * <p>Instances created from this method are usually related to contexts where validation and updates are performed in different
     * steps, or when creating new users based on the given {@code attributes}.
     *
     * @param context the context
     * @param attributes the attributes to associate with the instance returned from this method
     *
     * @return the user profile instance
     */
    UserProfile create(UserProfileContext context, Map<String, ?> attributes);

    /**
     * <p>Creates a new {@link UserProfile} instance for a given {@code context} and {@code attributes} for update purposes.
     *
     * <p>Instances created from this method are going to run validations and updates based on the given {@code user}. This
     * might be useful when updating an existing user.
     *
     * @param context the context
     * @param attributes the attributes to associate with the instance returned from this method
     * @param user the user to eventually update with the given {@code attributes}
     *
     * @return the user profile instance
     */
    UserProfile create(UserProfileContext context, Map<String, ?> attributes, UserModel user);

    /**
     * Get current UserProfile configuration. JSON formatted file is expected, but
     * depends on the implementation.
     *
     * @return current UserProfile configuration
     * @see #setConfiguration(String)
     */
    String getConfiguration();

    /**
     * Set new UserProfile configuration. It is persisted inside of the provider.
     *
     * @param configuration to be set
     * @throws RuntimeException if configuration is invalid (exact exception class
     *                          depends on the implementation) or configuration
     *                          can't be persisted.
     * @see #getConfiguration()
     */
    void setConfiguration(String configuration);
}
