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

package org.keycloak.representations;

import org.keycloak.OAuth2Constants;

import com.fasterxml.jackson.annotation.JsonProperty;

import static org.keycloak.OAuth2Constants.EXPIRES_IN;
import static org.keycloak.OAuth2Constants.INTERVAL;

/**
 * Representation for <a href="https://tools.ietf.org/html/draft-ietf-oauth-device-flow-15#section-3.3">Device Authorization Response</a>.
 *
 * @author <a href="mailto:h2-wada@nri.co.jp">Hiroyuki Wada</a>
 */
public class OAuth2DeviceAuthorizationResponse {

    /**
     * REQUIRED
     */
    @JsonProperty("device_code")
    protected String deviceCode;

    /**
     * REQUIRED
     */
    @JsonProperty(OAuth2Constants.USER_CODE)
    protected String userCode;

    /**
     * REQUIRED
     */
    @JsonProperty("verification_uri")
    protected String verificationUri;

    /**
     * OPTIONAL
     */
    @JsonProperty("verification_uri_complete")
    protected String verificationUriComplete;

    /**
     * REQUIRED
     */
    @JsonProperty(EXPIRES_IN)
    protected long expiresIn;

    /**
     * OPTIONAL
     */
    @JsonProperty(INTERVAL)
    protected long interval;

    public String getDeviceCode() {
        return deviceCode;
    }

    public void setDeviceCode(String deviceCode) {
        this.deviceCode = deviceCode;
    }

    public String getUserCode() {
        return userCode;
    }

    public void setUserCode(String userCode) {
        this.userCode = userCode;
    }

    public String getVerificationUri() {
        return verificationUri;
    }

    public void setVerificationUri(String verificationUri) {
        this.verificationUri = verificationUri;
    }

    public String getVerificationUriComplete() {
        return verificationUriComplete;
    }

    public void setVerificationUriComplete(String verificationUriComplete) {
        this.verificationUriComplete = verificationUriComplete;
    }

    public long getExpiresIn() {
        return expiresIn;
    }

    public void setExpiresIn(long expiresIn) {
        this.expiresIn = expiresIn;
    }

    public long getInterval() {
        return interval;
    }

    public void setInterval(long interval) {
        this.interval = interval;
    }
}
