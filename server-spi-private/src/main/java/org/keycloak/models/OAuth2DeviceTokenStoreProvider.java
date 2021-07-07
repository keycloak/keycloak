/*
 * Copyright 2019 Red Hat, Inc. and/or its affiliates
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

import java.util.Map;


/**
 * Provides cache for OAuth2 Device Authorization Grant tokens.
 *
 * @author <a href="mailto:h2-wada@nri.co.jp">Hiroyuki Wada</a>
 */
public interface OAuth2DeviceTokenStoreProvider extends Provider {

    /**
     * Stores the given device code and user code
     *
     * @param deviceCode
     * @param userCode
     * @param lifespanSeconds
     */
    void put(OAuth2DeviceCodeModel deviceCode, OAuth2DeviceUserCodeModel userCode, int lifespanSeconds);

    /**
     * Get the model object by the given device code
     *
     * @param realm
     * @param deviceCode
     * @return
     */
    OAuth2DeviceCodeModel getByDeviceCode(RealmModel realm, String deviceCode);

    /**
     * Check the device code is allowed to poll
     *
     * @param deviceCode
     * @return Return true if the given device code is allowed to poll
     */
    boolean isPollingAllowed(OAuth2DeviceCodeModel deviceCode);

    /**
     * Get the model object by the given user code
     *
     * @param realm
     * @param userCode
     * @return
     */
    OAuth2DeviceCodeModel getByUserCode(RealmModel realm, String userCode);

    /**
     * Approve the given user code
     *
     * @param realm
     * @param userCode
     * @param userSessionId
     * @return Return true if approving successful. If the code is already expired and cleared, it returns false.
     */
    boolean approve(RealmModel realm, String userCode, String userSessionId, Map<String, String> additionalParams);

    /**
     * Deny the given user code
     *
     * @param realm
     * @param userCode
     * @return Return true if denying successful. If the code is already expired and cleared, it returns false.
     */
    boolean deny(RealmModel realm, String userCode);

    /**
     * Remove the given device code
     *
     * @param realm
     * @param deviceCode
     * @return Return true if removing successful. If the code is already expired and cleared, it returns false.
     */
    boolean removeDeviceCode(RealmModel realm, String deviceCode);

    /**
     * Remove the given user code
     *
     * @param realm
     * @param userCode
     * @return Return true if removing successful. If the code is already expired and cleared, it returns false.
     */
    boolean removeUserCode(RealmModel realm, String userCode);
}
