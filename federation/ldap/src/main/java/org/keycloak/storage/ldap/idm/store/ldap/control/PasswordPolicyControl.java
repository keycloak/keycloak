/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.storage.ldap.idm.store.ldap.control;

import java.math.BigInteger;

import javax.naming.ldap.Control;

import org.jboss.logging.Logger;
import org.keycloak.storage.ldap.idm.store.ldap.BERDecoder;


/**
 * Implements (parts of) draft-behera-ldap-password-policy
 */
public class PasswordPolicyControl implements Control {

    /* https://datatracker.ietf.org/doc/html/draft-behera-ldap-password-policy-11#section-6.1 */
    public static final String OID = "1.3.6.1.4.1.42.2.27.8.5.1";

    private static final Logger logger = Logger.getLogger(PasswordPolicyControl.class);

    private static final int ERROR_CHANGE_AFTER_RESET = 2;

    private boolean changeAfterReset;

    /*
     * https://datatracker.ietf.org/doc/html/draft-behera-ldap-password-policy-11#section-6.2
     *
     * PasswordPolicyResponseValue ::= SEQUENCE {
     *    warning [0] CHOICE {
     *       timeBeforeExpiration [0] INTEGER (0 .. maxInt),
     *       graceAuthNsRemaining [1] INTEGER (0 .. maxInt) } OPTIONAL,
     *    error   [1] ENUMERATED {
     *       passwordExpired             (0),
     *       accountLocked               (1),
     *       changeAfterReset            (2),
     *       passwordModNotAllowed       (3),
     *       mustSupplyOldPassword       (4),
     *       insufficientPasswordQuality (5),
     *       passwordTooShort            (6),
     *       passwordTooYoung            (7),
     *       passwordInHistory           (8),
     *       passwordTooLong             (9) } OPTIONAL }
     */

    PasswordPolicyControl(byte[] encodedValue) {
        BERDecoder ber = new BERDecoder(encodedValue);

        try {
            ber.startSequence(); // PasswordPolicyResponseValue ::= SEQUENCE
            if (ber.isNextTag(BERDecoder.TAG_CLASS_CONTEXT_SPECIFIC, BERDecoder.TAG_FORM_PRIMITIVE, 0)) { // warning [0] CHOICE
                ber.skipElement();
            }
            if (ber.isNextTag(BERDecoder.TAG_CLASS_CONTEXT_SPECIFIC, BERDecoder.TAG_FORM_PRIMITIVE, 1)) { // error   [1] ENUMERATED
                int error = new BigInteger(ber.drainElementValue()).intValue();
                this.changeAfterReset = error == ERROR_CHANGE_AFTER_RESET;
            }

        } catch (BERDecoder.DecodeException ignored) {
            logger.errorf("Failed to parse PasswordPolicyResponseValue: %s", ignored.getMessage());
        }
    }

    public boolean changeAfterReset() {
        return changeAfterReset;
    }

    @Override
    public String getID() {
        return OID;
    }

    @Override
    public boolean isCritical() {
        return Control.NONCRITICAL;
    }

    @Override
    public byte[] getEncodedValue() {
        return new byte[0];
    }

}
