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
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

import org.keycloak.models.credential.OTPCredentialModel;
import org.keycloak.models.utils.Base32;
import org.keycloak.models.utils.HmacOTP;
import org.keycloak.utils.StringUtil;

import org.jboss.logging.Logger;

import static java.nio.charset.StandardCharsets.UTF_8;

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
    protected boolean isCodeReusable;

    private static final Map<String, String> algToKeyUriAlg = new HashMap<>();

    static {
        algToKeyUriAlg.put(HmacOTP.HMAC_SHA1, "SHA1");
        algToKeyUriAlg.put(HmacOTP.HMAC_SHA256, "SHA256");
        algToKeyUriAlg.put(HmacOTP.HMAC_SHA512, "SHA512");
    }

    public OTPPolicy() {
    }

    public OTPPolicy(String type, String algorithm, int initialCounter, int digits, int lookAheadWindow, int period) {
        this(type, algorithm, initialCounter, digits, lookAheadWindow, period, DEFAULT_IS_REUSABLE);
    }

    public OTPPolicy(String type, String algorithm, int initialCounter, int digits, int lookAheadWindow, int period, boolean isCodeReusable) {
        this.type = type;
        this.algorithm = algorithm;
        this.initialCounter = initialCounter;
        this.digits = digits;
        this.lookAheadWindow = lookAheadWindow;
        this.period = period;
        this.isCodeReusable = isCodeReusable;
    }

    public static OTPPolicy DEFAULT_POLICY = new OTPPolicy(OTPCredentialModel.TOTP, HmacOTP.HMAC_SHA1, 0, 6, 1, 30);
    public static final boolean DEFAULT_IS_REUSABLE = false;

    // Realm attributes
    public static final String REALM_REUSABLE_CODE_ATTRIBUTE = "realmReusableOtpCode";

    public String getAlgorithmKey() {
        return algToKeyUriAlg.containsKey(algorithm) ? algToKeyUriAlg.get(algorithm) : algorithm;
    }

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

    public boolean isCodeReusable() {
        return isCodeReusable;
    }

    public void setCodeReusable(boolean isReusable) {
        isCodeReusable = isReusable;
    }

    /**
     * Constructs the <code>otpauth://</code> URI based on the <a href="https://github.com/google/google-authenticator/wiki/Key-Uri-Format">Key-Uri-Format</a>.
     *
     * @param realm
     * @param user
     * @param secret
     * @return the <code>otpauth://</code> URI
     */
    public String getKeyURI(RealmModel realm, UserModel user, String secret) {

        String issuerName = !StringUtil.isNullOrEmpty(realm.getDisplayName()) ? realm.getDisplayName() : realm.getName();
        String accountName = user.getUsername();

        return getKeyURI(issuerName, accountName, secret);
    }

    /**
     * Constructs the <code>otpauth://</code> URI based on the <a href="https://github.com/google/google-authenticator/wiki/Key-Uri-Format">Key-Uri-Format</a>.
     *
     * @param rawIssuerName
     * @param rawAccountName
     * @param secret
     * @return the <code>otpauth://</code> URI
     */
    public String getKeyURI(String rawIssuerName, String rawAccountName, String secret) {

        String accountName = URLEncoder.encode(rawAccountName, UTF_8);
        /*
         * Replacing ':' in issuerName with a space because the ':' is not allowed in the issuer part of the label.
         * See: https://github.com/google/google-authenticator/wiki/Key-Uri-Format#label
         */
        String issuerName = rawIssuerName.replaceAll("[:]", " ");
        issuerName = URLEncoder.encode(issuerName, UTF_8).replaceAll("\\+", "%20");

        /*
         * The issuerName component in the label is usually shown in a authenticator app, such as
         * Google Authenticator or FreeOTP, as a hint for the user to which system an username
         * belongs to.
         */
        String label = issuerName + ":" + accountName;

        String parameters = "secret=" + Base32.encode(secret.getBytes()) //
                            + "&digits=" + digits //
                            + "&algorithm=" + algToKeyUriAlg.get(algorithm) //
                            + "&issuer=" + issuerName;

        if (type.equals(OTPCredentialModel.HOTP)) {
            parameters += "&counter=" + initialCounter;
        } else if (type.equals(OTPCredentialModel.TOTP)) {
            parameters += "&period=" + period;
        }

        return "otpauth://" + type + "/" + label + "?" + parameters;
    }

}
