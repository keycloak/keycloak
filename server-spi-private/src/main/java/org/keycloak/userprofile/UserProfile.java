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

import java.util.function.BiConsumer;

import org.keycloak.models.UserModel;

/**
 * <p>An interface providing as an entry point for managing users.
 *
 * <p>A {@code UserProfile} provides a manageable view for user information that also takes into account the context where it is being used.
 * The context represents the different places in Keycloak where users are created, updated, or validated.
 * Examples of contexts are: managing users through the Admin API, or through the Account API.
 *
 * <p>By taking the context into account, the state and behavior of {@link UserProfile} instances depend on the context they
 * are associated with, where validating, updating, creating, or obtaining representations of users is based on the configuration
 * and constraints associated with a context.
 *
 * <p>A {@code UserProfile} instance can be obtained through the {@link UserProfileProvider}.
 *
 * @see UserProfileContext
 * @see UserProfileProvider
 * @author <a href="mailto:markus.till@bosch.io">Markus Till</a>
 */
public interface UserProfile {

    /**
     * Validates the attributes associated with this instance.
     *
     * @throws ValidationException in case
     */
    void validate() throws ValidationException;

    /**
     * Creates a new {@link UserModel} based on the attributes associated with this instance.
     *
     * @throws ValidationException in case validation fails
     *
     * @return the {@link UserModel} instance created from this profile
     */
    UserModel create() throws ValidationException;

    /**
     * <p>Updates the {@link UserModel} associated with this instance. If no {@link UserModel} is associated with this instance, this operation has no effect.
     *
     * <p>Before updating the {@link UserModel}, this method first checks whether the {@link #validate()} method was previously
     * invoked. If not, the validation step is performed prior to updating the model.
     *
     * @param removeAttributes if attributes should be removed from the {@link UserModel} if they are not among the attributes associated with this instance.
     * @param changeListener a set of one or more listeners to listen for attribute changes
     * @throws ValidationException in case of any validation error
     */
    void update(boolean removeAttributes, BiConsumer<String, UserModel>... changeListener) throws ValidationException;

    /**
     * <p>The same as {@link #update(boolean, BiConsumer[])} but forcing the removal of attributes.
     *
     * @param changeListener a set of one or more listeners to listen for attribute changes
     * @throws ValidationException in case of any validation error
     */
    default void update(BiConsumer<String, UserModel>... changeListener) throws            ValidationException, RuntimeException {
        update(true, changeListener);
    }

    /**
     * Returns the attributes associated with this instance. Note that the attributes returned by this method are not necessarily
     * the same from the {@link UserModel}, but those that should be validated and possibly updated to the {@link UserModel}.
     *
     * @return the attributes associated with this instance.
     */
    Attributes getAttributes();
}
