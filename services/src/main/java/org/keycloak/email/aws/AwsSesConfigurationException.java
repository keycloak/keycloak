/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.email.aws;

/**
 * The SES sender is misconfigured.
 * <p>
 * Unchecked on purpose: when this provider is the server's selected email sender the condition is
 * fatal at boot — a Keycloak that cannot send email cannot verify an address or reset a password,
 * and discovering that at the first password reset instead of at startup is strictly worse. When the
 * provider is merely present on the classpath and another sender is selected, the factory catches it
 * and the server starts normally.
 */
public class AwsSesConfigurationException extends RuntimeException {

    public AwsSesConfigurationException(String message) {
        super(message);
    }

    public AwsSesConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }
}
