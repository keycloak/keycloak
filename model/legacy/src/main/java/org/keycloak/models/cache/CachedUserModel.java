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
package org.keycloak.models.cache;

import org.keycloak.models.UserModel;
import org.keycloak.models.UserProvider;

import java.util.concurrent.ConcurrentMap;

/**
 * Cached users will implement this interface
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public interface CachedUserModel extends UserModel {

    /**
     * Invalidates the cache for this user and returns a delegate that represents the actual data provider
     *
     * @return
     */
    UserModel getDelegateForUpdate();

    boolean isMarkedForEviction();

    /**
     * Invalidate the cache for this model
     *
     */
    void invalidate();

    /**
     * When was the model was loaded from database.
     *
     * @return
     */
    long getCacheTimestamp();

    /**
     * Returns a map that contains custom things that are cached along with this model.  You can write to this map.
     *
     * @return
     */
    ConcurrentMap getCachedWith();

    /**
     * The {@link CachedUserModel.Streams} interface differs from {@link CachedUserModel} in that it extends the
     * {@link UserModel.Streams} interface, allowing implementations of {@link CachedUserModel} to focus on the
     * {@link java.util.stream.Stream}-based methods in the {@link UserModel} interface.
     */
    interface Streams extends CachedUserModel, UserModel.Streams {
    }
}
