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

package org.keycloak.jose.jws;

import org.keycloak.jose.jws.crypto.RSAProvider;
import org.keycloak.jose.jws.crypto.SignatureProvider;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public enum Algorithm {

    none(null),
    HS256(null),
    HS384(null),
    HS512(null),
    RS256(new RSAProvider()),
    RS384(new RSAProvider()),
    RS512(new RSAProvider()),
    ES256(null),
    ES384(null),
    ES512(null)
    ;
    private SignatureProvider provider;

    Algorithm(SignatureProvider provider) {
        this.provider = provider;
    }

    public SignatureProvider getProvider() {
        return provider;
    }
}
