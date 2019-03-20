/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.models;

import org.keycloak.provider.Provider;

/**
 * Provides single-use cache for OAuth2 code parameter. Used to ensure that particular value of code parameter is used once.
 *
 * TODO: For now, it is separate provider as {@link CodeToTokenStoreProvider}, however will be good to merge those 2 providers to "SingleUseCacheProvider"
 * in the future as they provide very similar thing
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public interface SingleUseTokenStoreProvider extends Provider {

    /**
     * Will try to put the token into the cache. It will success just if token is not already there.
     *
     * @param tokenId
     * @param lifespanInSeconds Minimum lifespan for which successfully added token will be kept in the cache.
     * @return true if token was successfully put into the cache. This means that same token wasn't in the cache before
     */
    boolean putIfAbsent(String tokenId, int lifespanInSeconds);

}
