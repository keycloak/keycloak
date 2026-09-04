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

package org.keycloak.email.aws.credentials;

/**
 * A credential source was configured but could not produce credentials.
 * <p>
 * Messages of this exception reach the Keycloak server log and, through {@code EmailException}, an
 * administrator's browser. They must therefore name the <em>source</em> and the failure, never the
 * material: an access key id is the only part of a credential that may appear here.
 */
public class AwsCredentialsException extends Exception {

    public AwsCredentialsException(String message) {
        super(message);
    }

    public AwsCredentialsException(String message, Throwable cause) {
        super(message, cause);
    }
}
