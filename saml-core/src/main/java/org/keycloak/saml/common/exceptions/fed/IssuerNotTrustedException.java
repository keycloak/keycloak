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
package org.keycloak.saml.common.exceptions.fed;

import java.security.GeneralSecurityException;

/**
 * Exception indicating that the issuer is not trusted
 *
 * @author Anil.Saldhana@redhat.com
 * @since Jan 26, 2009
 */
public class IssuerNotTrustedException extends GeneralSecurityException {

    public IssuerNotTrustedException() {
        super();
    }

    public IssuerNotTrustedException(String message, Throwable cause) {
        super(message, cause);
    }

    public IssuerNotTrustedException(String msg) {
        super(msg);
    }

    public IssuerNotTrustedException(Throwable cause) {
        super(cause);
    }
}