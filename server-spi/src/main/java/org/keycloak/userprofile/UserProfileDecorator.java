/*
 * Copyright 2023 Red Hat, Inc. and/or its affiliates
 *  and other contributors as indicated by the @author tags.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.keycloak.userprofile;

import java.util.List;


/**
 * <p>This interface allows user storage providers to customize the user profile configuration and its attributes for realm
 * on a per-user storage provider basis.
 *
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public interface UserProfileDecorator {

    /**
     * <p>Decorates user profile with additional metadata. For instance, metadata attributes, which are available just for your user-storage
     * provider can be added there, so they are available just for the users coming from your provider.
     *
     * <p>This method is invoked every time a user is being managed through a user profile provider.
     *
     * @param providerId the id of the user storage provider to which the user is associated with
     * @param metadata the current {@link UserProfileMetadata} for the current realm
     * @return a list of attribute metadata.The {@link AttributeMetadata} returned from this method overrides any other metadata already set in {@code metadata} for a given attribute.
     */
    List<AttributeMetadata> decorateUserProfile(String providerId, UserProfileMetadata metadata);
}
