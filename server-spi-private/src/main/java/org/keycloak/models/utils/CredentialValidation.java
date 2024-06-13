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

package org.keycloak.models.utils;

import org.keycloak.models.credential.OTPCredentialModel;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class CredentialValidation {

    public static boolean validOTP(String token, OTPCredentialModel credentialModel, int lookAheadWindow) {
        if (credentialModel.getOTPCredentialData().getSubType().equals(OTPCredentialModel.TOTP)) {
            TimeBasedOTP validator = new TimeBasedOTP(credentialModel.getOTPCredentialData().getAlgorithm(),
                    credentialModel.getOTPCredentialData().getDigits(), credentialModel.getOTPCredentialData().getPeriod(),
                    lookAheadWindow);
            return validator.validateTOTP(token, credentialModel.getDecodedSecret());
        } else {
            HmacOTP validator = new HmacOTP(credentialModel.getOTPCredentialData().getDigits(),
                    credentialModel.getOTPCredentialData().getAlgorithm(), lookAheadWindow);
            int c = validator.validateHOTP(token, credentialModel.getDecodedSecret(),
                    credentialModel.getOTPCredentialData().getCounter());
            return c > -1;
        }

    }


}
