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

package org.keycloak.models;


import java.io.Serializable;

/**
 * @author Norbert Kelemen
 * @version $Revision: 1 $
 */
public class TrustedDevicePolicy implements Serializable {
    public static final boolean DEFAULT_IS_ENABLED = false;
    public static final int DEFAULT_EXPIRATION = 604800; // 7 days = 7 * 24 * 60 * 60

    public static final String REALM_IS_ENABLED_ATTRIBUTE = "realmTrustedDevicesEnabled";
    public static final String REALM_EXPIRATION_ATTRIBUTE = "realmTrustedDevicesExpiration";

    public static final TrustedDevicePolicy DEFAULT_POLICY = new TrustedDevicePolicy(DEFAULT_IS_ENABLED, DEFAULT_EXPIRATION);

    protected boolean isEnabled;
    // Trust expiration in seconds
    protected int trustExpiration;

    public TrustedDevicePolicy() {
    }

    public TrustedDevicePolicy(boolean isEnabled, int trustExpiration) {
        this.isEnabled = isEnabled;
        this.trustExpiration = trustExpiration;
    }


    public boolean isEnabled() {
        return isEnabled;
    }

    public void setEnabled(boolean enabled) {
        isEnabled = enabled;
    }

    public int getTrustExpiration() {
        return trustExpiration;
    }

    public void setTrustExpiration(int trustExpiration) {
        this.trustExpiration = trustExpiration;
    }
}
