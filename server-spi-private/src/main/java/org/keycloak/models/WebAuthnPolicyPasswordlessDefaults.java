/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
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

/**
 * Default values for the WebAuthn configuration when used as passwordless.
 *
 * @author rmartinc
 */
public class WebAuthnPolicyPasswordlessDefaults extends WebAuthnPolicyTwoFactorDefaults {

    public static WebAuthnPolicy get() {
        return new WebAuthnPolicyPasswordlessDefaults();
    }

    WebAuthnPolicyPasswordlessDefaults() {
        super();
        this.requireResidentKey = Constants.WEBAUTHN_POLICY_OPTION_YES;
        this.userVerificationRequirement = Constants.WEBAUTHN_POLICY_OPTION_REQUIRED;
    }
}
