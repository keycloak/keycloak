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

import org.jboss.logging.Logger;
import org.keycloak.models.utils.Base32;
import org.keycloak.models.utils.HmacOTP;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class OTPPolicy implements Serializable {

    protected static final Logger logger = Logger.getLogger(OTPPolicy.class);

    protected String type;
    protected String algorithm;
    protected int initialCounter;
    protected int digits;
    protected int lookAheadWindow;
    protected int period;

    private static final Map<String, String> algToKeyUriAlg = new HashMap<>();

    static {
        algToKeyUriAlg.put(HmacOTP.HMAC_SHA1, "SHA1");
        algToKeyUriAlg.put(HmacOTP.HMAC_SHA256, "SHA256");
        algToKeyUriAlg.put(HmacOTP.HMAC_SHA512, "SHA512");
    }

    public OTPPolicy() {
    }

    public OTPPolicy(String type, String algorithm, int initialCounter, int digits, int lookAheadWindow, int period) {
        this.type = type;
        this.algorithm = algorithm;
        this.initialCounter = initialCounter;
        this.digits = digits;
        this.lookAheadWindow = lookAheadWindow;
        this.period = period;
    }

    public static OTPPolicy DEFAULT_POLICY = new OTPPolicy(UserCredentialModel.TOTP, HmacOTP.HMAC_SHA1, 0, 6, 1, 30);

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public void setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
    }

    public int getInitialCounter() {
        return initialCounter;
    }

    public void setInitialCounter(int initialCounter) {
        this.initialCounter = initialCounter;
    }

    public int getDigits() {
        return digits;
    }

    public void setDigits(int digits) {
        this.digits = digits;
    }

    public int getLookAheadWindow() {
        return lookAheadWindow;
    }

    public void setLookAheadWindow(int lookAheadWindow) {
        this.lookAheadWindow = lookAheadWindow;
    }

    public int getPeriod() {
        return period;
    }

    public void setPeriod(int period) {
        this.period = period;
    }

    public String getKeyURI(RealmModel realm, UserModel user, String secret) {
        try {
            String displayName = realm.getDisplayName() != null && !realm.getDisplayName().isEmpty() ? realm.getDisplayName() : realm.getName();
            String uri;

            uri = "otpauth://" + type + "/" + URLEncoder.encode(user.getUsername(), "UTF-8") + "?secret=" +
                    Base32.encode(secret.getBytes()) + "&digits=" + digits + "&algorithm=" + algToKeyUriAlg.get(algorithm);

            uri += "&issuer=" + URLEncoder.encode(displayName, "UTF-8");

            if (type.equals(UserCredentialModel.HOTP)) {
                uri += "&counter=" + initialCounter;
            }
            if (type.equals(UserCredentialModel.TOTP)) {
                uri += "&period=" + period;
            }

            return uri;
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }
}
